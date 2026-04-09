package com.shoes.ecommerce.dto;

public class TwoFactorStatusResponse {
    private boolean enabled;

    public TwoFactorStatusResponse() {
    }

    public TwoFactorStatusResponse(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
