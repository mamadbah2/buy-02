package sn.dev.product_service.data.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@org.springframework.data.mongodb.core.mapping.Document(collection = "products")
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch-settings.json")
public class Product {
    @Id
    private String id;

    @Field(type = FieldType.Search_As_You_Type)
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Integer)
    private Integer quantity;

    @Field(type = FieldType.Keyword)
    private String userId;

    public Product(String name, String description, Double price, Integer quantity, String userId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.userId = userId;
    }

    public Product(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
