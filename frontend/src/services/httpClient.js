import axios from 'axios';

const httpClient = axios.create({
  baseURL: '/api',
  timeout: 20000
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('请求失败', error);
    return Promise.reject(error);
  }
);

export default httpClient;