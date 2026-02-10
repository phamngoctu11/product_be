package com.example.workflow.entity;
import com.example.workflow.nume.Role;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

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
    @Column(unique = true, nullable = false,name="user_name")
    private String username;
    @Column(nullable = false,name="password")
    private String password;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role ;
}