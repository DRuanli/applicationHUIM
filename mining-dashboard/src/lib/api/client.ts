// File: mining-dashboard/src/lib/api/client.ts
import axios, { AxiosInstance } from 'axios';
import toast from 'react-hot-toast';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        const message = error.response?.data?.message || 'An error occurred';
        toast.error(message);
        return Promise.reject(error);
      }
    );
  }

  async startMining(request: any) {
    const response = await this.client.post('/mining/analyze', request);
    return response.data;
  }

  async getMiningResults(jobId: string) {
    const response = await this.client.get(`/mining/results/${jobId}`);
    return response.data;
  }

  async getMiningStatus(jobId: string) {
    const response = await this.client.get(`/mining/status/${jobId}`);
    return response.data;
  }

  async getAllJobs(page = 0, size = 10) {
    const response = await this.client.get('/mining/jobs', {
      params: { page, size },
    });
    return response.data;
  }

  async uploadFile(file: File, type: string) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    const response = await this.client.post('/data/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async getTopKItemsets(jobId: string, k = 10) {
    const response = await this.client.get('/itemsets/top-k', {
      params: { jobId, k },
    });
    return response.data;
  }

  async exportItemsets(jobId: string, format: string) {
    const response = await this.client.get(`/itemsets/export/${jobId}`, {
      params: { format },
      responseType: format === 'csv' ? 'blob' : 'json',
    });
    return response.data;
  }

  async getStatistics() {
    const response = await this.client.get('/mining/statistics');
    return response.data;
  }
}

export const apiClient = new ApiClient();