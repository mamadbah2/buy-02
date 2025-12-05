package sn.dev.order_service.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sn.dev.order_service.data.entities.SubOrder;

import java.util.List;

public interface SubOrderRepository extends MongoRepository<SubOrder, String> {
     List<SubOrder> findByParentOrderId(String parentOrderId);
}