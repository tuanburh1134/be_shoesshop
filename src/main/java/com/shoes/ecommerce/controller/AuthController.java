package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.dto.AuthResponse;
import com.shoes.ecommerce.dto.ChangePasswordRequest;
import com.shoes.ecommerce.dto.GoogleLoginRequest;
import com.shoes.ecommerce.dto.LoginRequest;
import com.shoes.ecommerce.dto.RegisterRequest;
import com.shoes.ecommerce.dto.TwoFactorConfigRequest;
import com.shoes.ecommerce.dto.TwoFactorStatusRequest;
import com.shoes.ecommerce.dto.TwoFactorStatusResponse;
import com.shoes.ecommerce.dto.TwoFactorVerifyRequest;
import com.shoes.ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        logger.debug("Received register request for {}", req.getUsername());
        AuthResponse res = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        logger.debug("Received login request for {}", req.getUsername());
        AuthResponse res = userService.login(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest req) {
        logger.debug("Received google login request");
        AuthResponse res = userService.loginWithGoogle(req.getCredential(), req.getPin());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        logger.debug("Received change password request for {}", req.getUsername());
        AuthResponse res = userService.changePassword(req.getUsername(), req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/2fa/status")
    public ResponseEntity<TwoFactorStatusResponse> twoFactorStatus(@Valid @RequestBody TwoFactorStatusRequest req) {
        boolean enabled = userService.getTwoFactorStatus(req.getUsername());
        return ResponseEntity.ok(new TwoFactorStatusResponse(enabled));
    }

    @PostMapping("/2fa/configure")
    public ResponseEntity<AuthResponse> configureTwoFactor(@Valid @RequestBody TwoFactorConfigRequest req) {
        AuthResponse res = userService.configureTwoFactor(
                req.getUsername(),
                Boolean.TRUE.equals(req.getEnabled()),
                req.getPin(),
                req.getConfirmPin(),
                req.getCurrentPin()
        );
        return ResponseEntity.ok(res);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest req) {
        AuthResponse res = userService.verifyTwoFactorPin(req.getUsername(), req.getPin());
        return ResponseEntity.ok(res);
    }
}

