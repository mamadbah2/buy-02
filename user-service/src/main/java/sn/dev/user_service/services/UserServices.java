package sn.dev.user_service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.dev.user_service.data.entities.User;

import java.util.List;


public interface UserServices {
    String login(User user);
    User findByEmail(String email);
    User findById(String id);
    List<User> findAllSeller();
    Page<User> findAllUsers(Pageable pageable);
    User createUser(User user);
}
