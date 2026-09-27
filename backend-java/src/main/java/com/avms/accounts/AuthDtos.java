package com.avms.accounts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class AuthDtos {

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refresh) {}

  public record LogoutRequest(@NotBlank String refresh) {}

  public record ForgotPasswordRequest(@NotBlank @Email String email) {}

  public record ResetPasswordRequest(@NotBlank String uidb64, @NotBlank String token, @NotBlank @Size(min = 8) String newPassword) {}

  public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8) String newPassword) {}

  public record UserResponse(Long id, String email, String fullName, String phone, boolean isActive,
      boolean isStaff, boolean isSuperuser, Set<String> roleCodes, Set<String> permissions) {}

  public record RoleResponse(Long id, String code, String name, String description, Set<String> permissionCodes) {}

  public record UserCreateRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password,
      String fullName, String phone, Set<String> roles) {}

  public record UserUpdateRequest(String fullName, String phone, Boolean isActive, Set<String> roles) {}
}
