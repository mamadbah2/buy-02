package sn.dev.media_service.web.controllers.dto;

import lombok.Data;

// DTO pour récupérer les produits
@Data
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String userId;
}