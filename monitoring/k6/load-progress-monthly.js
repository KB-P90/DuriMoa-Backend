import { createLoadTestOptions, createLoadTestRequest, setup } from './load-test-common.js';

export { setup };
export const options = createLoadTestOptions('load-progress-monthly');
export default createLoadTestRequest('/api/progress/monthly');
