package com.NetflixClone.serviceImpl;

import com.NetflixClone.dao.UserRepository;
import com.NetflixClone.dto.request.UserRequest;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.dto.response.PageResponse;
import com.NetflixClone.dto.response.UserResponse;
import com.NetflixClone.entity.User;
import com.NetflixClone.enums.Role;
import com.NetflixClone.exception.EmailAlreadyExistsException;
import com.NetflixClone.exception.InvalidRoleException;
import com.NetflixClone.service.EmailService;
import com.NetflixClone.service.UserService;
import com.NetflixClone.util.PaginationUtils;
import com.NetflixClone.util.ServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ServiceUtils serviceUtils;

    @Override
    public MessageResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists.");
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required!");
        }

        validateRole(userRequest.getRole());
        User user=new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setFullName(userRequest.getFullName());
        user.setUsername(usernameOrDefault(userRequest));
        user.setRole(userRequest.getRole());
        user.setActive(true);
        user.setEmailVerified(false);
        String verificationToken= UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(86400));
        userRepository.save(user);
        emailService.sendVerificationEmail(userRequest.getEmail(),verificationToken);
        return new MessageResponse("User created successfully! A verification email has been sent.");
    }

    private void validateRole(Role role) {
        if (role == null) {
            throw new InvalidRoleException("Role is required!");
        }
    }

    private String usernameOrDefault(UserRequest userRequest) {
        String username = userRequest.getUsername();
        if (username == null || username.isBlank()) {
            return userRequest.getEmail().substring(0, userRequest.getEmail().indexOf('@'));
        }
        return username;
    }

    @Override
    public MessageResponse updateUser(Long id, UserRequest userRequest) {
        User user=serviceUtils.getUserByIdOrThrow(id);
        validateRole(userRequest.getRole());

        if (user.getRole() == Role.ADMIN && userRequest.getRole() != Role.ADMIN) {
            ensureNotLastActiveAdmin(user);
        }
        if (!user.getEmail().equalsIgnoreCase(userRequest.getEmail())
                && userRepository.existsByEmail(userRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        user.setFullName(userRequest.getFullName());
        user.setRole(userRequest.getRole());
        user.setEmail(userRequest.getEmail());
        user.setUsername(usernameOrDefault(userRequest));
        if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        userRepository.save(user);
        return new MessageResponse("User updated successfully!");
    }

    @Override
    @Transactional
    public MessageResponse deleteUser(Long id, String currentUserEmail) {
        User user=serviceUtils.getUserByIdOrThrow(id);
        if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new IllegalArgumentException("You cannot delete your own account!");
        }
        ensureNotLastActiveAdmin(user);
        userRepository.delete(user);
        return new MessageResponse("User deleted successfully!");
    }

    @Override
    public MessageResponse toggleUserStatus(Long id, String currentUserEmail) {
        User user=serviceUtils.getUserByIdOrThrow(id);
        if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new IllegalArgumentException("You cannot deactivate your own account!");
        }
        ensureNotLastActiveAdmin(user);
        user.setActive(!user.isActive());
        userRepository.save(user);
        return new MessageResponse(user.isActive() ? "User activated successfully!" : "User deactivated successfully!");
    }

    @Override
    public MessageResponse changeUserRole(Long id, UserRequest userRequest) {
        User user=serviceUtils.getUserByIdOrThrow(id);
        validateRole(userRequest.getRole());
        if (user.getRole() == Role.ADMIN && userRequest.getRole() != Role.ADMIN) {
            ensureNotLastActiveAdmin(user);
        }
        user.setRole(userRequest.getRole());
        userRepository.save(user);
        return new MessageResponse("User role updated successfully!");
    }

    @Override
    public PageResponse<UserResponse> getUsers(int page, int size, String search) {
        Pageable pageable = PaginationUtils.createPageRequest(page, size, "id", "asc");
        Page<User> users = (search != null && !search.isBlank())
                ? userRepository.searchUsers(search.trim(), pageable)
                : userRepository.findAll(pageable);
        return PaginationUtils.toPageResponse(users, UserResponse::fromEntity);
    }

    private void ensureNotLastActiveAdmin(User user) {
        if (user.isActive() && user.getRole()==Role.ADMIN){
            long activeAdminCount=userRepository.countByRoleAndActive(Role.ADMIN,true);

            if (activeAdminCount<=1){
                throw new IllegalArgumentException("Cannot remove or deactivate the last active admin user.");
            }
        }
    }
}
