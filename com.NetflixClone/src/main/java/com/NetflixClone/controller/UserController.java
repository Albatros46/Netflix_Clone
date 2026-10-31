package com.NetflixClone.controller;

import com.NetflixClone.dto.request.UserRequest;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.dto.response.PageResponse;
import com.NetflixClone.dto.response.UserResponse;
import com.NetflixClone.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getUsers(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) String search){
        return ResponseEntity.ok(userService.getUsers(page,size,search));
    }
    @PostMapping("/create-user")
    public ResponseEntity<MessageResponse> createUser(@Valid @RequestBody UserRequest userRequest){
        return ResponseEntity.ok(userService.createUser(userRequest));
    }
    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest){
        return ResponseEntity.ok(userService.updateUser(id,userRequest));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(userService.deleteUser(id,authentication.getName()));
    }
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<MessageResponse> toggleUserStatus(@PathVariable Long id, Authentication authentication){
        return ResponseEntity.ok(userService.toggleUserStatus(id,authentication.getName()));
    }
    @PutMapping("/{id}/change-role")
    public ResponseEntity<MessageResponse> changeUserRole(@PathVariable Long id, @RequestBody UserRequest userRequest){
        return ResponseEntity.ok(userService.changeUserRole(id,userRequest));
    }
}
