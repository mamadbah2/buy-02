package sn.dev.order_service.data.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import sn.dev.order_service.data.entities.SubOrder;

public interface SubOrderRepository extends MongoRepository<SubOrder, String> {
    List<SubOrder> findByParentOrderId(String parentOrderId);

    List<SubOrder> findBySellerId(String sellerId);

    List<SubOrder> findByUserId(String userId);

    List<SubOrder> findBySellerIdAndStatus(String sellerId, String status);

    Page<SubOrder> findBySellerId(String sellerId, Pageable pageable);

    Page<SubOrder> findBySellerIdAndStatus(String sellerId, String status, Pageable pageable);
}