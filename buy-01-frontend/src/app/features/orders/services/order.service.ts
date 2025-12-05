import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Order, OrderCommandRequest, UserStatistics } from '../models/order.models';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/orders`;

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  getOrdersByUser(userId: string): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/user/${userId}`);
  }

  getUserStatistics(userId: string): Observable<UserStatistics> {
    return this.http.get<UserStatistics>(`${this.apiUrl}/statistics/user/${userId}`);
  }

  /**
   * Converts a CART to a PENDING order
   */
  placeOrder(orderId: string, paymentMethod: string): Observable<Order> {
    const body: OrderCommandRequest = {
      status: 'PENDING',
      paymentMethod
    };
    return this.http.patch<Order>(`${this.apiUrl}/${orderId}/command`, body);
  }
}
