package com.shoes.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorStatusRequest {
    @NotBlank
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
