package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.entity.Device;
import com.shoes.ecommerce.service.DeviceService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService){ this.deviceService = deviceService; }

    @PostMapping("/register")
    public ResponseEntity<Device> register(@RequestBody Map<String, String> body, Principal principal){
        String deviceId = body.get("deviceId");
        if(deviceId == null || deviceId.isBlank()) return ResponseEntity.badRequest().build();
        String username = principal == null ? null : principal.getName();
        Device d = deviceService.registerDevice(deviceId, username);
        return ResponseEntity.status(201).body(d);
    }
}
