package com.example.demo.services.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;
import com.example.demo.repositories.RoleRepository;
import com.example.demo.dtos.auth.RegisterDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;

@Service
public class AuthService implements UserDetailsService {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository; 
    private final PasswordEncoder passwordEncoder; 

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
            user.getName(),
            user.getPassword(),
            mapRolesToAuthorities(user.getRoles())
        );
    }

    // New Registration Logic moved from Controller
    public User registerUser(RegisterDto registerDto) {
        if (userRepository.existsByName(registerDto.getUserName())) {
            throw new RuntimeException("Username is already taken!");
        }

        User user = new User();
        user.setName(registerDto.getUserName());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        // Assign default role
        Role role = roleRepository.findByName("User")
                .orElseThrow(() -> new RuntimeException("Default Role not found"));
        user.setRoles(Collections.singletonList(role));

        return userRepository.save(user);
    }

    private Collection<GrantedAuthority> mapRolesToAuthorities(List<Role> roles) {
    return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())) // Add ROLE_ prefix
            .collect(Collectors.toList());
}
}
