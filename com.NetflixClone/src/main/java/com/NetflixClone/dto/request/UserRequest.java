package com.NetflixClone.dto.request;

import com.NetflixClone.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Email is required!")
    @Email(message = "Invalid email format.")
    private String email;
    // Signup/Create'te zorunlu; admin guncellemesinde bos birakilirsa sifre degismez
    @Size(min = 6, message = "Password must be at least 6 characters long!")
    private String password;
    @NotBlank(message = "Full name is required!")
    private String fullName;
    private String username;
    private Role role;
    private Boolean active;
}
