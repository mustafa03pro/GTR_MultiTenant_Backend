package com.example.multi_tanent.spersusers.dto;

import java.util.Set;

import com.example.multi_tanent.master.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 4, message = "Password must be at least 4 characters long")
    private String password;

    @NotEmpty(message = "At least one role is required")
    private Set<Role> roles;
    private Boolean isActive;
    private Boolean isLocked;

    private Long storeId; // Optional: to associate user with a specific store
    private Long locationId; // Optional: to associate user with a specific location
}
