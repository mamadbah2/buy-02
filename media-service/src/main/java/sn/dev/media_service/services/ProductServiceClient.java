package sn.dev.media_service.services;

import sn.dev.media_service.web.controllers.dto.ProductDto;

import java.util.List;

public interface ProductServiceClient {
    List<ProductDto> fetchProductsFromProductService();
    String inferCategoryFromProductName(String name);
    String getRandomCategory();
}
