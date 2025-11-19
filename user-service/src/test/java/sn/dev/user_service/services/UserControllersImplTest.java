package sn.dev.user_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import sn.dev.user_service.data.entities.*;
import sn.dev.user_service.web.controllers.impl.UserControllersImpl;
import sn.dev.user_service.web.dto.requests.LoginRequests;
import sn.dev.user_service.web.dto.responses.LoginResponse;
import sn.dev.user_service.web.dto.responses.UserResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllersImplTest {

    @Mock
    private UserServices userServices;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private UserControllersImpl userControllers;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoginSuccess() {
        System.out.println("TEST LOGIN");
        // given
        LoginRequests loginRequests = new LoginRequests("john@example.com", "password123");
        sn.dev.user_service.data.entities.User mockUser = new sn.dev.user_service.data.entities.User();
        mockUser.setId("123");
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("password123");

        when(userServices.findByEmail("john@example.com")).thenReturn(mockUser);
        when(userServices.login(any(sn.dev.user_service.data.entities.User.class)))
                .thenReturn("fake-jwt-token");

        UserDetails mockUserDetails = User.withUsername("john@example.com")
                .password("password123")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(mockUserDetails);

        // when
        ResponseEntity<LoginResponse> response = userControllers.login(loginRequests);

        // then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("fake-jwt-token", response.getBody().getToken());

    }

    @Test
    void testGetUserById() {
        System.out.println("TEST GET ONE USER CONTROLLER");
        // given
        sn.dev.user_service.data.entities.User mockUser = new sn.dev.user_service.data.entities.User();
        mockUser.setId("123");
        mockUser.setEmail("alice@example.com");
        mockUser.setPassword("pwd");
        mockUser.setRole(Role.CLIENT);

        when(userServices.findById("123")).thenReturn(mockUser);

        // when
        ResponseEntity<UserResponse> response = userControllers.getUser("123");

        // then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("alice@example.com", response.getBody().getEmail());
        assertEquals("123", response.getBody().getId());
        assertTrue(response.getBody().hasLinks()); // HATEOAS link exists
    }

    @Test
    void testGetUsers() {
        System.out.println("TEST GET ALL USERS CONTROLLERS");
        // given
        sn.dev.user_service.data.entities.User user1 = new sn.dev.user_service.data.entities.User();
        user1.setId("1");
        user1.setEmail("bob@example.com");
        user1.setRole(Role.SELLER);

        sn.dev.user_service.data.entities.User user2 = new sn.dev.user_service.data.entities.User();
        user2.setId("2");
        user2.setEmail("jane@example.com");
        user2.setRole(Role.CLIENT);

        List<sn.dev.user_service.data.entities.User> users = List.of(user1, user2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<sn.dev.user_service.data.entities.User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userServices.findAllUsers(any(Pageable.class))).thenReturn(userPage);

        // when - avec les paramètres de pagination
        ResponseEntity<CollectionModel<UserResponse>> response = userControllers.getUsers(0, 10, "id", "asc");

        // then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertTrue(response.getBody().hasLinks()); // self link exists
    }
}
