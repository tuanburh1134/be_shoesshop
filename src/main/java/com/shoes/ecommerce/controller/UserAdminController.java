package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.dto.UserDTO;
import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserAdminController.class);

    public UserAdminController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> list(){
        List<UserDTO> users = userRepository.findAll().stream().map(u -> new UserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getBannedUntil(), u.getBannedForever())).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDTO> setRole(@PathVariable Long id, @RequestParam String role){
        String normalized = role == null ? "" : role.trim().toLowerCase();
        if(!("user".equals(normalized) || "admin".equals(normalized))){
            return ResponseEntity.badRequest().build();
        }
        return userRepository.findById(id).map(u -> {
            logger.info("Set role userId={} username={} -> {}", u.getId(), u.getUsername(), normalized);
            u.setRole(normalized);
            userRepository.save(u);
            return ResponseEntity.ok(new UserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getBannedUntil(), u.getBannedForever()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/ban")
    public ResponseEntity<UserDTO> banUser(@PathVariable Long id, @RequestParam(required = false) Integer days, @RequestParam(required = false) Boolean forever){
        if(days != null && days <= 0){
            return ResponseEntity.badRequest().build();
        }
        return userRepository.findById(id).map(u -> {
            if(Boolean.TRUE.equals(forever)){
                u.setBannedForever(true);
                u.setBannedUntil(null);
                logger.info("Ban forever userId={} username={}", u.getId(), u.getUsername());
            } else if(days != null && days > 0){
                long until = System.currentTimeMillis() + (long)days * 24 * 60 * 60 * 1000;
                u.setBannedUntil(until);
                u.setBannedForever(false);
                logger.info("Ban userId={} username={} for {} day(s)", u.getId(), u.getUsername(), days);
            } else {
                // unban
                u.setBannedForever(false);
                u.setBannedUntil(null);
                logger.info("Unban userId={} username={}", u.getId(), u.getUsername());
            }
            userRepository.save(u);
            return ResponseEntity.ok(new UserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getBannedUntil(), u.getBannedForever()));
        }).orElse(ResponseEntity.notFound().build());
    }
}
