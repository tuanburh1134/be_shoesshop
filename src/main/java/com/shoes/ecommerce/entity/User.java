package com.shoes.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private String email;
    @Column(columnDefinition = "TEXT")
    private String addresses; // JSON array string of addresses
    @Lob
    @Column(name = "avatar_url", columnDefinition = "LONGTEXT")
    private String avatarUrl;
    
    @Column(nullable = false)
    private String role = "user";

    @Column(name = "banned_until")
    private Long bannedUntil; // epoch millis until which account is banned

    @Column(name = "banned_forever")
    private Boolean bannedForever = false;

    @Column(name = "google_account")
    private Boolean googleAccount = false;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "pin_hash")
    private String pinHash;

    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = "user";
    }

    public User(String username, String password, String email, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role == null ? "user" : role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddresses() { return addresses; }
    public void setAddresses(String addresses) { this.addresses = addresses; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getBannedUntil() { return bannedUntil; }
    public void setBannedUntil(Long bannedUntil) { this.bannedUntil = bannedUntil; }
    public Boolean getBannedForever() { return bannedForever; }
    public void setBannedForever(Boolean bannedForever) { this.bannedForever = bannedForever; }
    public Boolean getGoogleAccount() { return googleAccount; }
    public void setGoogleAccount(Boolean googleAccount) { this.googleAccount = googleAccount; }
    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
}
