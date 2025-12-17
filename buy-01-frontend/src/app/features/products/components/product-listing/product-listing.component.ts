import { Component, inject, OnInit } from "@angular/core";
import { ProductService } from "../../services/product.service";
import { ProductModels } from "../../models/product.models";
import { RouterLink } from "@angular/router";
import { AuthService } from "../../../../auth/services/auth.service";
import { CommonModule } from "@angular/common";
import { JwtService } from "../../../../shared/services/jwt.service";
import { SearchComponent } from '../search/search.component';
import { CartService } from "../../../cart/services/cart.service";
import { ToastService } from "../../../../shared/services/toast.service";

interface PaginationState {
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  isFirst: boolean;
  isLast: boolean;
}

@Component({
  selector: "app-product-listing",
  imports: [RouterLink, CommonModule, SearchComponent],
  templateUrl: "./product-listing.component.html",
  styleUrl: "./product-listing.component.css",
})
export class ProductListingComponent implements OnInit {
  allProducts: ProductModels[] = [];
  filteredProducts: ProductModels[] = [];
  currentUser: any = null;
  isLoading = false;
  selectedSort: "" | "name" | "price" = "";
  sortDirection: "ASC" | "DESC" = "DESC";
  searchTerm: string = "";
  readonly pageSizeOptions = [12, 20, 36];
  private readonly defaultPageSize = 20;
  pagination: PaginationState = {
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    pageSize: this.defaultPageSize,
    isFirst: true,
    isLast: true,
  };

  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private jwtService = inject(JwtService);
  private toastService = inject(ToastService);

  // Helper methods for template
  getQuantityAsNumber(quantity: string): number {
    return Number(quantity) || 0;
  }

  getPriceAsNumber(price: string): number {
    return Number(price) || 0;
  }

  ngOnInit() {
    this.checkUserStatus();
    this.loadProducts();
  }

  private checkUserStatus() {
    if (this.authService.isLoggedIn()) {
      const token = this.authService.getToken();
      if (token) {
        this.currentUser = this.jwtService.decodeToken(token);
        if (!this.currentUser) {
          console.error("Error parsing user data");
        }
      }
    }
  }

  private loadProducts(page: number = this.pagination.currentPage) {
    console.log('ProductListingComponent: loadProducts called. Page:', page, 'SearchTerm:', this.searchTerm);
    this.isLoading = true;
    const safePage = Math.max(0, page);
    const commonParams = {
      page: safePage,
      size: this.pagination.pageSize
    };

    const hasSearch = this.searchTerm && this.searchTerm.trim().length > 0;
    console.log('ProductListingComponent: hasSearch:', hasSearch);

    const request$ = hasSearch
      ? this.productService.searchProducts(
          { query: this.searchTerm.trim() },
          commonParams,
        )
      : this.productService.getProductList(commonParams);

    request$
      .subscribe({
        next: (response) => {
          console.log("Products loaded successfully:", response);
          this.pagination = {
            totalElements: response.totalElements,
            totalPages: response.totalPages,
            currentPage: response.number,
            pageSize: response.size,
            isFirst: response.first,
            isLast: response.last,
          };
          this.allProducts = response.content ?? [];
          this.filteredProducts = [...this.allProducts];
          this.applyLocalSort();
          this.isLoading = false;
        },
        error: (err) => {
          console.error("Error loading products:", err);
          this.isLoading = false;
          this.allProducts = [];
          this.filteredProducts = [];
        },
      });
  }

  get isGuest(): boolean {
    return !this.authService.isLoggedIn();
  }

  get isClient(): boolean {
    return this.currentUser?.role === "CLIENT";
  }

  get isSeller(): boolean {
    return this.currentUser?.role === "SELLER";
  }

  addToCart(product: ProductModels) {
    if (this.isGuest) {
      // Enhanced guest experience
      this.showLoginPrompt();
      return;
    }

    // Enhanced cart logic
    console.log("Adding to cart:", product);
    
    this.cartService.addItemToCart(product.id, 1, Number(product.price)).subscribe({
      next: () => {
        this.showSuccessMessage(`${product.name} added to cart!`);
      },
      error: (err) => {
        console.error('Failed to add to cart', err);
        this.toastService.error('Error', 'Failed to add to cart');
      }
    });
  }

  formatPrice(price: number | string): string {
    const numericPrice = typeof price === "string" ? parseFloat(price) : price;
    return new Intl.NumberFormat("fr-SN", {
      style: "currency",
      currency: "XOF",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(numericPrice);
  }

  // Enhanced user interaction methods
  private showLoginPrompt() {
    // You can replace this with a more sophisticated modal or toast
    const shouldRedirect = confirm(
      "Please login to add items to your cart. Would you like to go to the login page now?",
    );
    if (shouldRedirect) {
      // Navigate to auth page
      window.location.href = "/auth";
    }
  }

  private showSuccessMessage(message: string) {
    this.toastService.success('Success', message);
  }

  // Enhanced product filtering methods (you can add these features)
  filterByCategory(category: string) {
    // Implementation for category filtering
    console.log(`Filtering by category: ${category}`);
  }

  sortProducts(sortBy: "name" | "price") {
    if (this.selectedSort === sortBy) {
      this.sortDirection = this.sortDirection === "ASC" ? "DESC" : "ASC";
    } else {
      this.selectedSort = sortBy;
      this.sortDirection = sortBy === "name" ? "ASC" : "DESC";
    }

    this.applyLocalSort();
  }

  private applyLocalSort() {
    if (!this.selectedSort) return;

    this.filteredProducts.sort((a, b) => 
      this.compareProducts(a, b, this.selectedSort as "name" | "price")
    );
    
    if (this.sortDirection === "DESC") {
      this.filteredProducts.reverse();
    }
  }

  // Method to refresh products
  refreshProducts() {
    this.allProducts = [];
    this.filteredProducts = [];
    this.selectedSort = "";
    this.sortDirection = "DESC";
    this.searchTerm = "";
    this.pagination = {
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: this.defaultPageSize,
      isFirst: true,
      isLast: true,
    };
    this.loadProducts();
  }

  // Search functionality
  onSearch(term: string) {
    console.log('ProductListingComponent: onSearch called with term:', term);
    this.searchTerm = term;
    // Reset to first page when searching
    this.loadProducts(0);
  }

  private applyFilters() {
    const term = this.searchTerm.trim().toLowerCase();
    let products = [...this.allProducts];

    if (term) {
      products = products.filter(
        (product) =>
          product.name.toLowerCase().includes(term) ||
          (product.description &&
            product.description.toLowerCase().includes(term)),
      );
    }

    if (this.selectedSort) {
      const activeSort = this.selectedSort;
      products.sort((a, b) => this.compareProducts(a, b, activeSort));
      if (this.sortDirection === "DESC") {
        products.reverse();
      }
    }

    this.filteredProducts = products;
  }

  // Track by function for better performance
  trackByProductId(index: number, product: ProductModels): string {
    return product.id;
  }

  // Check if product is new (created within last 30 days)
  isNewProduct(product: ProductModels): boolean {
    // This would typically check a createdAt date
    // For now, return false as we don't have that field
    return false;
  }

  get rangeStart(): number {
    if (!this.pagination.totalElements) {
      return 0;
    }
    return this.pagination.currentPage * this.pagination.pageSize + 1;
  }

  get rangeEnd(): number {
    if (!this.pagination.totalElements) {
      return 0;
    }
    return Math.min(
      (this.pagination.currentPage + 1) * this.pagination.pageSize,
      this.pagination.totalElements,
    );
  }

  get hasMultiplePages(): boolean {
    return this.pagination.totalPages > 1;
  }

  nextPage(): void {
    if (this.pagination.isLast || this.isLoading) return;
    this.loadProducts(this.pagination.currentPage + 1);
  }

  previousPage(): void {
    if (this.pagination.isFirst || this.isLoading) return;
    this.loadProducts(this.pagination.currentPage - 1);
  }

  onPageSizeChange(size: string): void {
    const newSize = Number(size);
    if (!newSize || newSize === this.pagination.pageSize) return;
    this.pagination.pageSize = newSize;
    this.loadProducts(0);
  }

  goToPage(page: number): void {
    if (
      page < 0 ||
      page >= this.pagination.totalPages ||
      page === this.pagination.currentPage
    ) {
      return;
    }
    this.loadProducts(page);
  }

  private compareProducts(
    a: ProductModels,
    b: ProductModels,
    sortBy: "name" | "price",
  ): number {
    switch (sortBy) {
      case "name":
        return a.name.localeCompare(b.name);
      case "price":
        return Number(a.price) - Number(b.price);
      default:
        return 0;
    }
  }
}
