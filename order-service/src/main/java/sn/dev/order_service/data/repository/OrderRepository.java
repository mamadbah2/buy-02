package sn.dev.order_service.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import sn.dev.order_service.data.entities.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    void deleteByUserId(String userId);
    Optional<Order> findByUserIdAndStatus(String userId, String status);

}
