param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$LokiUrl = "http://localhost:3100",
    [string]$LogPath = ".\logs\application.log",
    [Parameter(Mandatory = $true)]
    [string]$TestPhone,
    [string]$TestPassword,
    [string]$ProbeConnectedId = "connected-masking-verification-001",
    [string]$ProbeAccountNumber = "987-654-321012",
    [int]$LokiWaitSeconds = 3
)

$ErrorActionPreference = "Stop"
$absoluteLogPath = [System.IO.Path]::GetFullPath($LogPath)
$runId = "masking-verification-{0}" -f ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())

if (-not (Test-Path -LiteralPath $absoluteLogPath)) {
    throw "Application log file not found: $absoluteLogPath"
}

if ([string]::IsNullOrWhiteSpace($TestPassword)) {
    $securePassword = Read-Host "Test account password" -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $TestPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

function Invoke-LokiRunQuery {
    param(
        [string]$RequestIdPrefix,
        [datetime]$StartTime,
        [datetime]$EndTime
    )

    $startNanoseconds = ([DateTimeOffset]$StartTime).ToUnixTimeMilliseconds() * 1000000
    $endNanoseconds = ([DateTimeOffset]$EndTime).ToUnixTimeMilliseconds() * 1000000
    $query = '{job="durimoa-backend"} |= "' + $RequestIdPrefix + '"'
    $encodedQuery = [Uri]::EscapeDataString($query)
    $uri = "$LokiUrl/loki/api/v1/query_range?query=$encodedQuery&start=$startNanoseconds&end=$endNanoseconds&limit=5000&direction=forward"
    $response = Invoke-RestMethod -Method Get -Uri $uri -TimeoutSec 10

    $lines = @()
    foreach ($stream in @($response.data.result)) {
        foreach ($value in @($stream.values)) {
            if ($value.Count -ge 2) {
                $lines += [string]$value[1]
            }
        }
    }
    return $lines
}

function Get-OccurrenceCount {
    param(
        [string[]]$Lines,
        [string[]]$Needles
    )

    $count = 0
    foreach ($line in $Lines) {
        foreach ($needle in $Needles) {
            if (-not [string]::IsNullOrEmpty($needle) -and $line.Contains($needle)) {
                $count++
                break
            }
        }
    }
    return $count
}

$startLine = (Get-Content -LiteralPath $absoluteLogPath).Count
$startedAt = [datetime]::UtcNow
$normalizedPhone = $TestPhone.Replace("-", "")
$loginRequestId = "$runId-login"
$probeRequestId = "$runId-codef"
$phoneRequestId = "$runId-phone"

$loginBody = @{
    phone = $TestPhone
    password = $TestPassword
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod -Method Post -Uri ($BaseUrl.TrimEnd('/') + "/api/auth/login") `
    -Headers @{ "X-Request-ID" = $loginRequestId } `
    -ContentType "application/json" -Body $loginBody -TimeoutSec 10

$accessToken = [string]$loginResponse.data.accessToken
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "Login response did not contain an access token."
}
$rawJwt = $accessToken -replace '^Bearer\s+', ''
$authorization = if ($accessToken -match '^Bearer\s+') { $accessToken } else { "Bearer $accessToken" }
$authHeaders = @{ Authorization = $authorization }

try {
    Invoke-WebRequest -UseBasicParsing -Method Get `
        -Uri ($BaseUrl.TrimEnd('/') + "/api/users/phone-availability?phone=" + [Uri]::EscapeDataString($TestPhone)) `
        -Headers @{ Authorization = $authorization; "X-Request-ID" = $phoneRequestId } `
        -TimeoutSec 10 | Out-Null
} catch {
    # The log-safety assertion is valid even if the business request is rejected.
}

$probeBody = @{
    connectedId = $ProbeConnectedId
    organization = "0004"
    account = $ProbeAccountNumber
    startDate = "20260801"
    endDate = "20260824"
    orderBy = "0"
    inquiryType = "1"
} | ConvertTo-Json -Compress

try {
    Invoke-WebRequest -UseBasicParsing -Method Post `
        -Uri ($BaseUrl.TrimEnd('/') + "/api/codef/bank/transaction-list") `
        -Headers @{ Authorization = $authorization; "X-Request-ID" = $probeRequestId } `
        -ContentType "application/json" -Body $probeBody -TimeoutSec 10 | Out-Null
} catch {
    # Fake CODEF identifiers can fail after the controller writes the verification log.
}

Start-Sleep -Seconds $LokiWaitSeconds

$localLines = @(Get-Content -LiteralPath $absoluteLogPath | Select-Object -Skip $startLine)
$lokiLines = @(Invoke-LokiRunQuery -RequestIdPrefix $runId -StartTime $startedAt.AddSeconds(-5) -EndTime ([datetime]::UtcNow.AddSeconds(5)))

$phoneVariants = @($TestPhone, $normalizedPhone) | Select-Object -Unique
$rawValues = @($TestPassword, $rawJwt, $ProbeConnectedId, $ProbeAccountNumber) + $phoneVariants
$accountDigits = $ProbeAccountNumber.Replace("-", "")
$maskedAccount = if ($accountDigits.Length -gt 8) {
    $accountDigits.Substring(0, 4) + "****" + $accountDigits.Substring($accountDigits.Length - 4)
} else {
    "[REDACTED]"
}

$localRawCount = Get-OccurrenceCount -Lines $localLines -Needles $rawValues
$lokiRawCount = Get-OccurrenceCount -Lines $lokiLines -Needles $rawValues
$localRedactedCount = Get-OccurrenceCount -Lines $localLines -Needles @("[REDACTED]", "[REDACTED_JWT]", $maskedAccount)
$lokiRedactedCount = Get-OccurrenceCount -Lines $lokiLines -Needles @("[REDACTED]", "[REDACTED_JWT]", "[REDACTED_SENSITIVE_FIELD]", "[REDACTED_PHONE]", $maskedAccount)
$probeReachedController = @($localLines | Where-Object {
    $_.Contains($probeRequestId) -and $_.Contains("CodefController")
}).Count -gt 0
$passed = $probeReachedController `
    -and $localRawCount -eq 0 `
    -and $lokiRawCount -eq 0 `
    -and $localRedactedCount -gt 0 `
    -and $lokiRedactedCount -gt 0

$result = [ordered]@{
    run_id = $runId
    probe_reached_controller = $probeReachedController
    local_raw_sensitive_log_count = $localRawCount
    loki_raw_sensitive_log_count = $lokiRawCount
    local_masked_log_count = $localRedactedCount
    loki_masked_log_count = $lokiRedactedCount
    password_plaintext_exposed = (Get-OccurrenceCount -Lines ($localLines + $lokiLines) -Needles @($TestPassword)) -gt 0
    phone_plaintext_exposed = (Get-OccurrenceCount -Lines ($localLines + $lokiLines) -Needles $phoneVariants) -gt 0
    jwt_exposed = (Get-OccurrenceCount -Lines ($localLines + $lokiLines) -Needles @($rawJwt)) -gt 0
    connected_id_exposed = (Get-OccurrenceCount -Lines ($localLines + $lokiLines) -Needles @($ProbeConnectedId)) -gt 0
    full_account_number_exposed = (Get-OccurrenceCount -Lines ($localLines + $lokiLines) -Needles @($ProbeAccountNumber)) -gt 0
    masking_verification_passed = $passed
}

$result | ConvertTo-Json

$TestPassword = $null
$rawJwt = $null
$accessToken = $null

if (-not $passed) {
    exit 1
}
