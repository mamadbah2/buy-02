import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, switchMap, forkJoin, of, catchError } from 'rxjs';
import { LucideAngularModule, Package, Calendar, CreditCard, ChevronRight, X, ShoppingBag, Clock, CheckCircle, Truck, AlertCircle, Filter, Search } from 'lucide-angular';
import { SellerService } from '../../services/seller.service';
import { AuthService } from '../../../../auth/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { SubOrder, OrderItem } from '../../../orders/models/order.models';
import { ProductService } from '../../../products/services/product.service';
import { UserService } from '../../../../shared/services/user.service';
import { OrderService } from '../../../orders/services/order.service';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './seller-orders.component.html',
  styleUrl: './seller-orders.component.css'
})
export class SellerOrdersComponent implements OnInit, OnDestroy {
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
  readonly Filter = Filter;
  readonly Search = Search;

  orders: SubOrder[] = [];
  filteredOrders: SubOrder[] = [];
  isLoading = false;
  selectedOrder: SubOrder | null = null;
  productDetails: Map<string, any> = new Map();
  userDetails: Map<string, any> = new Map();
  
  // Filters
  statusFilter: string = 'ALL';
  searchQuery: string = '';

  private destroy$ = new Subject<void>();

  get totalRevenue(): number {
    return this.orders.reduce((acc, order) => acc + order.subTotal, 0);
  }

  get pendingOrdersCount(): number {
    return this.orders.filter(o => o.status === 'PENDING').length;
  }

  get completedOrdersCount(): number {
    return this.orders.filter(o => ['DELIVERED', 'SHIPPED'].includes(o.status)).length;
  }

  constructor(
    private sellerService: SellerService,
    private authService: AuthService,
    private toastService: ToastService,
    private productService: ProductService,
    private userService: UserService,
    private orderService: OrderService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  navigateToProduct(productId: string): void {
    this.closeModal();
    this.router.navigate(['/products', productId]);
  }

  loadOrders(): void {
    this.isLoading = true;
    
    this.authService.getCurrentUser()
      .pipe(
        takeUntil(this.destroy$),
        switchMap(user => {
          if (!user || !user.id) {
            throw new Error('User ID not found');
          }
          return this.sellerService.getSellerOrders(user.id);
        })
      )
      .subscribe({
        next: (orders) => {
          console.log('Fetched seller orders:', orders);
          // Ensure items is an array to prevent errors and map itemsList if present
          const safeOrders = (orders || []).map((o: any) => ({ 
            ...o, 
            items: o.items || o.itemsList || [],
            parentOrderId: o.parentOrderId || o.orderId // Fallback for parentOrderId
          }));
          
          this.orders = safeOrders.sort((a: SubOrder, b: SubOrder) => 
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
          console.log('Sorted seller orders:', this.orders);
          this.loadUserDetails(this.orders);
          this.applyFilters();
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error loading seller orders', err);
          this.toastService.showError('Failed to load orders');
          this.isLoading = false;
        }
      });
  }

  loadUserDetails(orders: SubOrder[]) {
    const userIds = [...new Set(orders.map(o => o.userId).filter(id => id && !this.userDetails.has(id)))];
    
    if (userIds.length === 0) return;

    const requests = userIds.map(id => 
      this.userService.getUserProfile(id).pipe(
        catchError(() => of(null))
      )
    );

    forkJoin(requests)
      .pipe(takeUntil(this.destroy$))
      .subscribe(users => {
        users.forEach(user => {
          if (user) {
            this.userDetails.set(user.id, user);
          }
        });
        // Trigger change detection or re-apply filters if needed to update view
        this.applyFilters();
      });
  }

  getCustomerName(userId: string): string {
    const user = this.userDetails.get(userId);
    return user ? user.name : 'Unknown Customer';
  }

  getCustomerEmail(userId: string): string {
    const user = this.userDetails.get(userId);
    return user ? user.email : '';
  }

  applyFilters(): void {
    this.filteredOrders = this.orders.filter(order => {
      const customerName = this.getCustomerName(order.userId);
      const matchesStatus = this.statusFilter === 'ALL' || order.status === this.statusFilter;
      const matchesSearch = !this.searchQuery || 
        order.id.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        (customerName && customerName.toLowerCase().includes(this.searchQuery.toLowerCase()));
      
      return matchesStatus && matchesSearch;
    });
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  viewOrderDetails(order: SubOrder): void {
    this.selectedOrder = order;
    document.body.style.overflow = 'hidden';
    this.loadProductDetails(order.items);

    console.log('Viewing order details:', order);
    console.log('Parent Order ID:', order.parentOrderId);

    // Fetch parent order for payment details
    if (order.parentOrderId) {
      this.orderService.getOrderById(order.parentOrderId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (parentOrder) => {
            console.log('Fetched parent order:', parentOrder);
            if (this.selectedOrder && this.selectedOrder.id === order.id) {
              this.selectedOrder = {
                ...this.selectedOrder,
                paymentMethod: parentOrder.paymentMethod
              };
              console.log('Updated selected order with payment method:', this.selectedOrder.paymentMethod);
            }
          },
          error: (err) => console.error('Error fetching parent order', err)
        });
    } else {
      console.warn('No parentOrderId found for this sub-order');
    }
  }

  loadProductDetails(items: OrderItem[]) {
    const itemsToFetch = items.filter(item => !this.productDetails.has(item.productId));
    
    if (itemsToFetch.length > 0) {
      const requests = itemsToFetch.map(item => 
        this.productService.getOneProduct(item.productId).pipe(
          catchError(() => of(null))
        )
      );

      forkJoin(requests)
        .pipe(takeUntil(this.destroy$))
        .subscribe(products => {
          products.forEach(product => {
            if (product) {
              this.productDetails.set(product.id, product);
            }
          });
          this.updateSelectedOrderProductNames();
        });
    } else {
      this.updateSelectedOrderProductNames();
    }
  }

  updateSelectedOrderProductNames() {
    if (!this.selectedOrder) return;
    
    const updatedItems = this.selectedOrder.items.map(item => {
      const product = this.productDetails.get(item.productId);
      if (product) {
        return { ...item, productName: product.name };
      }
      return item;
    });
    
    this.selectedOrder = { ...this.selectedOrder, items: updatedItems };
  }

  getProductImage(productId: string): string {
    const product = this.productDetails.get(productId);
    if (product && product.images && product.images.length > 0) {
      return product.images[0].imageUrl;
    }
    return 'assets/images/placeholder.png';
  }

  closeModal(): void {
    this.selectedOrder = null;
    document.body.style.overflow = '';
  }

  printOrder(): void {
    window.print();
  }

  updateStatus(order: SubOrder, newStatus: string): void {
    if (order.status === newStatus) return;

    this.isLoading = true;
    this.sellerService.updateSubOrderStatus(order.id, newStatus)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedOrder) => {
          // Update the order in the list
          const index = this.orders.findIndex(o => o.id === order.id);
          if (index !== -1) {
            this.orders[index] = { ...this.orders[index], status: updatedOrder.status as any };
            this.applyFilters();
          }
          
          // Update selected order if open
          if (this.selectedOrder && this.selectedOrder.id === order.id) {
            this.selectedOrder = { ...this.selectedOrder, status: updatedOrder.status as any };
          }

          this.toastService.showSuccess(`Order status updated to ${newStatus}`);
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error updating order status', err);
          this.toastService.showError('Failed to update order status');
          this.isLoading = false;
        }
      });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('fr-SN', { style: 'currency', currency: 'XOF' }).format(price);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
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

  getPaymentMethodLabel(method: string | undefined): string {
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
