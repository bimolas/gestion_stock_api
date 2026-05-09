package com.example.demo.dtos.auth;

import lombok.Data;

@Data
public class RegisterDto {
    private String userName;
    private String email;
    private String password;
    private String role;
}
