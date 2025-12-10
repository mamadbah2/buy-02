package sn.dev.user_service.web.controllers.impl;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.services.UserServices;
import sn.dev.user_service.web.controllers.UserControllers;
import sn.dev.user_service.web.dto.requests.LoginRequests;
import sn.dev.user_service.web.dto.requests.UserRequests;
import sn.dev.user_service.web.dto.responses.LoginResponse;
import sn.dev.user_service.web.dto.responses.UserResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class UserControllersImpl implements UserControllers {
    private final UserServices userServices;
    private final UserDetailsService userDetailsService;

    @Override
    @PostMapping("api/users/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequests loginRequests) {
        String userID = userServices.findByEmail(loginRequests.getEmail()).getId();
        User credentialsUser = loginRequests.toEntity();
        credentialsUser.setId(userID);
        String token = userServices.login(credentialsUser);
        UserDetails userDetails = userDetailsService.loadUserByUsername(credentialsUser.getEmail());
        LoginResponse loginResponse = new LoginResponse(userDetails, token);
        return ResponseEntity.ok(loginResponse);
    }

    @Override
    @GetMapping("api/users/{userID}/custom")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userID) {
        User user = userServices.findById(userID);
        UserResponse userResponse = new UserResponse(user);
        userResponse.add(linkTo(methodOn(this.getClass()).getUser(userID)).withSelfRel());
        return ResponseEntity.ok(userResponse);
    }

    @Override
    @GetMapping("api/users/custom")
    public ResponseEntity<CollectionModel<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        // Créer l'objet Sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);

        // Créer le Pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Récupérer les utilisateurs paginés
        Page<User> userPage = userServices.findAllUsers(pageable);

        // Convertir en UserResponse avec liens HATEOAS
        List<UserResponse> userDtos = userPage.getContent().stream()
                .map(user -> {
                    UserResponse dto = new UserResponse(user);
                    dto.add(linkTo(methodOn(UserControllersImpl.class)
                            .getUser(user.getId()))
                            .withSelfRel());
                    return dto;
                })
                .collect(Collectors.toList());

        // Créer les liens de pagination
        List<Link> links = new ArrayList<>();

        // Lien self
        links.add(linkTo(methodOn(UserControllersImpl.class)
                .getUsers(page, size, sortBy, sortDirection))
                .withSelfRel());

        // Lien first
        if (userPage.getTotalPages() > 0) {
            links.add(linkTo(methodOn(UserControllersImpl.class)
                    .getUsers(0, size, sortBy, sortDirection))
                    .withRel("first"));
        }

        // Lien previous
        if (userPage.hasPrevious()) {
            links.add(linkTo(methodOn(UserControllersImpl.class)
                    .getUsers(page - 1, size, sortBy, sortDirection))
                    .withRel("prev"));
        }

        // Lien next
        if (userPage.hasNext()) {
            links.add(linkTo(methodOn(UserControllersImpl.class)
                    .getUsers(page + 1, size, sortBy, sortDirection))
                    .withRel("next"));
        }

        // Lien last
        if (userPage.getTotalPages() > 0) {
            links.add(linkTo(methodOn(UserControllersImpl.class)
                    .getUsers(userPage.getTotalPages() - 1, size, sortBy, sortDirection))
                    .withRel("last"));
        }

        // Créer le CollectionModel avec les liens
        CollectionModel<UserResponse> collectionModel = CollectionModel.of(userDtos, links);

        return ResponseEntity.ok(collectionModel);
    }

    //    Cette méthode retourne une liste simple sans HATEOAS pour des cas d'utilisation spécifiques exemple les autres service
    @Override
    @GetMapping("api/users/seller")
    public ResponseEntity<List<UserResponse>> getAllSeller() {
        List<User> users = userServices.findAllSeller();
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @Override
    @PostMapping("api/users")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequests userRequests) {
        // Convertir le DTO en entité
        User user = userRequests.toEntity();

        // Créer l'utilisateur via le service
        User savedUser = userServices.createUser(user);

        // Créer la réponse
        UserResponse userResponse = new UserResponse(savedUser);

        // Ajouter le lien HATEOAS self
        userResponse.add(linkTo(methodOn(this.getClass()).getUser(savedUser.getId())).withSelfRel());

        // Construire l'URI de la ressource créée
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.getId())
                .toUri();

        // Retourner 201 Created avec l'URI et le corps
        return ResponseEntity.created(location).body(userResponse);
    }


}
