import { createLoadTestOptions, createLoadTestRequest, setup } from './load-test-common.js';

export { setup };
export const options = createLoadTestOptions('load-expense-monthly');
export default createLoadTestRequest('/api/expense/monthly');
