import axios from 'axios';
import type {
  ApiResult,
  PageResponse,
  Post,
  CreatePostRequest,
  UpdatePostRequest,
} from '../types/post';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/posts';

export const isSuccess = (result: ApiResult<unknown>): boolean => {
  return result.code === 200 || result.code === 0;
};

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      console.error('API Error:', error.response.status, error.response.data);
    } else if (error.request) {
      console.error('Network Error:', error.message);
    } else {
      console.error('Error:', error.message);
    }
    return Promise.reject(error);
  }
);

export const postApi = {
  listPublished: async (page = 0, size = 10, signal?: AbortSignal) => {
    const res = await api.get<ApiResult<PageResponse<Post>>>('/published', {
      params: { page, size },
      signal,
    });
    return res.data;
  },

  listAll: async (page = 0, size = 10, signal?: AbortSignal) => {
    const res = await api.get<ApiResult<PageResponse<Post>>>('/all', {
      params: { page, size },
      signal,
    });
    return res.data;
  },

  getById: async (id: number, signal?: AbortSignal) => {
    const res = await api.get<ApiResult<Post>>(`/${id}`, { signal });
    return res.data;
  },

  create: async (data: CreatePostRequest, signal?: AbortSignal) => {
    const res = await api.post<ApiResult<Post>>('', data, { signal });
    return res.data;
  },

  update: async (id: number, data: UpdatePostRequest, signal?: AbortSignal) => {
    const res = await api.put<ApiResult<Post>>(`/${id}`, data, { signal });
    return res.data;
  },

  delete: async (id: number, signal?: AbortSignal) => {
    const res = await api.delete<ApiResult<void>>(`/${id}`, { signal });
    return res.data;
  },

  search: async (keyword: string, page = 0, size = 10, signal?: AbortSignal) => {
    const res = await api.get<ApiResult<PageResponse<Post>>>('/search', {
      params: { keyword, page, size },
      signal,
    });
    return res.data;
  },

  togglePublish: async (id: number, signal?: AbortSignal) => {
    const res = await api.post<ApiResult<Post>>(`/${id}/toggle-publish`, {}, { signal });
    return res.data;
  },

  like: async (id: number, signal?: AbortSignal) => {
    const res = await api.post<ApiResult<Post>>(`/${id}/like`, {}, { signal });
    return res.data;
  },
};
