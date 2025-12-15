package sn.dev.user_service.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.dev.user_service.data.entities.Role;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.data.repositories.UserRepositories;
import sn.dev.user_service.exceptions.UserAlreadyExistsException;
import sn.dev.user_service.services.impl.UserServicesImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // active mockito
public class UserServicesTest {
    @Mock
    private UserRepositories userRepositories;

    @Mock
    private JWTServices jwtServices;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServicesImpl userServicesImpl;

    @Test
    void testLogin_SuccessfulAuthentication() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");

        String expectedToken = "mocked_jwt_token";

        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);

        Mockito.when(jwtServices.generateToken(Mockito.any(Authentication.class), Mockito.isNull())).thenReturn(expectedToken);

        String result = userServicesImpl.login(user);

        Assertions.assertEquals(expectedToken, result);
        Mockito.verify(authenticationManager, Mockito.times(1)).authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class));
        Mockito.verify(jwtServices, Mockito.times(1)).generateToken(Mockito.any(Authentication.class), Mockito.isNull());
    }


    @Test
    void login_InvalidCredentials_ThrowsAuthenticationException() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");

        // Mocking behavior for a failed authentication
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Invalid username or password"));

        // When / Then
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> userServicesImpl.login(user));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtServices, never()).generateToken(any(), any()); // Should not generate token
    }

    @Test
    void login_AuthenticationNotAuthenticated_ReturnsFail() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        Authentication authentication = mock(Authentication.class);

        // Mocking behavior where authentication succeeds but is not authenticated
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        String result = userServicesImpl.login(user);

        // Then
        assertEquals("fail", result);
        verify(jwtServices, never()).generateToken(any(), any());
    }

    @Test
    void findByEmail_UserExists_ReturnsUser() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        when(userRepositories.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        User foundUser = userServicesImpl.findByEmail("test@example.com");

        // Then
        assertNotNull(foundUser);
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void findByEmail_UserNotFound_ThrowsException() {
        // Given
        when(userRepositories.findByEmail("non-existent@example.com")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> userServicesImpl.findByEmail("non-existent@example.com"));
    }

    @Test
    void findById_UserExists_ReturnsUser() {
        // Given
        User user = new User();
        user.setId("user-id-123");
        when(userRepositories.findById("user-id-123")).thenReturn(Optional.of(user));

        // When
        User foundUser = userServicesImpl.findById("user-id-123");

        // Then
        assertNotNull(foundUser);
        assertEquals("user-id-123", foundUser.getId());
    }

    @Test
    void findById_UserNotFound_ThrowsException() {
        // Given
        when(userRepositories.findById("non-existent-id")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> userServicesImpl.findById("non-existent-id"));
    }

    @Test
    void findAllUsers_ReturnsAllUsers() {
        User userA = new User();
        userA.setEmail("test@example.com");
        userA.setPassword("password");

        User userB = new User();
        userB.setEmail("test1@example.com");
        userB.setPassword("password");

        // Given
        List<User> userList = Arrays.asList(userA, userB);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(userList, pageable, userList.size());
        when(userRepositories.findAll(pageable)).thenReturn(userPage);

        // When
        Page<User> foundUsers = userServicesImpl.findAllUsers(pageable);

        // Then
        assertEquals(2, foundUsers.getTotalElements());
        assertEquals("test@example.com", foundUsers.getContent().get(0).getEmail());
        verify(userRepositories, times(1)).findAll(pageable);
    }

    @Test
    void findAllUsers_EmptyPage_ReturnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = Page.empty(pageable);
        when(userRepositories.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<User> foundUsers = userServicesImpl.findAllUsers(pageable);

        // Then
        assertEquals(0, foundUsers.getTotalElements());
        assertTrue(foundUsers.getContent().isEmpty());
    }

    @Test
    void findAllSeller_ReturnsSellers() {
        // Given
        User seller1 = new User();
        seller1.setEmail("seller1@example.com");
        seller1.setRole(Role.SELLER);

        User seller2 = new User();
        seller2.setEmail("seller2@example.com");
        seller2.setRole(Role.SELLER);

        List<User> sellers = Arrays.asList(seller1, seller2);
        when(userRepositories.findAllByRole(Role.SELLER)).thenReturn(sellers);

        // When
        List<User> foundSellers = userServicesImpl.findAllSeller();

        // Then
        assertEquals(2, foundSellers.size());
        assertEquals(Role.SELLER, foundSellers.get(0).getRole());
        assertEquals(Role.SELLER, foundSellers.get(1).getRole());
        verify(userRepositories, times(1)).findAllByRole(Role.SELLER);
    }

    @Test
    void findAllSeller_NoSellers_ReturnsEmptyList() {
        // Given
        when(userRepositories.findAllByRole(Role.SELLER)).thenReturn(List.of());

        // When
        List<User> foundSellers = userServicesImpl.findAllSeller();

        // Then
        assertTrue(foundSellers.isEmpty());
    }

    @Test
    void createUser_Success_ReturnsCreatedUser() {
        // Given
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("password123");
        newUser.setRole(Role.CLIENT);

        User savedUser = new User();
        savedUser.setId("generated-id");
        savedUser.setEmail("newuser@example.com");
        savedUser.setPassword("encoded_password");
        savedUser.setRole(Role.CLIENT);

        when(userRepositories.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepositories.save(any(User.class))).thenReturn(savedUser);

        // When
        User createdUser = userServicesImpl.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        assertEquals("newuser@example.com", createdUser.getEmail());
        assertEquals("encoded_password", createdUser.getPassword());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepositories, times(1)).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsUserAlreadyExistsException() {
        // Given
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword("password");

        User newUser = new User();
        newUser.setEmail("existing@example.com");
        newUser.setPassword("password123");

        when(userRepositories.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        // When / Then
        assertThrows(UserAlreadyExistsException.class, () -> userServicesImpl.createUser(newUser));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepositories, never()).save(any(User.class));
    }

    @Test
    void createUser_PasswordIsEncoded() {
        // Given
        User newUser = new User();
        newUser.setEmail("test@example.com");
        newUser.setPassword("plainPassword");

        when(userRepositories.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepositories.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User createdUser = userServicesImpl.createUser(newUser);

        // Then
        assertEquals("encodedPassword", createdUser.getPassword());
        verify(passwordEncoder).encode("plainPassword");
    }

    @Test
    void testLogin_SuccessfulAuthentication_WithUserId() {
        // Given
        User user = new User();
        user.setId("user-id-123");
        user.setEmail("test@example.com");
        user.setPassword("password");

        String expectedToken = "mocked_jwt_token_with_id";

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(jwtServices.generateToken(any(Authentication.class), eq("user-id-123"))).thenReturn(expectedToken);

        // When
        String result = userServicesImpl.login(user);

        // Then
        assertEquals(expectedToken, result);
        verify(jwtServices, times(1)).generateToken(any(Authentication.class), eq("user-id-123"));
    }

    @Test
    void findAllUsers_WithPagination_ReturnsCorrectPage() {
        // Given
        User user1 = new User();
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setEmail("user2@example.com");

        List<User> userList = Arrays.asList(user1, user2);
        Pageable pageable = PageRequest.of(1, 5); // Second page, 5 elements per page
        Page<User> userPage = new PageImpl<>(userList, pageable, 12); // 12 total elements
        when(userRepositories.findAll(pageable)).thenReturn(userPage);

        // When
        Page<User> foundUsers = userServicesImpl.findAllUsers(pageable);

        // Then
        assertEquals(12, foundUsers.getTotalElements());
        assertEquals(3, foundUsers.getTotalPages());
        assertEquals(1, foundUsers.getNumber()); // Current page
        assertEquals(2, foundUsers.getContent().size());
    }

    @Test
    void createUser_WithClientRole_ReturnsUserWithClientRole() {
        // Given
        User newUser = new User();
        newUser.setEmail("client@example.com");
        newUser.setPassword("password123");
        newUser.setRole(Role.CLIENT);

        when(userRepositories.findByEmail("client@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepositories.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User createdUser = userServicesImpl.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        assertEquals(Role.CLIENT, createdUser.getRole());
        assertEquals("client@example.com", createdUser.getEmail());
    }

    @Test
    void createUser_WithSellerRole_ReturnsUserWithSellerRole() {
        // Given
        User newUser = new User();
        newUser.setEmail("seller@example.com");
        newUser.setPassword("password123");
        newUser.setRole(Role.SELLER);

        when(userRepositories.findByEmail("seller@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepositories.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User createdUser = userServicesImpl.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        assertEquals(Role.SELLER, createdUser.getRole());
        assertEquals("seller@example.com", createdUser.getEmail());
    }

    @Test
    void findById_VerifyRepositoryCalledWithCorrectId() {
        // Given
        String userId = "specific-user-id";
        User user = new User();
        user.setId(userId);
        user.setEmail("specific@example.com");
        when(userRepositories.findById(userId)).thenReturn(Optional.of(user));

        // When
        User foundUser = userServicesImpl.findById(userId);

        // Then
        verify(userRepositories, times(1)).findById(userId);
        assertEquals("specific@example.com", foundUser.getEmail());
    }

    @Test
    void findByEmail_VerifyRepositoryCalledWithCorrectEmail() {
        // Given
        String email = "specific@example.com";
        User user = new User();
        user.setEmail(email);
        when(userRepositories.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        User foundUser = userServicesImpl.findByEmail(email);

        // Then
        verify(userRepositories, times(1)).findByEmail(email);
        assertEquals(email, foundUser.getEmail());
    }

}




