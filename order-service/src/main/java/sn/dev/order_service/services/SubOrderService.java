package sn.dev.order_service.services;

import sn.dev.order_service.data.entities.SubOrder;

import java.util.List;

public interface SubOrderService {
    SubOrder getById(String id);

    List<SubOrder> getBySellerId(String sellerId);

    List<SubOrder> getByUserId(String userId);

    SubOrder updateStatus(String id, String status);

    List<SubOrder> getBySellerIdAndStatus(String sellerId, String status);
}

