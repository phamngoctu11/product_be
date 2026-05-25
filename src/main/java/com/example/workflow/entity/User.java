package com.example.workflow.entity;

import com.example.workflow.nume.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(nullable = false,name = "firstname")
    private String firstname;

    @Column(nullable = false,name = "lastname")
    private String lastname;

    @Column(unique = true, nullable = false,name="username")
    private String username;

    @Column(nullable = false,name="password")
    private String password;

    @Column(nullable=false,name="gender")
    private String gender;

    @Column(nullable = true,name="address")
    private String address;

    @Column(nullable = false,name="phone",unique = true)
    private String phone;

    @Column(nullable=true,name="birth")
    private LocalDate birth;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role; // Đã nhận diện 4 role mới từ Enum

    @Column(name="reputation")
    private int reputation;

    @Column(name="isdelete")
    private boolean isDelete;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name="email")
    private String email;

    @Column(name = "is_active")
    private Boolean isActive = false;
}