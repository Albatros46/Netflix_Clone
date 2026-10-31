package com.NetflixClone.service;

import com.NetflixClone.dto.request.UserRequest;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.dto.response.PageResponse;
import com.NetflixClone.dto.response.UserResponse;

public interface UserService {
    MessageResponse createUser(UserRequest userRequest);

    MessageResponse updateUser(Long id, UserRequest userRequest);

    MessageResponse deleteUser(Long id, String currentUserEmail);

    MessageResponse toggleUserStatus(Long id, String currentUserEmail);

    MessageResponse changeUserRole(Long id, UserRequest userRequest);

    PageResponse<UserResponse> getUsers(int page, int size, String search);
}
