package com.ajayaventure.service;

import com.ajayaventure.dto.CreateUserRequest;
import com.ajayaventure.exception.FieldError.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {

    private final UserService service = new UserService();

    @Test
    void createWithoutUsernameFailsValidationBeforeAnyDbCall() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("u@example.com");
        request.setPassword("LongEnough1!");
        request.setRoleId(3L);
        request.setBusinessUnitIds(List.of(1L));
        // username intentionally missing
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, request));
    }

    @Test
    void createWithoutRoleFailsValidation() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("someone");
        request.setEmail("u@example.com");
        request.setPassword("LongEnough1!");
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, request));
    }
}