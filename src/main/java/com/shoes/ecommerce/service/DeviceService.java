package com.shoes.ecommerce.service;

import com.shoes.ecommerce.entity.Device;

public interface DeviceService {
    Device registerDevice(String deviceId, String username);
}
