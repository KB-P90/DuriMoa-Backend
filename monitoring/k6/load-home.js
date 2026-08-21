import { createLoadTestOptions, createLoadTestRequest, setup } from './load-test-common.js';

export { setup };
export const options = createLoadTestOptions('load-home');
export default createLoadTestRequest('/api/home');
