package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.entity.Device;
import com.shoes.ecommerce.entity.Notification;
import com.shoes.ecommerce.repository.DeviceRepository;
import com.shoes.ecommerce.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceRepository deviceRepository;

    public NotificationController(NotificationService notificationService, DeviceRepository deviceRepository){
        this.notificationService = notificationService;
        this.deviceRepository = deviceRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> listByDevice(@RequestParam String deviceId, Principal principal){
        if(deviceId == null || deviceId.isBlank()) return ResponseEntity.badRequest().build();

        if(principal == null) return ResponseEntity.status(401).build();

        // If caller is not admin, ensure the device belongs to the caller
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            Optional<Device> dOpt = deviceRepository.findByDeviceId(deviceId);
            if(dOpt.isEmpty()){
                return ResponseEntity.status(403).build();
            }
            var d = dOpt.get();
            var user = d.getUser();
            if(user == null || !user.getUsername().equals(principal.getName())){
                return ResponseEntity.status(403).build();
            }
        }

        List<Notification> list = notificationService.listByDeviceId(deviceId);
        return ResponseEntity.ok(list);
    }
}
