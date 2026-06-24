package com.example.workflow.mapper;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import com.example.workflow.nume.Role;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-17T08:44:37+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Autowired
    private CartMapper cartMapper;

    @Override
    public User toEntity(UserCreDTO request) {
        if ( request == null ) {
            return null;
        }

        User user = new User();

        if ( request.getRole() != null ) {
            user.setRole( Enum.valueOf( Role.class, request.getRole() ) );
        }
        user.setId( request.getId() );
        user.setFirstname( request.getFirstname() );
        user.setLastname( request.getLastname() );
        user.setUsername( request.getUsername() );
        user.setPassword( request.getPassword() );
        user.setGender( request.getGender() );
        user.setAddress( request.getAddress() );
        user.setPhone( request.getPhone() );
        user.setBirth( request.getBirth() );
        user.setAvatarUrl( request.getAvatarUrl() );
        user.setEmail( request.getEmail() );

        return user;
    }

    @Override
    public UserResDTO toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResDTO userResDTO = new UserResDTO();

        userResDTO.setCart( cartMapper.toDto( user.getCart() ) );
        userResDTO.setBirth( user.getBirth() );
        userResDTO.setPhone( user.getPhone() );
        userResDTO.setId( user.getId() );
        userResDTO.setUsername( user.getUsername() );
        userResDTO.setFirstname( user.getFirstname() );
        userResDTO.setLastname( user.getLastname() );
        userResDTO.setGender( user.getGender() );
        userResDTO.setAddress( user.getAddress() );
        if ( user.getRole() != null ) {
            userResDTO.setRole( user.getRole().name() );
        }
        userResDTO.setReputation( user.getReputation() );
        userResDTO.setDelete( user.isDelete() );
        userResDTO.setAvatarUrl( user.getAvatarUrl() );
        userResDTO.setEmail( user.getEmail() );

        return userResDTO;
    }

    @Override
    public void updateUser(User user, UserCreDTO request) {
        if ( request == null ) {
            return;
        }

        user.setAvatarUrl( request.getAvatarUrl() );
        user.setFirstname( request.getFirstname() );
        user.setLastname( request.getLastname() );
        user.setUsername( request.getUsername() );
        user.setGender( request.getGender() );
        user.setAddress( request.getAddress() );
        user.setPhone( request.getPhone() );
        user.setBirth( request.getBirth() );
        user.setEmail( request.getEmail() );
    }
}
