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
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User("admin", encoder.encode("admin"), "admin@example.com", "admin");
                userRepository.save(admin);
                logger.info("Default admin created: admin / admin");
            }
        };
    }
}
