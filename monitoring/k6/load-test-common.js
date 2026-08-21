import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

const REQUIRED_ACCOUNT_COUNT = 50;

export function createLoadTestOptions(defaultTestRunId) {
    return {
        scenarios: {
            ramping_load: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '30s', target: 10 },
                    { duration: '2m', target: 10 },
                    { duration: '30s', target: 30 },
                    { duration: '2m', target: 30 },
                    { duration: '30s', target: 40 },
                    { duration: '1m', target: 40 },
                    { duration: '30s', target: 50 },
                    { duration: '1m', target: 50 },
                    { duration: '30s', target: 0 },
                ],
                gracefulRampDown: '30s',
            },
        },
        thresholds: {
            'http_req_failed{request_type:load_test}': ['rate<0.01'],
            'http_req_duration{request_type:load_test}': ['p(95)<500'],
            checks: ['rate>0.99'],
        },
        setupTimeout: '2m',
        tags: {
            application: 'durimoa-backend',
            test_type: 'load_test',
            test_run_id: __ENV.K6_TEST_RUN_ID || defaultTestRunId,
        },
    };
}

export function createFixedLoadTestOptions(defaultTestRunId, vus, duration) {
    return {
        scenarios: {
            fixed_load: {
                executor: 'constant-vus',
                vus,
                duration,
                gracefulStop: '30s',
            },
        },
        thresholds: {
            'http_req_failed{request_type:load_test}': ['rate<0.01'],
            'http_req_duration{request_type:load_test}': ['p(95)<500'],
            checks: ['rate>0.99'],
        },
        setupTimeout: '2m',
        tags: {
            application: 'durimoa-backend',
            test_type: 'load_test',
            test_run_id: __ENV.K6_TEST_RUN_ID || defaultTestRunId,
        },
    };
}

export function setup() {
    return loginLoadTestAccounts(REQUIRED_ACCOUNT_COUNT);
}

export function loginLoadTestAccounts(requiredAccountCount) {
    const accountCount = Number(__ENV.LOAD_TEST_ACCOUNT_COUNT || requiredAccountCount);
    const phonePrefix = __ENV.LOAD_TEST_PHONE_PREFIX || '010-9900-';
    const password = __ENV.LOAD_TEST_PASSWORD;

    if (!Number.isInteger(accountCount) || accountCount < requiredAccountCount) {
        throw new Error(`LOAD_TEST_ACCOUNT_COUNT must be at least ${requiredAccountCount}`);
    }
    if (!password) {
        throw new Error('LOAD_TEST_PASSWORD environment variable is required');
    }

    const baseUrl = (__ENV.BASE_URL || 'https://durimoa.kro.kr').replace(/\/$/, '');
    const accessTokens = [];

    for (let accountSequence = 1; accountSequence <= accountCount; accountSequence += 1) {
        const phone = `${phonePrefix}${String(accountSequence).padStart(4, '0')}`;
        const response = http.post(
            `${baseUrl}/api/auth/login`,
            JSON.stringify({ phone, password }),
            {
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                tags: {
                    endpoint: '/api/auth/login',
                    request_type: 'authentication',
                },
            }
        );

        if (response.status !== 200) {
            throw new Error(`Load-test account login failed at sequence ${accountSequence}`);
        }

        let responseBody;
        try {
            responseBody = response.json();
        } catch (error) {
            throw new Error(`Login response is not valid JSON at sequence ${accountSequence}`);
        }

        const accessToken = responseBody && responseBody.data && responseBody.data.accessToken;
        if (!accessToken) {
            throw new Error(`Login response has no access token at sequence ${accountSequence}`);
        }
        accessTokens.push(accessToken.replace(/^Bearer\s+/i, '').trim());
    }

    return { accessTokens };
}

export function createLoadTestRequest(apiPath) {
    return function (setupData) {
        const baseUrl = (__ENV.BASE_URL || 'https://durimoa.kro.kr').replace(/\/$/, '');
        const tokenIndex = (exec.vu.idInTest - 1) % setupData.accessTokens.length;
        const response = http.get(`${baseUrl}${apiPath}`, {
            headers: {
                Accept: 'application/json',
                Authorization: `Bearer ${setupData.accessTokens[tokenIndex]}`,
            },
            tags: {
                endpoint: apiPath,
                request_type: 'load_test',
            },
        });

        check(response, {
            'status is 200': (result) => result.status === 200,
            'response is JSON': (result) =>
                (result.headers['Content-Type'] || '').includes('application/json'),
        });
        sleep(1);
    };
}
