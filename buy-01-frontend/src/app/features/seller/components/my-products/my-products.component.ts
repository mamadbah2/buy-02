import { Component, OnInit, ChangeDetectorRef } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule, Router } from "@angular/router";
import { FormsModule } from "@angular/forms";
import { SellerService, ProductResponse } from "../../services/seller.service";
import {
  ConfirmationModalComponent,
  ConfirmationModalData,
} from "../../../../shared/components/confirmation-modal/confirmation-modal.component";
import { ToastService } from "../../../../shared/services/toast.service";
import { 
  LucideAngularModule, 
  ChevronRight, 
  RefreshCw, 
  Plus, 
  Package, 
  CheckCircle, 
  PauseCircle, 
  AlertCircle, 
  Search, 
  X, 
  ArrowUp, 
  ArrowDown, 
  LayoutGrid, 
  List, 
  Eye, 
  Edit, 
  Trash2 
} from "lucide-angular";

@Component({
  selector: "app-my-products",
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ConfirmationModalComponent,
    LucideAngularModule
  ],
  templateUrl: "./my-products.component.html",
  styleUrls: ["./my-products.component.css"],
})
export class MyProductsComponent implements OnInit {
  // Icons
  readonly ChevronRight = ChevronRight;
  readonly RefreshCw = RefreshCw;
  readonly Plus = Plus;
  readonly Package = Package;
  readonly CheckCircle = CheckCircle;
  readonly PauseCircle = PauseCircle;
  readonly AlertCircle = AlertCircle;
  readonly Search = Search;
  readonly X = X;
  readonly ArrowUp = ArrowUp;
  readonly ArrowDown = ArrowDown;
  readonly LayoutGrid = LayoutGrid;
  readonly List = List;
  readonly Eye = Eye;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  products: ProductResponse[] = [];
  filteredProducts: ProductResponse[] = [];
  isLoading = true;
  searchTerm = "";
  selectedCategory = "all";
  selectedStatus = "all";
  sortBy = "name";
  sortOrder: "asc" | "desc" = "asc";
  viewMode: "grid" | "list" = "grid";

  // Confirmation modal properties
  showConfirmModal: boolean = false;
  confirmModalData: ConfirmationModalData = {
    title: "",
    message: "",
    type: "danger",
  };
  isDeleting: boolean = false;
  productToDelete: string | null = null;

  constructor(
    private sellerService: SellerService,
    private router: Router,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  private loadProducts(): void {
    this.isLoading = true;

    // Get current user info for validation
    const token = this.sellerService.getToken();
    const currentUserId = token ? this.sellerService.getUserId(token) : null;

    this.sellerService.getMyProducts().subscribe({
      next: (products) => {
        // Additional security check: validate all products belong to current user
        if (currentUserId) {
          const validProducts = products.filter(
            (p) => p.userId === currentUserId,
          );
          this.products = validProducts;
        } else {
          this.products = products;
        }

        this.filteredProducts = [...this.products];
        this.isLoading = false;
        this.applyFilters();
      },
      error: (error) => {
        console.error("Error loading products:", error);
        this.isLoading = false;
        this.products = [];
        this.filteredProducts = [];

        // Handle authentication errors
        if (error.status === 401) {
          this.router.navigate(["/auth"]);
        }
      },
    });
  }

  refreshProducts(): void {
    this.isLoading = true;
    this.loadProducts();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  clearSearch(): void {
    this.searchTerm = "";
    this.applyFilters();
  }

  toggleSortOrder(): void {
    this.sortOrder = this.sortOrder === "asc" ? "desc" : "asc";
    this.applySort();
  }

  applyFilters(): void {
    let filtered = [...this.products];

    // Apply search filter
    if (this.searchTerm) {
      const searchLower = this.searchTerm.toLowerCase();
      filtered = filtered.filter(
        (product) =>
          product.name.toLowerCase().includes(searchLower) ||
          product.description.toLowerCase().includes(searchLower),
      );
    }

    // Apply category filter (removed since backend doesn't have category field)
    // Category filtering can be implemented when backend adds category support

    // Apply status filter based on quantity
    if (this.selectedStatus !== "all") {
      if (this.selectedStatus === "active") {
        filtered = filtered.filter((product) => product.quantity > 0);
      } else if (this.selectedStatus === "out_of_stock") {
        filtered = filtered.filter((product) => product.quantity === 0);
      }
    }

    this.filteredProducts = filtered;
    this.applySort();
  }

  applySort(): void {
    this.filteredProducts.sort((a, b) => {
      let valueA: any = a[this.sortBy as keyof ProductResponse];
      let valueB: any = b[this.sortBy as keyof ProductResponse];

      if (typeof valueA === "string") {
        valueA = valueA.toLowerCase();
        valueB = valueB.toLowerCase();
      }

      if (valueA < valueB) {
        return this.sortOrder === "asc" ? -1 : 1;
      }
      if (valueA > valueB) {
        return this.sortOrder === "asc" ? 1 : -1;
      }
      return 0;
    });
  }

  getStatusClass(quantity: number): string {
    if (quantity === 0) return "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400";
    if (quantity <= 10) return "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400";
    return "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400";
  }

  getStatusLabel(quantity: number): string {
    if (quantity === 0) return "Out of Stock";
    if (quantity <= 10) return "Low Stock";
    return "Active";
  }

  getStockClass(quantity: number): string {
    if (quantity === 0) return "text-red-600 dark:text-red-400";
    if (quantity <= 10) return "text-yellow-600 dark:text-yellow-400";
    return "text-green-600 dark:text-green-400";
  }

  getProductImage(product: ProductResponse): string {
    if (product.images && product.images.length > 0) {
      return product.images[0].imageUrl;
    }
    return "https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=300&h=300&fit=crop";
  }

  onImageError(event: any): void {
    event.target.src =
      "https://images.unsplash.com/photo-1560472354-b33ff0c44a43?w=300&h=300&fit=crop";
  }

  truncateDescription(description: string, length: number = 60): string {
    return description.length > length
      ? description.substring(0, length) + "..."
      : description;
  }

  trackByProductId(index: number, product: ProductResponse): string {
    return product.id;
  }

  setViewMode(mode: "grid" | "list"): void {
    this.viewMode = mode;
  }

  hasActiveFilters(): boolean {
    return this.searchTerm !== "" || this.selectedStatus !== "all";
  }

  clearAllFilters(): void {
    this.searchTerm = "";
    this.selectedStatus = "all";
    this.applyFilters();
  }

  getStartIndex(): number {
    return this.filteredProducts.length > 0 ? 1 : 0;
  }

  getEndIndex(): number {
    return this.filteredProducts.length;
  }

  viewProduct(productId: string): void {
    // Navigate to product detail page
    window.open(`/products/${productId}`, "_blank");
  }

  formatCurrency(amount: number | string): string {
    const numericAmount =
      typeof amount === "string" ? parseFloat(amount) : amount;
    return new Intl.NumberFormat("fr-SN", {
      style: "currency",
      currency: "XOF",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(numericAmount);
  }

  editProduct(productId: string): void {
    this.router.navigate(["/seller/edit-product", productId]);
  }

  deleteProduct(productId: string): void {
    console.log("=== DELETE BUTTON CLICKED ===");
    console.log("Product ID:", productId);
    console.log("View Mode:", this.viewMode);

    const product = this.products.find((p) => p.id === productId);
    const productName = product ? product.name : "this product";

    console.log("Product found:", product);
    this.productToDelete = productId;
    this.confirmModalData = {
      title: "Delete Product",
      message: `Are you sure you want to delete "${productName}"? This action cannot be undone and will permanently remove the product from your store.`,
      confirmText: "Delete",
      cancelText: "Cancel",
      type: "danger",
    };

    this.showConfirmModal = true;
    this.cdr.detectChanges();
  }

  onConfirmDelete(): void {
    if (!this.productToDelete) {
      return;
    }

    this.isDeleting = true;
    this.cdr.detectChanges();

    const productId = this.productToDelete;
    const product = this.products.find((p) => p.id === productId);
    const productName = product ? product.name : "product";

    this.sellerService.deleteProduct(productId).subscribe({
      next: (response) => {
        // Remove product from local array
        this.products = this.products.filter((p) => p.id !== productId);
        this.applyFilters();

        // Show success toast
        this.toastService.productDeleted(productName);

        // Close modal and reset state
        this.closeConfirmModal();
      },
      error: (error) => {
        console.error("Error deleting product:", error);

        // Handle different error types with toast notifications
        if (error.status === 404) {
          this.toastService.warning(
            "Product Not Found",
            "This product no longer exists on the server. Removing from your list.",
          );
          // Remove from local array if it doesn't exist on server
          this.products = this.products.filter((p) => p.id !== productId);
          this.applyFilters();
        } else if (error.status === 403) {
          this.toastService.error(
            "Permission Denied",
            "You don't have permission to delete this product.",
          );
        } else if (error.status === 401) {
          this.toastService.authError();
        } else if (error.status === 0) {
          this.toastService.networkError();
        } else if (error.status >= 500) {
          this.toastService.serverError();
        } else {
          this.toastService.error(
            "Delete Failed",
            `Failed to delete "${productName}". Please try again later.`,
          );
        }

        // Close modal and reset state
        this.closeConfirmModal();
      },
    });
  }

  onCancelDelete(): void {
    this.closeConfirmModal();
  }

  private closeConfirmModal(): void {
    this.showConfirmModal = false;
    this.isDeleting = false;
    this.productToDelete = null;
    this.confirmModalData = {
      title: "",
      message: "",
      type: "danger",
    };
    this.cdr.detectChanges();
  }

  get totalProducts(): number {
    return this.products.length;
  }

  get activeProducts(): number {
    return this.products.filter((p) => p.quantity > 0).length;
  }

  get inactiveProducts(): number {
    return this.products.filter((p) => p.quantity > 0 && p.quantity <= 10)
      .length;
  }

  get outOfStockProducts(): number {
    return this.products.filter((p) => p.quantity === 0).length;
  }
}
