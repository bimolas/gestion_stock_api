package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.JwtConfig;
import com.example.demo.dtos.auth.AuthResponseDto;
import com.example.demo.dtos.auth.LoginDto;
import com.example.demo.dtos.auth.RegisterDto;
import com.example.demo.models.User;
import com.example.demo.services.auth.AuthService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Collections;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/Api/Auth/")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService; // Use the service instead of repos
    private final JwtConfig jwtConfig;

    public AuthController(
            AuthenticationManager authenticationManager,
            AuthService authService,
            JwtConfig jwtConfig) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("Register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterDto registerDto) {
        try {
            // Call the service
            authService.registerUser(registerDto);

            // Authenticate and generate token
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(registerDto.getUserName(), registerDto.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtConfig.generateToken(authentication);
            return ResponseEntity.ok(new AuthResponseDto(token));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthResponseDto(e.getMessage()));
        }
    }

    @PostMapping("Login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUserName(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = jwtConfig.generateToken(authentication);
        return ResponseEntity.ok(new AuthResponseDto(token));
    }
}
