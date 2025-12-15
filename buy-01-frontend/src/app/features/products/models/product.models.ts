import { MediaModels } from "./media.models";

export interface ProductModels {
  id: string;
  name: string;
  description: string;
  quantity: string;
  price: string;
  userId: string;
  images: MediaModels[];
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ProductPage extends PaginatedResponse<ProductModels> {}

export interface ProductQueryParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface ProductSearchParams {
  query?: string;
  minPrice?: number;
  maxPrice?: number;
}
