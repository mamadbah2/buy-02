import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LucideAngularModule, Package, Calendar, CreditCard, ChevronRight, X, ShoppingBag, Clock, CheckCircle, Truck, AlertCircle } from 'lucide-angular';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../../../auth/services/auth.service';
import { ProductService } from '../../../products/services/product.service';
import { Order, OrderItem } from '../../models/order.models';
import { ToastService } from '../../../../shared/services/toast.service';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './my-orders.component.html',
  styleUrl: './my-orders.component.css'
})
export class MyOrdersComponent implements OnInit, OnDestroy {
  // Icons
  readonly Package = Package;
  readonly Calendar = Calendar;
  readonly CreditCard = CreditCard;
  readonly ChevronRight = ChevronRight;
  readonly X = X;
  readonly ShoppingBag = ShoppingBag;
  readonly Clock = Clock;
  readonly CheckCircle = CheckCircle;
  readonly Truck = Truck;
  readonly AlertCircle = AlertCircle;

  orders: Order[] = [];
  isLoading = false;
  selectedOrder: Order | null = null;
  productDetails: Map<string, any> = new Map();
  private destroy$ = new Subject<void>();

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private productService: ProductService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrders(): void {
    this.isLoading = true;
    this.authService.getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          if (user && user.id) {
            this.orderService.getOrdersByUser(user.id)
              .pipe(takeUntil(this.destroy$))
              .subscribe({
                next: (orders) => {
                  this.orders = orders.filter(o => o.status !== 'CART').sort((a, b) => 
                    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
                  );
                  this.isLoading = false;
                },
                error: (err) => {
                  console.error('Error loading orders', err);
                  this.toastService.showError('Failed to load orders');
                  this.isLoading = false;
                }
              });
          } else {
            this.isLoading = false;
          }
        },
        error: (err) => {
          console.error('Error getting user', err);
          this.isLoading = false;
        }
      });
  }

  viewOrderDetails(order: Order): void {
    this.selectedOrder = order;
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
    if (order.items) {
      this.loadProductDetails(order.items);
    }
  }

  loadProductDetails(items: OrderItem[]) {
    if (!items) return;
    const itemsToFetch = items.filter(item => !this.productDetails.has(item.productId));
    
    if (itemsToFetch.length === 0) return;

    const requests = itemsToFetch.map(item => 
      this.productService.getOneProduct(item.productId).pipe(
        catchError(() => of(null)),
        map(product => ({ id: item.productId, product }))
      )
    );

    forkJoin(requests).pipe(takeUntil(this.destroy$)).subscribe(results => {
      results.forEach(res => {
        if (res && res.product) {
          this.productDetails.set(res.id, res.product);
        }
      });
    });
  }

  getProductName(productId: string): string {
    const product = this.productDetails.get(productId);
    return product ? product.name : 'Loading...';
  }

  getProductPrice(productId: string): number {
    const product = this.productDetails.get(productId);
    return product ? product.price : 0;
  }

  getProductImage(productId: string): string {
    const product = this.productDetails.get(productId);
    if (product && product.images && product.images.length > 0) {
      return product.images[0].imageUrl;
    }
    return 'assets/images/placeholder.png'; // Fallback
  }

  closeModal(): void {
    this.selectedOrder = null;
    document.body.style.overflow = ''; // Restore scrolling
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('fr-SN', { style: 'currency', currency: 'XOF' }).format(price);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMED': return 'bg-blue-100 text-blue-800';
      case 'PROCESSING': return 'bg-purple-100 text-purple-800';
      case 'SHIPPED': return 'bg-indigo-100 text-indigo-800';
      case 'DELIVERED': return 'bg-green-100 text-green-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusIcon(status: string): any {
    switch (status) {
      case 'PENDING': return this.Clock;
      case 'CONFIRMED': return this.CheckCircle;
      case 'PROCESSING': return this.Package;
      case 'SHIPPED': return this.Truck;
      case 'DELIVERED': return this.CheckCircle;
      case 'CANCELLED': return this.AlertCircle;
      default: return this.Package;
    }
  }

  getPaymentMethodLabel(method: string): string {
    switch (method) {
      case 'WAVE': return 'Wave Mobile Money';
      case 'ORANGE_MONEY': return 'Orange Money';
      case 'CASH_ON_DELIVERY': return 'Cash on Delivery';
      case 'PAYPAL': return 'PayPal';
      case 'DEBIT_CARD': return 'Credit Card';
      default: return method || 'Unknown';
    }
  }
}
