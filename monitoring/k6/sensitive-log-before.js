import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const testPhone = __ENV.TEST_PHONE || '010-0000-9001';
const testPassword = __ENV.TEST_PASSWORD || 'BeforeOnly!9001';
const createTestUser = (__ENV.CREATE_TEST_USER || 'false').toLowerCase() === 'true';
const runDirectLogProbe = (__ENV.RUN_DIRECT_LOG_PROBE || 'false').toLowerCase() === 'true';
const probeConnectedId = __ENV.PROBE_CONNECTED_ID || 'connected-sensitive-probe-001';
const probeAccountNumber = __ENV.PROBE_ACCOUNT_NUMBER || '123-456-789012';

const loginSucceeded = new Counter('sensitive_test_login_succeeded');
const loginFailed = new Counter('sensitive_test_login_failed');
const protectedRequestSucceeded = new Counter('sensitive_test_protected_request_succeeded');

function assertSafeTarget() {
    const localTarget = /^https?:\/\/(localhost|127\.0\.0\.1|host\.docker\.internal)(:\d+)?(\/|$)/i
        .test(baseUrl);
    const remoteAllowed = (__ENV.ALLOW_REMOTE_TARGET || 'false').toLowerCase() === 'true';

    if (!localTarget && !remoteAllowed) {
        throw new Error(
            '원격 서버 실행이 차단되었습니다. 테스트 환경임을 확인한 뒤 '
            + 'ALLOW_REMOTE_TARGET=true를 명시하세요.',
        );
    }
}

assertSafeTarget();

export const options = {
    scenarios: {
        sensitive_log_before: {
            executor: 'constant-vus',
            vus: Number(__ENV.TEST_VUS || 1),
            duration: __ENV.TEST_DURATION || '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],
    },
    tags: {
        application: 'durimoa-backend',
        test_type: 'sensitive-log-before',
        test_run_id: __ENV.K6_TEST_RUN_ID || 'sensitive-before-local',
    },
};

function jsonHeaders() {
    return {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    };
}

function signup() {
    return http.post(
        `${baseUrl}/api/auth/signup`,
        JSON.stringify({
            phone: testPhone,
            password: testPassword,
            passwordConfirm: testPassword,
            name: 'Before Test User',
            role: 'B',
            serviceTermsAgreed: true,
            privacyTermsAgreed: true,
            financeTermsAgreed: true,
        }),
        {
            headers: jsonHeaders(),
            tags: { endpoint: '/api/auth/signup' },
        },
    );
}

function login(password = testPassword) {
    return http.post(
        `${baseUrl}/api/auth/login`,
        JSON.stringify({ phone: testPhone, password }),
        {
            headers: jsonHeaders(),
            tags: { endpoint: '/api/auth/login' },
        },
    );
}

function extractAccessToken(response) {
    if (response.status !== 200) {
        return null;
    }

    try {
        const accessToken = response.json('data.accessToken');
        if (typeof accessToken !== 'string' || accessToken.length === 0) {
            return null;
        }
        return accessToken.replace(/^Bearer\s+/i, '');
    } catch (error) {
        return null;
    }
}

function sendDirectLogProbe(accessToken) {
    if (!runDirectLogProbe || !accessToken) {
        return;
    }

    const response = http.post(
        `${baseUrl}/api/codef/bank/transaction-list`,
        JSON.stringify({
            connectedId: probeConnectedId,
            organization: '0004',
            account: probeAccountNumber,
            startDate: '20260801',
            endDate: '20260824',
            orderBy: '0',
            inquiryType: '1',
        }),
        {
            headers: {
                ...jsonHeaders(),
                Authorization: `Bearer ${accessToken}`,
            },
            tags: { endpoint: '/api/codef/bank/transaction-list', probe: 'direct-sensitive-log' },
            timeout: __ENV.PROBE_TIMEOUT || '10s',
        },
    );

    // 외부 CODEF는 가짜 값 때문에 실패할 수 있지만 401/403/404가 아니면 컨트롤러 로그는 실행됐습니다.
    check(response, {
        'direct sensitive log probe reached controller': (result) =>
            result.status !== 0
            && result.status !== 401
            && result.status !== 403
            && result.status !== 404,
    });
}

export function setup() {
    if (createTestUser) {
        const signupResponse = signup();
        check(signupResponse, {
            'signup created or test user already exists': (response) =>
                response.status === 201 || response.status === 400 || response.status === 409,
        });
    }

    const response = login();
    const accessToken = extractAccessToken(response);
    check(response, {
        'setup login succeeded': () => accessToken !== null,
    });

    sendDirectLogProbe(accessToken);

    return { accessToken };
}

export default function (data) {
    // 성공 로그인은 전화번호 조회 조건과 비밀번호 해시 조회 결과의 SQL 로그 노출을 유발할 수 있습니다.
    const successfulLoginResponse = login();
    if (successfulLoginResponse.status === 200) {
        loginSucceeded.add(1);
    } else {
        loginFailed.add(1);
    }

    // 실패 로그인도 전화번호와 비밀번호 관련 요청/예외 로그 노출 여부를 확인합니다.
    const failedLoginResponse = login(`${testPassword}-wrong`);
    check(failedLoginResponse, {
        'invalid password is rejected': (response) => response.status >= 400 && response.status < 500,
    });

    if (data.accessToken) {
        const authHeaders = {
            Accept: 'application/json',
            Authorization: `Bearer ${data.accessToken}`,
        };

        const profileResponse = http.get(`${baseUrl}/api/users/profile`, {
            headers: authHeaders,
            tags: { endpoint: '/api/users/profile' },
        });

        const phoneResponse = http.get(
            `${baseUrl}/api/users/phone-availability?phone=${encodeURIComponent(testPhone)}`,
            {
                headers: authHeaders,
                tags: { endpoint: '/api/users/phone-availability' },
            },
        );

        // 계좌가 연결된 테스트 계정이면 log4jdbc 결과 로그의 계좌번호 노출 여부도 확인됩니다.
        const linkedAssetsResponse = http.get(`${baseUrl}/api/mypage/linked-assets`, {
            headers: authHeaders,
            tags: { endpoint: '/api/mypage/linked-assets' },
        });

        if (profileResponse.status === 200
                && phoneResponse.status === 200
                && linkedAssetsResponse.status === 200) {
            protectedRequestSucceeded.add(1);
        }
    }

    sleep(Number(__ENV.SLEEP_SECONDS || 1));
}
