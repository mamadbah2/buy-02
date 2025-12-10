import { Injectable } from "@angular/core";
import {
  HttpClient,
  HttpHeaders,
  HttpErrorResponse,
} from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError, map } from "rxjs/operators";
import { environment } from "../../../../environments/environment";
import { AuthService } from "../../../auth/services/auth.service";
import { JwtService } from "../../../shared/services/jwt.service";
import { SubOrder } from "../../orders/models/order.models";

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  images: File[];
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
  images: {
    id: string;
    imageUrl: string;
    productId: string;
  }[];
}

@Injectable({
  providedIn: "root",
})
export class SellerService {
  private apiUrl = `${environment.apiUrl}/api/products`;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private jwtService: JwtService,
  ) {}

  createProduct(
    productData: CreateProductRequest,
  ): Observable<ProductResponse> {
    const token = this.authService.getToken();

    // Debug logging
    console.log(
      "Creating product with token:",
      token ? "Token exists" : "No token",
    );
    console.log("API URL:", this.apiUrl);

    if (!token) {
      return throwError(() => new Error("No authentication token available"));
    }

    const formData = new FormData();

    // Add product fields to FormData
    formData.append("name", productData.name);
    formData.append("description", productData.description);
    formData.append("price", productData.price.toString());
    formData.append("quantity", productData.quantity.toString());

    // Add images to FormData
    productData.images.forEach((image, index) => {
      console.log(`Adding image ${index}:`, image.name, image.type, image.size);
      formData.append("images", image);
    });

    // Headers - DO NOT set Content-Type for FormData
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    console.log("Request headers:", headers.keys());

    return this.http
      .post<ProductResponse>(this.apiUrl, formData, { headers })
      .pipe(catchError(this.handleError));
  }

  getMyProducts(): Observable<ProductResponse[]> {
    const token = this.authService.getToken();

    if (!token) {
      return throwError(() => new Error("No authentication token available"));
    }

    // Decode JWT to get userId
    const currentUserId = this.jwtService.getUserId(token);

    if (!currentUserId) {
      return throwError(() => new Error("User ID not found in token"));
    }

    if (!this.jwtService.isTokenValid(token)) {
      return throwError(
        () => new Error("Invalid or expired authentication token"),
      );
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    });

    // Get all products and filter by userId on the frontend
    return this.http.get<any>(this.apiUrl + `/seller/${currentUserId}`, { headers }).pipe(
      map(response => response.content),
      catchError(this.handleError)
    );
  }

  updateProduct(
    id: string,
    productData: Partial<CreateProductRequest>,
  ): Observable<ProductResponse> {
    const token = this.authService.getToken();

    if (!token) {
      return throwError(() => new Error("No authentication token available"));
    }

    const formData = new FormData();

    if (productData.name) formData.append("name", productData.name);
    if (productData.description)
      formData.append("description", productData.description);
    if (productData.price)
      formData.append("price", productData.price.toString());
    if (productData.quantity)
      formData.append("quantity", productData.quantity.toString());

    if (productData.images && productData.images.length > 0) {
      productData.images.forEach((image) => {
        formData.append("images", image);
      });
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.http
      .put<ProductResponse>(`${this.apiUrl}/${id}`, formData, { headers })
      .pipe(catchError(this.handleError));
  }

  deleteProduct(id: string): Observable<any> {
    const token = this.authService.getToken();

    if (!token) {
      return throwError(() => new Error("No authentication token available"));
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    });

    return this.http
      .delete(`${this.apiUrl}/${id}`, { headers })
      .pipe(catchError(this.handleError));
  }

  getSellerOrders(sellerId: string): Observable<SubOrder[]> {
    const token = this.authService.getToken();
    if (!token) {
      return throwError(() => new Error("No authentication token available"));
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    });

    return this.http
      .get<any>(`${environment.apiUrl}/api/sub-orders/seller/${sellerId}`, { headers })
      .pipe(
        map(response => response.content),
        catchError(this.handleError)
      );
  }

  updateSubOrderStatus(subOrderId: string, status: string): Observable<SubOrder> {
    const token = this.authService.getToken();
    if (!token) {
      return throwError(() => new Error("No authentication token available"));
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    return this.http
      .patch<SubOrder>(
        `${environment.apiUrl}/api/sub-orders/${subOrderId}/status`, 
        { status }, 
        { 
          headers
        }
      )
      .pipe(catchError(this.handleError));
  }

  private handleError = (error: HttpErrorResponse) => {
    console.error("HTTP Error:", error);

    if (error.status === 401) {
      console.error("Authentication failed - token may be invalid or expired");
      // Optionally redirect to login
      // this.router.navigate(['/login']);
    } else if (error.status === 403) {
      console.error("Access forbidden - insufficient permissions");
    } else if (error.status === 0) {
      console.error("Network error - check if backend is running");
    }

    return throwError(() => error);
  };

  getToken(): string | null {
    return this.authService.getToken();
  }

  getUserId(token: string): string | null {
    return this.jwtService.getUserId(token);
  }
}
