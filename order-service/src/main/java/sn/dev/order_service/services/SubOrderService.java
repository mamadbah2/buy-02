package sn.dev.order_service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.dev.order_service.data.entities.SubOrder;

import java.util.List;

public interface SubOrderService {
    SubOrder getById(String id);

    List<SubOrder> getBySellerId(String sellerId);

    SubOrder updateStatus(String id, String status);

    Page<SubOrder> getSubOrdersBySeller(String sellerId, String status, Pageable pageable);
}
