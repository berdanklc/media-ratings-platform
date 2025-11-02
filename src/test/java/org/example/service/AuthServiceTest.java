package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AuthService.
 * These tests verify the business logic for user registration, login, and token validation.
 */
class AuthServiceTest {

    @Mock
    private UserRepository userRepository; // Mocked repository to isolate the service logic.

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test.
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository);
    }

    /**
     * Tests successful user registration.
     */
    @Test
    void testRegister_Success() throws SQLException {
        // Arrange: A new username and a valid password.
        String username = "testuser";
        String password = "password123";

        // Mock repository behavior: username is not found, and saving is successful.
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1); // Simulate database ID generation.
            return user;
        });

        // Act: Attempt to register the user.
        User result = authService.register(username, password);

        // Assert: The user is created correctly and the save method was called.
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(username, result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Tests registration failure when the username already exists.
     */
    @Test
    void testRegister_UserAlreadyExists() throws SQLException {
        // Arrange: An existing username.
        String username = "existinguser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new User(username, "pass")));

        // Act & Assert: Expect an IllegalArgumentException and ensure save is never called.
        assertThrows(IllegalArgumentException.class, () -> authService.register(username, "newpass"));
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests registration failure with an empty username.
     */
    @Test
    void testRegister_EmptyUsername() throws SQLException {
        // Act & Assert: Expect an exception for invalid input.
        assertThrows(IllegalArgumentException.class, () -> authService.register("", "password"));
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful user login.
     */
    @Test
    void testLogin_Success() throws SQLException {
        // Arrange: A valid user and password.
        String username = "testuser";
        String password = "password123";
        User user = new User(1, username, password);

        // Mock repository behavior: user is found, and token update is successful.
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).updateToken(anyInt(), anyString());

        // Act: Attempt to log in.
        String token = authService.login(username, password);

        // Assert: A valid token is generated and the user's token is updated.
        assertNotNull(token);
        assertTrue(token.startsWith(username + "-mrpToken-"));
        verify(userRepository, times(1)).updateToken(eq(1), anyString());
    }

    /**
     * Tests login failure with an invalid username.
     */
    @Test
    void testLogin_InvalidUsername() throws SQLException {
        // Arrange: Mock repository to find no user.
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert: Expect an exception for invalid credentials.
        assertThrows(IllegalArgumentException.class, () -> authService.login("nonexistent", "password"));
    }

    /**
     * Tests login failure with an incorrect password.
     */
    @Test
    void testLogin_InvalidPassword() throws SQLException {
        // Arrange: A user with a different password.
        String username = "testuser";
        User user = new User(1, username, "correctpassword");
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act & Assert: Expect an exception for invalid credentials.
        assertThrows(IllegalArgumentException.class, () -> authService.login(username, "wrongpassword"));
    }

    /**
     * Tests successful token validation.
     */
    @Test
    void testValidateToken_Success() throws SQLException {
        // Arrange: A valid token and a corresponding user.
        String token = "testuser-mrpToken-12345";
        User user = new User(1, "testuser", "password");
        when(userRepository.findByToken(token)).thenReturn(Optional.of(user));

        // Act: Validate the token.
        Optional<User> resultOptional = authService.validateToken(token);
        User result = resultOptional.orElseThrow(() -> new SecurityException("Token validation failed"));

        // Assert: The correct user is returned.
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    /**
     * Tests token validation failure with an invalid token.
     */
    @Test
    void testValidateToken_InvalidToken() throws SQLException {
        // Arrange: Mock repository to find no user for the token.
        when(userRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // Act & Assert: Expect a SecurityException for an invalid token.
        assertThrows(SecurityException.class, () -> {
            Optional<User> resultOptional = authService.validateToken("invalid-token");
            resultOptional.orElseThrow(() -> new SecurityException("Token validation failed"));
        });
    }

    /**
     * Tests token validation failure with an empty token string.
     */
    @Test
    void testValidateToken_EmptyToken() {
        // Act & Assert: Expect a SecurityException for a missing token.
        assertThrows(SecurityException.class, () -> authService.validateToken(""));
    }
}
