package com.shoes.ecommerce.service.impl;

import com.shoes.ecommerce.entity.Device;
import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.DeviceRepository;
import com.shoes.ecommerce.repository.UserRepository;
import com.shoes.ecommerce.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeviceServiceImpl implements DeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Device registerDevice(String deviceId, String username) {
        if(deviceId == null || deviceId.isBlank()) throw new IllegalArgumentException("deviceId is required");
        User user = null;
        if(username != null) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        Optional<Device> existing = deviceRepository.findByDeviceId(deviceId);
        if(existing.isPresent()){
            Device d = existing.get();
            d.setLastSeenAt(System.currentTimeMillis());
            if(user != null && d.getUser() == null){
                d.setUser(user);
            }
            return deviceRepository.save(d);
        }

        Device d = new Device();
        d.setDeviceId(deviceId);
        d.setUser(user);
        d.setCreatedAt(System.currentTimeMillis());
        d.setLastSeenAt(System.currentTimeMillis());
        LOGGER.info("Registering device {} for user={}", deviceId, username);
        return deviceRepository.save(d);
    }
}
