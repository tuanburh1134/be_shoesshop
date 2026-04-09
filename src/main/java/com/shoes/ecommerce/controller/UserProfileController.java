package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.DeviceRepository;
import com.shoes.ecommerce.repository.NotificationRepository;
import com.shoes.ecommerce.repository.OrderRepository;
import com.shoes.ecommerce.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/me")
public class UserProfileController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DeviceRepository deviceRepository;
    private final NotificationRepository notificationRepository;

    public UserProfileController(UserRepository userRepository,
                                 OrderRepository orderRepository,
                                 DeviceRepository deviceRepository,
                                 NotificationRepository notificationRepository){
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.deviceRepository = deviceRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<User> me(Principal principal){
        if(principal == null) return ResponseEntity.status(401).build();
        return userRepository.findByUsername(principal.getName()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<User> update(Principal principal, @RequestBody User payload){
        if(principal == null) return ResponseEntity.status(401).build();
        var opt = userRepository.findByUsername(principal.getName());
        if(opt.isEmpty()) return ResponseEntity.notFound().build();
        var u = opt.get();
        // allow updating addresses and avatarUrl and phone/email
        u.setAddresses(payload.getAddresses());
        if(payload.getAvatarUrl()!=null) u.setAvatarUrl(payload.getAvatarUrl());
        if(payload.getEmail()!=null) u.setEmail(payload.getEmail());
        userRepository.save(u);
        return ResponseEntity.ok(u);
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> deleteMyAccount(Principal principal){
        if(principal == null) return ResponseEntity.status(401).build();
        var opt = userRepository.findByUsername(principal.getName());
        if(opt.isEmpty()) return ResponseEntity.notFound().build();

        var u = opt.get();
        Long userId = u.getId();

        var orders = orderRepository.findByUserId(userId);
        orders.forEach(o -> o.setUser(null));
        orderRepository.saveAll(orders);

        var devices = deviceRepository.findByUserId(userId);
        deviceRepository.deleteAll(devices);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);

        userRepository.delete(u);
        return ResponseEntity.noContent().build();
    }
}
