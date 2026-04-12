package com.attendance.util;

import com.attendance.entity.Role;
import com.attendance.entity.User;
import com.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Override
    public void run(String... args) throws Exception {
        // Create Admin user if not exists
        if (userRepository.findByEmail("admin@attendance.com").isEmpty()) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@attendance.com")
                    .password(encoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("Default Admin created: admin@attendance.com / admin123");
        }
    }
}
