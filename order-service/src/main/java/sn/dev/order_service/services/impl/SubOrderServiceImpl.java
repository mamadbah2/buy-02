package sn.dev.order_service.services.impl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.dev.order_service.data.entities.SubOrder;
import sn.dev.order_service.data.repository.SubOrderRepository;
import sn.dev.order_service.services.SubOrderService;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class SubOrderServiceImpl implements SubOrderService {
    private final SubOrderRepository subOrderRepository;
    @Override
    public SubOrder getById(String id) {
        return subOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SubOrder not found with id: " + id
                ));
    }
    @Override
    public List<SubOrder> getBySellerId(String sellerId) {
        return subOrderRepository.findBySellerId(sellerId);
    }
    @Override
    public SubOrder updateStatus(String id, String status) {
        SubOrder subOrder = getById(id);
        subOrder.setStatus(status);
        subOrder.setUpdatedAt(java.time.Instant.now());
        return subOrderRepository.save(subOrder);
    }
    @Override
    public Page<SubOrder> getSubOrdersBySeller(String sellerId, String status, Pageable pageable) {
        log.info("Fetching sub-orders for seller: {}, status: {}, page: {}, size: {}",
                sellerId, status, pageable.getPageNumber(), pageable.getPageSize());
        // Créer un Pageable avec tri par createdAt DESC
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        // Filtrer par status si fourni
        if (status != null && !status.isBlank()) {
            return subOrderRepository.findBySellerIdAndStatus(sellerId, status, pageableWithSort);
        }
        return subOrderRepository.findBySellerId(sellerId, pageableWithSort);
    }
}
