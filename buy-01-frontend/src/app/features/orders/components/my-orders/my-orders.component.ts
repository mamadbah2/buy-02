import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LucideAngularModule, Package, Calendar, CreditCard, ChevronRight, X, ShoppingBag, Clock, CheckCircle, Truck, AlertCircle } from 'lucide-angular';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../../../auth/services/auth.service';
import { ProductService } from '../../../products/services/product.service';
import { Order, OrderItem, SubOrder } from '../../models/order.models';
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
  subOrders: SubOrder[] = [];
  isLoading = false;
  isLoadingSubOrders = false;
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
    this.subOrders = [];
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
    
    if (order.status !== 'CART') {
      this.isLoadingSubOrders = true;
      this.orderService.getSubOrdersByOrderId(order.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (subOrders) => {
            this.subOrders = subOrders;
            const allItems = subOrders.flatMap(so => so.items);
            this.loadProductDetails(allItems);
            this.isLoadingSubOrders = false;
            this.checkAndSyncOrderStatus();
          },
          error: (err) => {
            console.error('Error loading sub-orders', err);
            this.isLoadingSubOrders = false;
            // Fallback to order items if sub-orders fail
            if (order.items) {
              this.loadProductDetails(order.items);
            }
          }
        });
    } else if (order.items) {
      this.loadProductDetails(order.items);
    }
  }

  checkAndSyncOrderStatus(): void {
    if (!this.selectedOrder || this.subOrders.length === 0) return;

    const allDelivered = this.subOrders.every(so => so.status === 'DELIVERED');
    
    if (allDelivered && this.selectedOrder.status !== 'DELIVERED') {
      this.orderService.updateOrderStatus(this.selectedOrder.id, 'DELIVERED', this.selectedOrder.paymentMethod)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (updatedOrder) => {
            if (this.selectedOrder) {
              this.selectedOrder.status = updatedOrder.status;
              // Update the order in the main list as well
              const index = this.orders.findIndex(o => o.id === updatedOrder.id);
              if (index !== -1) {
                this.orders[index] = updatedOrder;
              }
              this.toastService.showSuccess('Order marked as Delivered');
            }
          },
          error: (err) => console.error('Failed to sync order status', err)
        });
    }
  }

  getOrderProgress(): number {
    if (!this.subOrders.length) return 0;
    const totalProgress = this.subOrders.reduce((acc, so) => acc + this.getStatusProgress(so.status), 0);
    return Math.round(totalProgress / this.subOrders.length);
  }

  getStatusProgress(status: string): number {
    switch (status) {
      case 'PENDING': return 10;
      case 'CONFIRMED': return 25;
      case 'PROCESSING': return 50;
      case 'SHIPPED': return 75;
      case 'DELIVERED': return 100;
      case 'CANCELLED': return 0;
      default: return 0;
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
