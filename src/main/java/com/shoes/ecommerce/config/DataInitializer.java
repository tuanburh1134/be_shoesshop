package com.shoes.ecommerce.config;

import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    private final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            User admin = userRepository.findByUsername("admin").orElse(null);

            if (admin == null) {
                User created = new User("admin", encoder.encode("admin"), "admin@example.com", "admin");
                userRepository.save(created);
                logger.warn("Default admin created: admin / admin");
                return;
            }

            boolean changed = false;
            if (admin.getRole() == null || !"admin".equalsIgnoreCase(admin.getRole())) {
                admin.setRole("admin");
                changed = true;
            }
            if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                admin.setEmail("admin@example.com");
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
                logger.warn("Recovered admin account metadata for username=admin (role/email corrected)");
            }
        };
    }
}
