package com.example.demo.config;

import com.example.demo.models.Role;
import com.example.demo.repositories.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds required reference data on startup.
 * Ensures the "User" role exists so registration never fails with
 * "Default Role not found".
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (roleRepository.findByName("User").isEmpty()) {
            roleRepository.save(new Role(null, "User"));
            System.out.println("[DataInitializer] 'User' role created.");
        }
    }
}
