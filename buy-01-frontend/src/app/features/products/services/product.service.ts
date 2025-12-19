import { Injectable } from "@angular/core";
import { catchError, Observable, throwError } from "rxjs";
import { HttpClient, HttpParams } from "@angular/common/http";
import {
  ProductModels,
  ProductPage,
  ProductQueryParams,
  ProductSearchParams,
} from "../models/product.models";
import { TranscriptionResponse } from "../models/transcription.response";
import { environment } from "../../../../environments/environment";

@Injectable({
  providedIn: "root",
})
export class ProductService {
  private apiUrl = environment.apiUrl;
  private readonly defaultQuery: Required<ProductQueryParams> = {
    page: 0,
    size: 20,
    sortBy: "id",
    sortDirection: "DESC"
  };

  constructor(private httpClient: HttpClient) {}

  getProductList(
    params: ProductQueryParams = {},
  ): Observable<ProductPage> {
    const mergedParams = { ...this.defaultQuery, ...params };
    let httpParams = new HttpParams();

    Object.entries(mergedParams).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        httpParams = httpParams.set(key, value.toString());
      }
    });

    return this.httpClient
      .get<ProductPage>(`${this.apiUrl}/api/products`, { params: httpParams })
      .pipe(catchError((err) => throwError(() => err)));
  }

  searchProducts(
    search: ProductSearchParams = {},
    params: ProductQueryParams = {},
  ): Observable<ProductPage> {
    const mergedParams = { ...this.defaultQuery, ...params };
    let httpParams = new HttpParams();

    // Add pagination/sort params
    Object.entries(mergedParams).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        httpParams = httpParams.set(key, value.toString());
      }
    });
    console.log('Search Params:', search);
    // Add search-specific params
    if (search.query?.trim()) {
      httpParams = httpParams.set("query", search.query.trim());
    }
    if (typeof search.minPrice === "number") {
      httpParams = httpParams.set("minPrice", String(search.minPrice));
    }
    if (typeof search.maxPrice === "number") {
      httpParams = httpParams.set("maxPrice", String(search.maxPrice));
    }

    return this.httpClient
      .get<ProductPage>(`${this.apiUrl}/api/products/search`, {
        params: httpParams,
      })
      .pipe(catchError((err) => throwError(() => err)));
  }

  /**
   * Get product name suggestions for autocomplete
   * GET {apiUrl}/api/products/suggest?query=...
   */
  suggestProducts(query: string): Observable<string[]> {
    const httpParams = new HttpParams().set("query", query ?? "");
    return this.httpClient
      .get<string[]>(`${this.apiUrl}/api/products/suggest`, {
        params: httpParams,
      })
      .pipe(catchError((err) => throwError(() => err)));
  }

  getOneProduct(id: string): Observable<ProductModels> {
    // const xender =
    return this.httpClient
      .get<ProductModels>(`${this.apiUrl}/api/products/${id}`)
      .pipe(catchError((err) => throwError(() => err)));

    // return xender;
  }

  transcribeAudio(file: Blob, language: string = 'wol'): Observable<TranscriptionResponse> {
    const formData = new FormData();
    
     // Determine extension based on MIME type
    let extension = 'webm'; // Default for Chrome/Firefox
    if (file.type.includes('mp4')) {
      extension = 'mp4'; // Safari often uses audio/mp4
    } else if (file.type.includes('wav')) {
      extension = 'wav';
    } else if (file.type.includes('ogg')) {
      extension = 'ogg';
    }

    formData.append('file', file, `recording.${extension}`);
    formData.append('language', language);
    return this.httpClient.post<TranscriptionResponse>(`${this.apiUrl}/api/stt/transcribe`, formData);
  }
}
