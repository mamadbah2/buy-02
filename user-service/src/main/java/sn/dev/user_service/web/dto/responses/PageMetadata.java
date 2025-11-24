package sn.dev.user_service.web.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageMetadata {
    private int size;
    private long totalElements;
    private int totalPages;
    private int number;
}

