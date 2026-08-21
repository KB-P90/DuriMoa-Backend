import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1,
    iterations: Number(__ENV.TEST_ITERATIONS || 1),
    thresholds: {
        'http_req_failed{request_type:baseline}': ['rate<0.01'],
        'http_req_duration{request_type:baseline}': ['p(95)<500'],
        checks: ['rate>0.99'],
    },
    tags: {
        application: 'durimoa-backend',
        test_type: 'baseline',
        test_run_id: __ENV.K6_TEST_RUN_ID || 'local',
    },
};

export function setup() {
    if (__ENV.ACCESS_TOKEN) {
        return { accessToken: normalizeAccessToken(__ENV.ACCESS_TOKEN) };
    }

    if (!__ENV.LOGIN_PHONE || !__ENV.LOGIN_PASSWORD) {
        throw new Error(
            'Set LOGIN_PHONE and LOGIN_PASSWORD, or provide ACCESS_TOKEN directly'
        );
    }

    const baseUrl = (__ENV.BASE_URL || 'https://durimoa.kro.kr').replace(/\/$/, '');
    const response = http.post(
        `${baseUrl}/api/auth/login`,
        JSON.stringify({
            phone: __ENV.LOGIN_PHONE,
            password: __ENV.LOGIN_PASSWORD,
        }),
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
        throw new Error(`Login failed with HTTP status ${response.status}`);
    }

    let responseBody;
    try {
        responseBody = response.json();
    } catch (error) {
        throw new Error('Login response is not valid JSON');
    }

    const accessToken = responseBody && responseBody.data && responseBody.data.accessToken;
    if (!accessToken) {
        throw new Error('Login response does not contain data.accessToken');
    }

    return { accessToken: normalizeAccessToken(accessToken) };
}

export function createBaselineRequest(apiPath) {
    return function (setupData) {
        const baseUrl = (__ENV.BASE_URL || 'https://durimoa.kro.kr').replace(/\/$/, '');

        const response = http.get(`${baseUrl}${apiPath}`, {
            headers: {
                Accept: 'application/json',
                Authorization: `Bearer ${setupData.accessToken}`,
            },
            tags: {
                endpoint: apiPath,
                request_type: 'baseline',
            },
        });

        check(response, {
            'status is 200': (result) => result.status === 200,
            'response is JSON': (result) =>
                (result.headers['Content-Type'] || '').includes('application/json'),
        });
    };
}

function normalizeAccessToken(accessToken) {
    return accessToken.replace(/^Bearer\s+/i, '').trim();
}
