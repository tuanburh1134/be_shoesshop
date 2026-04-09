package com.shoes.ecommerce.dto;

public class AuthResponse {
    private String username;
    private String message;
    private String role;
    private Boolean twoFactorEnabled;
    private String avatarUrl;

    public AuthResponse() {}
    public AuthResponse(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public AuthResponse(String username, String message, String role) {
        this.username = username;
        this.message = message;
        this.role = role;
    }

    public AuthResponse(String username, String message, String role, Boolean twoFactorEnabled) {
        this.username = username;
        this.message = message;
        this.role = role;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public AuthResponse(String username, String message, String role, Boolean twoFactorEnabled, String avatarUrl) {
        this.username = username;
        this.message = message;
        this.role = role;
        this.twoFactorEnabled = twoFactorEnabled;
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
