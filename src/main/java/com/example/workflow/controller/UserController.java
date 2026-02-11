package com.example.workflow.controller;
import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;
    private final RuntimeService runtimeService;
    @PostMapping()
    public ResponseEntity<Map<String, String>> register(@RequestBody UserCreDTO dto) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", dto.getUsername());
        variables.put("password", dto.getPassword());
        variables.put("role", dto.getRole());
        runtimeService.startProcessInstanceByKey("CreateUserProcess", variables);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Quy trình tạo User đã bắt đầu!");
        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<UserResDTO>> getAll() {
        List<UserResDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserResDTO> getById(@PathVariable Long id) {
        UserResDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    @PutMapping("/{id}")
    public ResponseEntity<UserResDTO> update(@PathVariable Long id, @RequestBody UserCreDTO request) {
        UserResDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}