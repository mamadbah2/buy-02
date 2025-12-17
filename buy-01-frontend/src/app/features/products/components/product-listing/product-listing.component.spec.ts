import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductListingComponent } from './product-listing.component';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../../cart/services/cart.service';
import { AuthService } from '../../../../auth/services/auth.service';
import { JwtService } from '../../../../shared/services/jwt.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { ProductModels, ProductPage } from '../../models/product.models';
import { Order } from '../../../orders/models/order.models';

describe('ProductListingComponent', () => {
  let component: ProductListingComponent;
  let fixture: ComponentFixture<ProductListingComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let jwtServiceSpy: jasmine.SpyObj<JwtService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  const mockProduct: ProductModels = {
    id: '1',
    name: 'Test Product',
    description: 'Test Description',
    price: '100',
    quantity: '10',
    userId: 'user1',
    images: []
  };

  const mockProductPage: ProductPage = {
    content: [mockProduct],
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0,
    numberOfElements: 1,
    first: true,
    last: true,
    empty: false
  };

  const mockOrder: Order = {
    id: 'order1',
    userId: 'user1',
    total: 100,
    status: 'CART',
    paymentMethod: 'CARD',
    createdAt: new Date().toISOString(),
    items: []
  };

  beforeEach(async () => {
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductList', 'searchProducts']);
    cartServiceSpy = jasmine.createSpyObj('CartService', ['addItemToCart']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'getToken']);
    jwtServiceSpy = jasmine.createSpyObj('JwtService', ['decodeToken']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [ProductListingComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: JwtService, useValue: jwtServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListingComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load products on init', () => {
      productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
      fixture.detectChanges();
      expect(productServiceSpy.getProductList).toHaveBeenCalled();
      expect(component.allProducts.length).toBe(1);
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should check user status on init', () => {
      productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
      authServiceSpy.isLoggedIn.and.returnValue(true);
      authServiceSpy.getToken.and.returnValue('fake-token');
      jwtServiceSpy.decodeToken.and.returnValue({ 
        role: 'CLIENT',
        userID: 'u1',
        email: 'test@test.com',
        exp: 123,
        iat: 123
      });

      fixture.detectChanges();

      expect(authServiceSpy.isLoggedIn).toHaveBeenCalled();
      expect(jwtServiceSpy.decodeToken).toHaveBeenCalledWith('fake-token');
      expect(component.currentUser.role).toEqual('CLIENT');
    });
  });

  describe('Loading Products', () => {
    it('should call searchProducts when searchTerm is present', () => {
      productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
      fixture.detectChanges(); // init

      component.searchTerm = 'test';
      productServiceSpy.searchProducts.and.returnValue(of(mockProductPage));
      
      // Trigger load via onSearch or directly
      component.onSearch('test');
      
      expect(productServiceSpy.searchProducts).toHaveBeenCalled();
      expect(component.searchTerm).toBe('test');
    });

    it('should handle error when loading products', () => {
      productServiceSpy.getProductList.and.returnValue(throwError(() => new Error('Error')));
      fixture.detectChanges();
      
      expect(component.isLoading).toBeFalse();
      expect(component.allProducts).toEqual([]);
    });
  });

  describe('Cart', () => {
    it('should prompt login for guest user', () => {
      productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
      authServiceSpy.isLoggedIn.and.returnValue(false);
      fixture.detectChanges();

      spyOn(window, 'confirm').and.returnValue(false); // Don't redirect in test
      
      component.addToCart(mockProduct);
      
      expect(window.confirm).toHaveBeenCalled();
      expect(cartServiceSpy.addItemToCart).not.toHaveBeenCalled();
    });

    it('should add to cart for logged in user', () => {
      productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
      authServiceSpy.isLoggedIn.and.returnValue(true);
      fixture.detectChanges();

      cartServiceSpy.addItemToCart.and.returnValue(of(mockOrder));
      
      component.addToCart(mockProduct);
      
      expect(cartServiceSpy.addItemToCart).toHaveBeenCalledWith(mockProduct.id, 1, 100);
      expect(toastServiceSpy.success).toHaveBeenCalled();
    });

    it('should handle add to cart error', () => {
      productServiceSpy.getProductList.and.returnValue(of(mockProductPage));
      authServiceSpy.isLoggedIn.and.returnValue(true);
      fixture.detectChanges();

      cartServiceSpy.addItemToCart.and.returnValue(throwError(() => new Error('Error')));
      
      component.addToCart(mockProduct);
      
      expect(toastServiceSpy.error).toHaveBeenCalled();
    });
  });

  describe('Sorting', () => {
    beforeEach(() => {
      productServiceSpy.getProductList.and.returnValue(of({
        ...mockProductPage,
        content: [
          { ...mockProduct, id: '1', name: 'B Product', price: '200' },
          { ...mockProduct, id: '2', name: 'A Product', price: '100' }
        ]
      }));
      fixture.detectChanges();
    });

    it('should sort by name ASC', () => {
      component.sortProducts('name');
      expect(component.selectedSort).toBe('name');
      expect(component.sortDirection).toBe('ASC');
      expect(component.filteredProducts[0].name).toBe('A Product');
    });

    it('should sort by name DESC when clicked twice', () => {
      component.sortProducts('name');
      component.sortProducts('name');
      expect(component.sortDirection).toBe('DESC');
      expect(component.filteredProducts[0].name).toBe('B Product');
    });

    it('should sort by price ASC', () => {
      // Initial sort for price is DESC in component logic
      component.sortProducts('price'); 
      expect(component.selectedSort).toBe('price');
      expect(component.sortDirection).toBe('DESC');
      expect(component.filteredProducts[0].price).toBe('200');

      // Click again to toggle to ASC
      component.sortProducts('price');
      expect(component.sortDirection).toBe('ASC');
      expect(component.filteredProducts[0].price).toBe('100');
    });
  });

  describe('Pagination', () => {
    beforeEach(() => {
      productServiceSpy.getProductList.and.returnValue(of({
        ...mockProductPage,
        totalPages: 2,
        first: false,
        last: false,
        number: 1
      }));
      fixture.detectChanges();
    });

    it('should go to next page', () => {
      component.pagination.isLast = false;
      component.pagination.currentPage = 0;
      component.nextPage();
      expect(productServiceSpy.getProductList).toHaveBeenCalledTimes(2); // Init + nextPage
    });

    it('should go to previous page', () => {
      component.pagination.isFirst = false;
      component.pagination.currentPage = 1;
      component.previousPage();
      expect(productServiceSpy.getProductList).toHaveBeenCalledTimes(2);
    });
  });
});
