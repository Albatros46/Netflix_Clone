package com.NetflixClone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String fullName;
    private String username;
    private String role;
}
