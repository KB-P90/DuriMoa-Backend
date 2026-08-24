import {
    createFixedLoadTestOptions,
    createLoadTestRequest,
    loginLoadTestAccounts,
} from './load-test-common.js';

export function setup() {
    return loginLoadTestAccounts(30);
}

export const options = createFixedLoadTestOptions(
    'load-records-monthly-30vu',
    30,
    '2m'
);
export default createLoadTestRequest('/api/records/monthly?month=2026-08&type=WEDDING');
