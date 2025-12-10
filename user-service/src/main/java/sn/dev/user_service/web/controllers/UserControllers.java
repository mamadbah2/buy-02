package sn.dev.user_service.web.controllers;

import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import sn.dev.user_service.web.dto.requests.LoginRequests;
import sn.dev.user_service.web.dto.requests.UserRequests;
import sn.dev.user_service.web.dto.responses.LoginResponse;
import sn.dev.user_service.web.dto.responses.UserResponse;

import java.util.List;


public interface UserControllers {
    ResponseEntity<LoginResponse> login(LoginRequests loginRequests);
    ResponseEntity<UserResponse> getUser(String userID);
    ResponseEntity<CollectionModel<UserResponse>> getUsers(int page, int size, String sortBy, String sortDirection);
    ResponseEntity<List<UserResponse>> getAllSeller();
    ResponseEntity<UserResponse> register(UserRequests userRequests);
}
