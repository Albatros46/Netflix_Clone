package com.NetflixClone.service;

import com.NetflixClone.dto.request.UserRequest;
import com.NetflixClone.dto.response.EmailValidationResponse;
import com.NetflixClone.dto.response.LoginResponse;
import com.NetflixClone.dto.response.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface AuthService {
    MessageResponse signup(@Valid UserRequest userRequest);

    LoginResponse login(String email,String password);

    EmailValidationResponse validateEmail(String email);

    MessageResponse verifyEmail(String token);

    MessageResponse resendVerificationEmail(String email);

    MessageResponse forgotPassword(String email);

    MessageResponse resetPassword(String token,String newPassword);

    MessageResponse changePassword(String email,String currentPassword, String newPassword);

    LoginResponse currentUser(String email);
}
