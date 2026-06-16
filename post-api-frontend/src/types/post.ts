export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface Post {
  id: number;
  title: string;
  content: string;
  authorName: string;
  coverImage: string | null;
  viewCount: number;
  likeCount: number;
  isPublished: boolean;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  title: string;
  content: string;
  authorName: string;
  coverImage?: string;
}

export interface UpdatePostRequest {
  title: string;
  content: string;
  authorName?: string;
  coverImage?: string;
  isPublished?: boolean;
}
