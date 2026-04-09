package com.shoes.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "devices", indexes = {@Index(columnList = "deviceId")})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String deviceId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Long createdAt;
    private Long lastSeenAt;

    public Device() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Long lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
