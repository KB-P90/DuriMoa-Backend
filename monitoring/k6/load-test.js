import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || 'https://durimoa.kro.kr').replace(/\/$/, '');
const apiPath = (__ENV.API_PATH || '/v2/api-docs').startsWith('/')
    ? (__ENV.API_PATH || '/v2/api-docs')
    : `/${__ENV.API_PATH}`;

export const options = {
    vus: Number(__ENV.TEST_VUS || 10),
    duration: __ENV.TEST_DURATION || '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        checks: ['rate>0.99'],
    },
    tags: {
        application: 'durimoa-backend',
        test_run_id: __ENV.K6_TEST_RUN_ID || 'local',
    },
};

export default function () {
    const headers = { Accept: 'application/json' };
    if (__ENV.ACCESS_TOKEN) {
        headers.Authorization = `Bearer ${__ENV.ACCESS_TOKEN}`;
    }

    const response = http.get(`${baseUrl}${apiPath}`, {
        headers,
        tags: { endpoint: apiPath },
    });

    check(response, {
        'status is successful': (result) => result.status >= 200 && result.status < 400,
    });
    sleep(1);
}
