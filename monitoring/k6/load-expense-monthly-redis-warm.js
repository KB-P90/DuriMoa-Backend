import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { loginLoadTestAccounts } from './load-test-common.js';

const ACCOUNT_COUNT = 30;

export const options = {
    scenarios: {
        redis_warm_cache: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.K6_RATE || 20),
            timeUnit: '1s',
            duration: __ENV.K6_DURATION || '2m',
            preAllocatedVUs: 30,
            maxVUs: 60,
            gracefulStop: '30s',
        },
    },
    thresholds: {
        'http_req_failed{request_type:redis_warm}': ['rate<0.01'],
        'http_req_duration{request_type:redis_warm}': ['p(95)<500'],
        'checks{request_type:redis_warm}': ['rate>0.99'],
        dropped_iterations: ['count==0'],
    },
    setupTimeout: '3m',
    tags: {
        application: 'durimoa-backend',
        test_type: 'redis_cache',
        cache_state: 'warm',
        test_run_id: __ENV.K6_TEST_RUN_ID || 'expense-monthly-redis-warm',
    },
};

function getBaseUrl() {
    return (__ENV.BASE_URL || 'https://durimoa.kro.kr').replace(/\/$/, '');
}

function requestMonthlyExpense(baseUrl, accessToken, requestType) {
    return http.get(`${baseUrl}/api/expense/monthly`, {
        headers: {
            Accept: 'application/json',
            Authorization: `Bearer ${accessToken}`,
        },
        tags: {
            endpoint: '/api/expense/monthly',
            request_type: requestType,
        },
    });
}

export function setup() {
    const setupData = loginLoadTestAccounts(ACCOUNT_COUNT);
    const baseUrl = getBaseUrl();

    for (let index = 0; index < setupData.accessTokens.length; index += 1) {
        const response = requestMonthlyExpense(
            baseUrl,
            setupData.accessTokens[index],
            'cache_warmup'
        );
        if (response.status !== 200) {
            throw new Error(
                `Cache warmup failed at sequence ${index + 1}: HTTP ${response.status}`
            );
        }
    }

    return setupData;
}

export default function (setupData) {
    const tokenIndex = (exec.vu.idInTest - 1) % setupData.accessTokens.length;
    const response = requestMonthlyExpense(
        getBaseUrl(),
        setupData.accessTokens[tokenIndex],
        'redis_warm'
    );

    check(response, {
        'status is 200': (result) => result.status === 200,
        'response is JSON': (result) =>
            (result.headers['Content-Type'] || '').includes('application/json'),
        'response contains data': (result) => {
            try {
                const responseBody = result.json();
                return Boolean(responseBody && responseBody.data);
            } catch (error) {
                return false;
            }
        },
    }, {
        request_type: 'redis_warm',
    });
}
