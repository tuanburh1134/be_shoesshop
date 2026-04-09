package com.shoes.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorVerifyRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String pin;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
