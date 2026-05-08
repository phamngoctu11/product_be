package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RuntimeService runtimeService;

    // CHUYỂN LOGIC TỪ CONTROLLER XUỐNG ĐÂY
    public void startUserRegistrationProcess(UserCreDTO dto) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", dto.getUsername());
        variables.put("password", dto.getPassword());
        variables.put("firstname", dto.getFirstname());
        variables.put("lastname", dto.getLastname());
        variables.put("gender", dto.getGender());
        variables.put("address", dto.getAddress());
        variables.put("role", dto.getRole());
        variables.put("birth", dto.getBirth());
        variables.put("phone", dto.getPhone());
        variables.put("email",dto.getEmail());
        runtimeService.startProcessInstanceByKey("CreateUserProcess", variables);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'list-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserListDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAllCustom(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#id")
    public UserResDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id")
    })
    public UserResDTO updateUser(Long id, UserCreDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found to update"));
        userMapper.updateUser(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id")
    })
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found to update"));
        user.setDelete(true);
        userRepository.save(user);
    }
}