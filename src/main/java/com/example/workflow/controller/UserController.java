package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.nume.Role;
import com.example.workflow.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody UserCreDTO dto) {
        userService.startUserRegistrationProcess(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Tạo người dùng thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserListDTO>>> getAll(
            @Min(value = 0, message = "Page must be zero or positive") @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "Size must be positive")
            @Max(value = 100, message = "Size must be at most 100") @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<Role> roles
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserListDTO> users = userService.getAllUsers(roles, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResDTO>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResDTO>> updateMe(
            @Valid @RequestBody UserProfileUpdateDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateMyProfile(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResDTO>> getById(
            @PathVariable String id
    ) {
        UserResDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResDTO>> update(
            @PathVariable String id,
            @Valid @RequestBody UserCreDTO request
    ) {
        UserResDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
