import { Routes } from "@angular/router";
import { SignComponent } from "./auth/components/sign/sign.component";
import { ProductListingComponent } from "./features/products/components/product-listing/product-listing.component";
import { authGuard } from "./auth/guards/auth.guard";
import { sellerGuard } from "./auth/guards/seller.guard";
import { ProductDetailsComponent } from "./features/products/components/product-details/product-details.component";
import { MyProductsComponent } from "./features/seller/components/my-products/my-products.component";
import { CreateProductComponent } from "./features/seller/components/create-product/create-product.component";
import { EditProductComponent } from "./features/seller/components/edit-product/edit-product.component";
import { MyAccountComponent } from "./features/seller/components/my-account/my-account.component";
import { NotFoundComponent } from "./shared/components/not-found/not-found.component";

export const routes: Routes = [
  { path: "auth", component: SignComponent },
  {
    path: "products",
    component: ProductListingComponent,
    // Remove authGuard - allow guest access
  },
  {
    path: "products/:id",
    component: ProductDetailsComponent,
    // Allow guest access to product details
  },
  {
    path: "cart",
    loadComponent: () =>
      import("./features/cart/cart.component").then((m) => m.CartComponent),
    canActivate: [authGuard], // Only logged-in users
  },
  {
    path: "orders",
    loadComponent: () =>
      import("./features/orders/components/my-orders/my-orders.component").then(
        (m) => m.MyOrdersComponent,
      ),
    canActivate: [authGuard], // Only logged-in users
  },

  {
    path: "seller/my-products",
    component: MyProductsComponent,
    canActivate: [sellerGuard], // Only sellers
  },
  {
    path: "seller/create-product",
    component: CreateProductComponent,
    canActivate: [sellerGuard], // Only sellers
  },
  {
    path: "seller/orders",
    loadComponent: () =>
      import("./features/seller/components/seller-orders/seller-orders.component").then(
        (m) => m.SellerOrdersComponent,
      ),
    canActivate: [sellerGuard],
  },
  {
    path: "seller/edit-product/:id",
    component: EditProductComponent,
    canActivate: [sellerGuard], // Only sellers
  },
  {
    path: "seller/my-account",
    component: MyAccountComponent,
    canActivate: [authGuard], // Only authenticated users
  },
  { path: "", redirectTo: "products", pathMatch: "full" },
  { path: "**", component: NotFoundComponent }, // Wildcard route for 404 errors
];
