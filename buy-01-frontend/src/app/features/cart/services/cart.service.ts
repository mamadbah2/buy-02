import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, switchMap, map, tap } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Order, CartUpdateRequest } from '../../orders/models/order.models';
import { AuthService } from '../../../auth/services/auth.service';
import { JwtService } from '../../../shared/services/jwt.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private jwtService = inject(JwtService);
  private apiUrl = `${environment.apiUrl}/api/cart`;
  private ordersUrl = `${environment.apiUrl}/api/orders`;

  private getUserId(): string | null {
    const token = this.authService.getToken();
    if (!token) return null;
    const decoded = this.jwtService.decodeToken(token);
    return decoded?.userID || decoded?.['id'] || null;
  }

  /**
   * Get the current user's cart
   */
  getCart(): Observable<Order> {
    const userId = this.getUserId();
    if (!userId) return throwError(() => new Error('User not authenticated'));
    
    return this.http.get<Order>(`${this.apiUrl}/user/${userId}`);
  }

  /**
   * Add an item to the cart. 
   * If item exists, it increments the quantity.
   * If item does not exist, it adds it.
   */
  addItemToCart(productId: string, quantityToAdd: number, price: number = 0): Observable<Order> {
    return this.getCart().pipe(
      switchMap(cart => {
        let newQuantity = quantityToAdd;
        
        if (cart && cart.items) {
          const existingItem = cart.items.find(item => item.productId === productId);
          if (existingItem) {
            newQuantity = existingItem.quantity + quantityToAdd;
          }
        }
        
        return this.updateCartItem(productId, newQuantity, cart.id);
      }),
      catchError(err => {
        if (err.status === 404) {
          console.log('Cart not found, creating a new cart');
          return this.createCart(productId, quantityToAdd, price);
        }
        return throwError(() => err);
      })
    );
  }

  createCart(productId: string, quantity: number, price: number): Observable<Order> {
    const userId = this.getUserId();
    if (!userId) return throwError(() => new Error('User not authenticated'));

    const body = {
      userId,
      paymentMethod: "",
      status: "CART",
      items: [
        {
          productId,
          quantity,
          price
        }
      ]
    };
    return this.http.post<Order>(this.ordersUrl, body);
  }

  /**
   * Update specific item quantity (Directly sets the quantity)
   */
  updateCartItem(productId: string, quantity: number, cartId: string): Observable<Order> {
    const userId = this.getUserId();
    if (!userId) return throwError(() => new Error('User not authenticated'));
    
    const body: CartUpdateRequest = { productId, quantity };
    return this.http.patch<Order>(`${this.apiUrl}/${cartId}`, body);
  }

  /**
   * Remove an item from the cart
   */
  removeFromCart(productId: string, cartId: string): Observable<void> {
    const userId = this.getUserId();
    if (!userId) return throwError(() => new Error('User not authenticated'));
    
    return this.http.delete<void>(`${this.apiUrl}/${cartId}/products/${productId}`);
  }
}
