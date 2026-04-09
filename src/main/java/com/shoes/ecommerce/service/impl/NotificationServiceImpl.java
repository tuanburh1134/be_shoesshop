package com.shoes.ecommerce.service.impl;

import com.shoes.ecommerce.entity.Device;
import com.shoes.ecommerce.entity.Notification;
import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.DeviceRepository;
import com.shoes.ecommerce.repository.NotificationRepository;
import com.shoes.ecommerce.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final DeviceRepository deviceRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, DeviceRepository deviceRepository) {
        this.notificationRepository = notificationRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Notification createNotification(User user, String deviceId, String message) {
        if(deviceId == null || deviceId.isBlank()){
            // if no deviceId provided, create notifications for all registered devices of the user
            if(user == null) return null;
            var devices = deviceRepository.findByUserId(user.getId());
            Notification last = null;
            for(Device d: devices){
                Notification n = new Notification();
                n.setUser(user);
                n.setDeviceId(d.getDeviceId());
                n.setMessage(message);
                n.setSeen(false);
                n.setCreatedAt(System.currentTimeMillis());
                last = notificationRepository.save(n);
                LOGGER.info("Created notification id={} for user={} device={}", last.getId(), user.getUsername(), d.getDeviceId());
            }
            return last;
        }

        Notification n = new Notification();
        n.setUser(user);
        n.setDeviceId(deviceId);
        n.setMessage(message);
        n.setSeen(false);
        n.setCreatedAt(System.currentTimeMillis());
        Notification saved = notificationRepository.save(n);
        LOGGER.info("Created notification id={} for user={} device={}", saved.getId(), user == null ? null : user.getUsername(), deviceId);
        return saved;
    }

    @Override
    public List<Notification> listByDeviceId(String deviceId) {
        return notificationRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
    }
}
