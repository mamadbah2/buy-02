package sn.dev.order_service.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sn.dev.order_service.config.FeignSupportConfig;
import sn.dev.order_service.web.dto.UserResponseDto;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "${user.service.url}",
        configuration = FeignSupportConfig.class
)
public interface UserClient {
    @GetMapping("/{id}")
    UserResponseDto getById(@PathVariable("id") String id);

    @GetMapping("/seller")
    List<UserResponseDto> getAllUsers();
}

