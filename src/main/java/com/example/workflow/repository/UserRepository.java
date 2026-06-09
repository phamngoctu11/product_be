package com.example.workflow.repository;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.entity.User;
import com.example.workflow.nume.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsernameAndIsDeleteFalse(String username);
    @Query("SELECT new com.example.workflow.dto.UserListDTO(u.id, u.firstname, u.lastname, u.reputation, u.role,u.avatarUrl) FROM User u where u.isDelete = false")
    Page<UserListDTO> findAllCustom(Pageable pageable);

    boolean existsByEmail(String email);

    List<User> findByRoleInAndIsDeleteFalseAndEmailIsNotNull(List<Role> roles);
}
