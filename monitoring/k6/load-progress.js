import { createLoadTestOptions, createLoadTestRequest, setup } from './load-test-common.js';

export { setup };
export const options = createLoadTestOptions('load-progress');
export default createLoadTestRequest('/api/progress');
