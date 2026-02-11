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
    date = "2026-02-11T09:57:03+0700",
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
        user.setUsername( request.getUsername() );
        user.setPassword( request.getPassword() );

        return user;
    }

    @Override
    public UserResDTO toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResDTO userResDTO = new UserResDTO();

        userResDTO.setCart( cartMapper.toDTO( user.getCart() ) );
        userResDTO.setId( user.getId() );
        userResDTO.setUsername( user.getUsername() );
        if ( user.getRole() != null ) {
            userResDTO.setRole( user.getRole().name() );
        }

        return userResDTO;
    }

    @Override
    public void updateUser(User user, UserCreDTO request) {
        if ( request == null ) {
            return;
        }

        if ( request.getRole() != null ) {
            user.setRole( Enum.valueOf( Role.class, request.getRole() ) );
        }
        else {
            user.setRole( null );
        }
        user.setUsername( request.getUsername() );
    }
}
