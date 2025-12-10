package sn.dev.product_service.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sn.dev.product_service.config.FeignSupportConfig;
import sn.dev.product_service.web.dto.UserResponse;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "${user.service.url}",
        configuration = FeignSupportConfig.class
)
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserResponse getUserById(@PathVariable("id") String id);

    @GetMapping("/seller")
    List<UserResponse> getAllSeller();
}
