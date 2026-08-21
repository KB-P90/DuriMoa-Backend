import {
    createFixedLoadTestOptions,
    createLoadTestRequest,
    loginLoadTestAccounts,
} from './load-test-common.js';

export function setup() {
    return loginLoadTestAccounts(30);
}

export const options = createFixedLoadTestOptions(
    'load-expense-monthly-30vu',
    30,
    '2m'
);
export default createLoadTestRequest('/api/expense/monthly');
