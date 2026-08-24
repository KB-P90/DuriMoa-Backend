param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$LokiUrl = "http://localhost:3100",
    [string]$LogPath = ".\logs\application.log",
    [string]$ApiPath = "/v2/api-docs",
    [int]$RequestCount = 20,
    [int]$PollIntervalMilliseconds = 200,
    [int]$CollectionTimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
$requestIdHeader = "X-Request-ID"
$runId = "logging-sli-{0}" -f ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
$absoluteLogPath = [System.IO.Path]::GetFullPath($LogPath)

if (-not (Test-Path -LiteralPath $absoluteLogPath)) {
    throw "Application log file not found: $absoluteLogPath"
}

function Invoke-LokiQuery {
    param(
        [string]$Query,
        [datetime]$StartTime,
        [datetime]$EndTime
    )

    $startNanoseconds = ([DateTimeOffset]$StartTime).ToUnixTimeMilliseconds() * 1000000
    $endNanoseconds = ([DateTimeOffset]$EndTime).ToUnixTimeMilliseconds() * 1000000
    $encodedQuery = [Uri]::EscapeDataString($Query)
    $uri = "$LokiUrl/loki/api/v1/query_range?query=$encodedQuery&start=$startNanoseconds&end=$endNanoseconds&limit=5000&direction=forward"
    return Invoke-RestMethod -Method Get -Uri $uri -TimeoutSec 10
}

function Get-LokiLines {
    param([object]$QueryResult)

    $lines = @()
    foreach ($stream in @($QueryResult.data.result)) {
        foreach ($value in @($stream.values)) {
            if ($value.Count -ge 2) {
                $lines += [string]$value[1]
            }
        }
    }
    return $lines
}

function Publish-MeasurementToLoki {
    param(
        [string]$MeasurementJson,
        [string]$RunId
    )

    $timestampNanoseconds = ([DateTimeOffset]::UtcNow).ToUnixTimeMilliseconds() * 1000000
    $payload = @{
        streams = @(
            @{
                stream = @{
                    job = "durimoa-logging-sli"
                    service = "durimoa-backend"
                    environment = "local"
                    metric_stream = "operations"
                }
                values = @(,
                    @([string]$timestampNanoseconds, $MeasurementJson)
                )
            }
        )
    } | ConvertTo-Json -Depth 8 -Compress

    Invoke-RestMethod -Method Post -Uri "$LokiUrl/loki/api/v1/push" `
        -ContentType "application/json" -Body $payload -TimeoutSec 10 | Out-Null
}

function Get-Percentile95 {
    param([double[]]$Values)

    if ($Values.Count -eq 0) {
        return $null
    }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Max(0, [Math]::Ceiling($sorted.Count * 0.95) - 1)
    return [Math]::Round($sorted[$index], 2)
}

$startLine = (Get-Content -LiteralPath $absoluteLogPath).Count
$testStartedAt = [datetime]::UtcNow
$requests = @()

for ($index = 1; $index -le $RequestCount; $index++) {
    $requestId = "$runId-$index"
    $sentAt = [datetime]::UtcNow
    $statusCode = 0
    $responseRequestId = $null

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Method Get -Uri ($BaseUrl.TrimEnd('/') + $ApiPath) `
            -Headers @{ $requestIdHeader = $requestId } -TimeoutSec 10
        $statusCode = [int]$response.StatusCode
        $responseRequestId = $response.Headers[$requestIdHeader]
    } catch {
        if ($null -ne $_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $responseRequestId = $_.Exception.Response.Headers[$requestIdHeader]
        }
    }

    $requestMeasurement = [pscustomobject]@{
        RequestId = $requestId
        SentAt = $sentAt
        StatusCode = $statusCode
        ResponseRequestId = $responseRequestId
        HeaderMatched = $responseRequestId -eq $requestId
        LokiFound = $false
        CollectionDelayMs = $null
    }
    $requests += $requestMeasurement

    # Poll immediately per request so request generation time is not counted as Loki ingestion delay.
    $collectionDeadline = [datetime]::UtcNow.AddSeconds($CollectionTimeoutSeconds)
    while ([datetime]::UtcNow -lt $collectionDeadline -and -not $requestMeasurement.LokiFound) {
        $query = '{job="durimoa-backend"} |= "' + $requestId + '"'
        $queryResult = Invoke-LokiQuery -Query $query -StartTime $sentAt.AddSeconds(-5) -EndTime ([datetime]::UtcNow.AddSeconds(5))
        $lokiLines = @(Get-LokiLines -QueryResult $queryResult)
        if (@($lokiLines | Select-String -SimpleMatch $requestId).Count -gt 0) {
            $requestMeasurement.LokiFound = $true
            $requestMeasurement.CollectionDelayMs = ([datetime]::UtcNow - $sentAt).TotalMilliseconds
        } else {
            Start-Sleep -Milliseconds $PollIntervalMilliseconds
        }
    }
}

$newLogLines = @(Get-Content -LiteralPath $absoluteLogPath | Select-Object -Skip $startLine)
$locallyTrackedCount = 0
foreach ($request in $requests) {
    if (@($newLogLines | Select-String -SimpleMatch $request.RequestId).Count -gt 0) {
        $locallyTrackedCount++
    }
}

$causeRequestId = "$runId-cause"
$causeBody = @{ phone = "010-0000-0000"; password = "invalid-password" } | ConvertTo-Json -Compress
try {
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri ($BaseUrl.TrimEnd('/') + "/api/auth/login") `
        -Headers @{ $requestIdHeader = $causeRequestId } -ContentType "application/json" `
        -Body $causeBody -TimeoutSec 10 | Out-Null
} catch {
    # An authentication failure is intentional for the root-cause discovery measurement.
}

$discoveryStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$causeFound = $false
$causeDeadline = [datetime]::UtcNow.AddSeconds($CollectionTimeoutSeconds)
while ([datetime]::UtcNow -lt $causeDeadline -and -not $causeFound) {
    $query = '{job="durimoa-backend", level="ERROR"} |= "' + $causeRequestId + '"'
    $result = Invoke-LokiQuery -Query $query -StartTime $testStartedAt.AddSeconds(-5) -EndTime ([datetime]::UtcNow.AddSeconds(5))
    $causeFound = @($result.data.result).Count -gt 0
    if (-not $causeFound) {
        Start-Sleep -Milliseconds $PollIntervalMilliseconds
    }
}
$discoveryStopwatch.Stop()

$headerIssuedCount = @($requests | Where-Object HeaderMatched).Count
$lokiCollectedCount = @($requests | Where-Object LokiFound).Count
$collectionDelays = @($requests | Where-Object LokiFound | ForEach-Object { [double]$_.CollectionDelayMs })
$p95Delay = Get-Percentile95 -Values $collectionDelays

$result = [ordered]@{
    run_id = $runId
    measured_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")
    request_count = $RequestCount
    request_id_response_header_rate_percent = [Math]::Round(($headerIssuedCount / $RequestCount) * 100, 2)
    request_log_trace_success_rate_percent = [Math]::Round(($locallyTrackedCount / $RequestCount) * 100, 2)
    loki_log_collection_success_rate_percent = [Math]::Round(($lokiCollectedCount / $RequestCount) * 100, 2)
    log_collection_delay_p95_ms = $p95Delay
    log_collection_delay_p95_target_met = $null -ne $p95Delay -and $p95Delay -le 5000
    request_id_root_cause_log_found = $causeFound
    request_id_root_cause_discovery_ms = if ($causeFound) { [Math]::Round($discoveryStopwatch.Elapsed.TotalMilliseconds, 2) } else { $null }
}

$measurementJson = $result | ConvertTo-Json -Compress
$publishedToLoki = $false
try {
    Publish-MeasurementToLoki -MeasurementJson $measurementJson -RunId $runId
    $publishedToLoki = $true
} catch {
    Write-Warning "Failed to publish measurement result to Loki: $($_.Exception.Message)"
}
$result["published_to_loki"] = $publishedToLoki
$result | ConvertTo-Json

if ($headerIssuedCount -ne $RequestCount -or $locallyTrackedCount -ne $RequestCount -or $lokiCollectedCount -ne $RequestCount -or -not $causeFound) {
    exit 1
}
