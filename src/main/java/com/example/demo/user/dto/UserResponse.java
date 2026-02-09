package com.example.demo.user.dto;

import com.example.demo.user.role.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private Role role;
    private String email;
    private String telephone;
    private String nom;
    private String prenom;
}
