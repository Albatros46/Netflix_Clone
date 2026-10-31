package com.NetflixClone.dto.response;

import com.NetflixClone.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String username;
    private String role;
    private boolean active;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResponse fromEntity(User user){
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getUsername(),
                user.getRole().name(),
                user.isActive(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
