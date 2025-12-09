export interface OrderItem {
  productId: string;
  productName?: string; // Optional, populated by frontend if missing in DTO
  quantity: number;
  price: number;
  totalPrice?: number;
}

export interface Order {
  id: string;
  userId: string;
  total: number;
  status: 'CART' | 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  paymentMethod: string;
  createdAt: string;
  items: OrderItem[]; // Mapped from orderItemList in backend
}

export interface CartUpdateRequest {
  productId: string;
  quantity: number;
}

export interface OrderCommandRequest {
  status: string;
  paymentMethod: string;
}

export interface ProductStatistic {
  productId: string;
  productName: string;
  totalQuantity: number;
  totalRevenue: number;
  orderCount: number;
}

export interface UserStatistics {
  userId: string;
  totalSpent: number;
  totalOrders: number;
  mostPurchasedProducts: ProductStatistic[];
  bestSellingProducts: ProductStatistic[];
}

export interface SubOrder {
  id: string;
  parentOrderId: string;
  sellerId: string;
  items: OrderItem[];
  subTotal: number;
  status: 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
  userId: string;
  customerName?: string; // Optional, might need to fetch user details
  customerEmail?: string;
  shippingAddress?: string;
  paymentMethod?: string; // Might come from parent order
}
