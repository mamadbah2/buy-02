import { Injectable } from "@angular/core";
import { catchError, Observable, throwError } from "rxjs";
import { HttpClient, HttpParams } from "@angular/common/http";
import {
  ProductModels,
  ProductPage,
  ProductQueryParams,
} from "../models/product.models";
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
    sortDirection: "DESC",
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

  getOneProduct(id: string): Observable<ProductModels> {
    // const xender =
    return this.httpClient
      .get<ProductModels>(`${this.apiUrl}/api/products/${id}`)
      .pipe(catchError((err) => throwError(() => err)));

    // return xender;
  }
}
