package com.vegan.api.admin;

import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAuthorizationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    void configuredAdminCanAccess() {
        User admin = new User("admin@example.com", "hash", "admin", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        AdminAuthorizationService service = new AdminAuthorizationService(userRepository, "admin@example.com");

        assertDoesNotThrow(() -> service.requireAdmin(1L));
    }

    @Test
    void normalUserIsForbidden() {
        User user = new User("user@example.com", "hash", "user", null, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        AdminAuthorizationService service = new AdminAuthorizationService(userRepository, "admin@example.com");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requireAdmin(2L));
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void missingLoginIsUnauthorized() {
        AdminAuthorizationService service = new AdminAuthorizationService(userRepository, "admin@example.com");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requireAdmin(null));
        assertEquals(401, exception.getStatusCode().value());
    }
}
