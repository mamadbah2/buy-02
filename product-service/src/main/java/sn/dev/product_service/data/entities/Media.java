package sn.dev.product_service.data.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// This is like the dto of media client
public class Media {
    private String id;
    private String imageUrl;
}
