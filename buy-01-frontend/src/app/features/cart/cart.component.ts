import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from './services/cart.service';
import { OrderService } from '../orders/services/order.service';
import { ProductService } from '../products/services/product.service';
import { ToastService } from '../../shared/services/toast.service';
import { Order, OrderItem } from '../orders/models/order.models';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LucideAngularModule, Trash2, Minus, Plus, CreditCard, ShoppingBag, ArrowRight, ArrowLeft } from 'lucide-angular';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, LucideAngularModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {
  cart: Order | null = null;
  isLoading = false;
  isProcessing = false;
  error: string | null = null;
  selectedPaymentMethod = 'WAVE';
  
  // Cache for product details (images, names)
  productDetails: Map<string, any> = new Map();

  // Icons
  readonly Trash2 = Trash2;
  readonly Minus = Minus;
  readonly Plus = Plus;
  readonly CreditCard = CreditCard;
  readonly ShoppingBag = ShoppingBag;
  readonly ArrowRight = ArrowRight;
  readonly ArrowLeft = ArrowLeft;

  private cartService = inject(CartService);
  private orderService = inject(OrderService);
  private productService = inject(ProductService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  ngOnInit() {
    this.loadCart();
  }

  loadCart() {
    this.isLoading = true;
    this.cartService.getCart().subscribe({
      next: (cart) => {
        this.cart = cart;
        if (cart && cart.items) {
          this.loadProductDetails(cart.items);
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading cart', err);
        // 404 means empty cart usually in this API design
        if (err.status !== 404) {
          this.error = 'Could not load cart.';
        }
        this.isLoading = false;
      }
    });
  }

  loadProductDetails(items: OrderItem[]) {
    const requests = items.map(item => 
      this.productService.getOneProduct(item.productId).pipe(
        catchError(() => of(null)),
        map(product => ({ id: item.productId, product }))
      )
    );

    if (requests.length === 0) return;

    forkJoin(requests).subscribe(results => {
      results.forEach(res => {
        if (res && res.product) {
          this.productDetails.set(res.id, res.product);
        }
      });
    });
  }

  updateQuantity(item: OrderItem, change: number) {
    const newQuantity = item.quantity + change;
    if (newQuantity < 1) return;
    
    this.isProcessing = true;
    this.cartService.getCart().subscribe({
      next: (cart) => {
        if (cart) {
          this.performUpdateQuantity(item, newQuantity, cart.id);
        } else {
          this.isProcessing = false;
        }
      },
      error: (err) => {
        console.error('Error fetching cart for update', err);
        this.isProcessing = false;
      }
    });
  }

  private performUpdateQuantity(item: OrderItem, newQuantity: number, cartId: string) {
    this.cartService.updateCartItem(item.productId, newQuantity, cartId).subscribe({
      next: (updatedCart) => {
        this.cart = updatedCart;
        this.isProcessing = false;
      },
      error: (err) => {
        console.error('Error updating quantity', err);
        this.isProcessing = false;
      }
    });
  }

  removeItem(productId: string) {
     if (!this.cart) return;

    this.isProcessing = true;
    this.cartService.removeFromCart(productId, this.cart.id).subscribe({
      next: () => {
        this.loadCart();
        this.isProcessing = false;
      },
      error: (err) => {
        console.error('Error removing item', err);
        this.isProcessing = false;
      }
    });
  }

  checkout() {
    if (!this.cart) return;

    this.isProcessing = true;
    this.orderService.placeOrder(this.cart.id, this.selectedPaymentMethod).subscribe({
      next: () => {
        this.toastService.success('Success', 'Order placed successfully!');
        this.router.navigate(['/products']); // Or /orders if you implement it
      },
      error: (err) => {
        console.error('Checkout failed', err);
        this.toastService.error('Error', 'Checkout failed. Please try again.');
        this.isProcessing = false;
      }
    });
  }

  getProductName(productId: string): string {
    return this.productDetails.get(productId)?.name || 'Loading Product...';
  }

  getProductPrice(productId: string): number {
    return this.productDetails.get(productId)?.price || 0;
  }

  getProductImage(productId: string): string {
    const product = this.productDetails.get(productId);
    if (product && product.images && product.images.length > 0) {
      return product.images[0].imageUrl;
    }
    return 'assets/images/placeholder.png';
  }
  
  formatPrice(price: number): string {
    return new Intl.NumberFormat('fr-SN', { style: 'currency', currency: 'XOF' }).format(price);
  }
}
