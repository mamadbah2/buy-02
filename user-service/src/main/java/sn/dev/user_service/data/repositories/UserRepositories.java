package sn.dev.user_service.data.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import sn.dev.user_service.data.entities.Role;
import sn.dev.user_service.data.entities.User;

@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepositories extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    List<User> findAllByRole(Role role);
}
