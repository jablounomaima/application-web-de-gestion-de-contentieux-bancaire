package com.example.demo.user.dto;

import com.example.demo.user.role.Role;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private Role role;
    private String email;
    private String telephone;
    private String nom;
    private String prenom;
}
