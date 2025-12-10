import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Order, OrderCommandRequest, UserStatistics, SubOrder } from '../models/order.models';

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

  getSubOrdersByOrderId(orderId: string): Observable<SubOrder[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${orderId}/sub-orders`).pipe(
      map(subOrders => subOrders.map(so => ({
        ...so,
        items: so.items || so.itemsList || []
      })))
    );
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
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/confirm`, body);
  }

  updateOrderStatus(orderId: string, status: string, paymentMethod: string): Observable<Order> {
    return this.http.patch<Order>(`${this.apiUrl}/${orderId}/command`, { status, paymentMethod });
  }
}
