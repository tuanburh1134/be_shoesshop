package com.shoes.ecommerce.service;

import com.shoes.ecommerce.dto.AuthResponse;
import com.shoes.ecommerce.dto.LoginRequest;
import com.shoes.ecommerce.dto.RegisterRequest;
import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.exception.AuthenticationFailedException;
import com.shoes.ecommerce.exception.ResourceAlreadyExistsException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.shoes.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final String googleClientId;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository,
                       @Value("${google.oauth.client-id:}") String googleClientId) {
        this.userRepository = userRepository;
        this.googleClientId = googleClientId;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        logger.info("Register attempt for username={}", req.getUsername());
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ResourceAlreadyExistsException("username.already.exists");
        }
        String hashed = passwordEncoder.encode(req.getPassword());
        User u = new User(req.getUsername(), hashed, req.getEmail(), "user");
        userRepository.save(u);
        logger.info("User registered: {}", req.getUsername());
        return buildAuthResponse(u, "Registration successful");
    }

    public AuthResponse login(LoginRequest req) {
        logger.info("Login attempt for username={}", req.getUsername());
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("invalid.credentials"));
        if (isBanned(user)) {
            throw new AuthenticationFailedException("account.locked");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("invalid.credentials");
        }
        verifyTwoFactorForUser(user, req.getPin());
        logger.info("User authenticated: {}", user.getUsername());
        return buildAuthResponse(user, "Login successful");
    }

    @Transactional
    public AuthResponse changePassword(String username, String oldPassword, String newPassword) {
        logger.info("Change password attempt for username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("invalid.credentials"));

        if (Boolean.TRUE.equals(user.getGoogleAccount())) {
            throw new AuthenticationFailedException("password.change.not.supported.google");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AuthenticationFailedException("old.password.invalid");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new AuthenticationFailedException("password.same.as.old");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password changed successfully for username={}", username);
        return buildAuthResponse(user, "Password changed successfully");
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idTokenString, String pin) {
        logger.info("Google login attempt");

        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new AuthenticationFailedException("google.email.missing");
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new AuthenticationFailedException("google.email.not.verified");
        }

        User user = userRepository.findFirstByEmail(email)
                .orElseGet(() -> createUserFromGooglePayload(payload));

        String googlePicture = getGooglePicture(payload);
        if (googlePicture != null && !googlePicture.isBlank()
            && (user.getAvatarUrl() == null || !googlePicture.equals(user.getAvatarUrl()))) {
            user.setAvatarUrl(googlePicture);
            userRepository.save(user);
        }

        if (isBanned(user)) {
            throw new AuthenticationFailedException("account.locked");
        }
        verifyTwoFactorForUser(user, pin);

        logger.info("Google user authenticated: {}", user.getUsername());
        return buildAuthResponse(user, "Google login successful");
    }

    public boolean getTwoFactorStatus(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("invalid.credentials"));
        return Boolean.TRUE.equals(user.getTwoFactorEnabled());
    }

    @Transactional
    public AuthResponse configureTwoFactor(String username, boolean enabled, String pin, String confirmPin, String currentPin) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("invalid.credentials"));

        if (enabled) {
            requireValidPin(pin);
            if (confirmPin == null || !pin.equals(confirmPin)) {
                throw new AuthenticationFailedException("pin.confirm.mismatch");
            }
            user.setPinHash(passwordEncoder.encode(pin));
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
            return buildAuthResponse(user, "Two-factor enabled");
        }

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            requireValidPin(currentPin);
            if (user.getPinHash() == null || !passwordEncoder.matches(currentPin, user.getPinHash())) {
                throw new AuthenticationFailedException("pin.invalid");
            }
        }

        user.setTwoFactorEnabled(false);
        user.setPinHash(null);
        userRepository.save(user);
        return buildAuthResponse(user, "Two-factor disabled");
    }

    public AuthResponse verifyTwoFactorPin(String username, String pin) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("invalid.credentials"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            return buildAuthResponse(user, "Two-factor is disabled");
        }

        requireValidPin(pin);
        if (user.getPinHash() == null || !passwordEncoder.matches(pin, user.getPinHash())) {
            throw new AuthenticationFailedException("pin.invalid");
        }

        return buildAuthResponse(user, "Pin verified");
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new AuthenticationFailedException("google.login.not.configured");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new AuthenticationFailedException("google.token.invalid");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException ex) {
            logger.warn("Google token verification failed", ex);
            throw new AuthenticationFailedException("google.token.invalid");
        }
    }

    private User createUserFromGooglePayload(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String username = buildUniqueUsername(email);
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = new User(username, randomPassword, email, "user");
        user.setAvatarUrl(getGooglePicture(payload));
        user.setGoogleAccount(true);
        userRepository.save(user);
        logger.info("Created local user from Google account: {}", username);
        return user;
    }

    private String getGooglePicture(GoogleIdToken.Payload payload) {
        Object picture = payload.get("picture");
        return picture == null ? null : String.valueOf(picture);
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        return new AuthResponse(
                user.getUsername(),
                message,
                user.getRole(),
                Boolean.TRUE.equals(user.getTwoFactorEnabled()),
                user.getAvatarUrl()
        );
    }

    private void verifyTwoFactorForUser(User user, String pin) {
        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            return;
        }
        requireValidPin(pin);
        if (user.getPinHash() == null || !passwordEncoder.matches(pin, user.getPinHash())) {
            throw new AuthenticationFailedException("pin.invalid");
        }
    }

    private void requireValidPin(String pin) {
        if (pin == null || pin.isBlank()) {
            throw new AuthenticationFailedException("pin.required");
        }
        if (!pin.matches("\\d{6}")) {
            throw new AuthenticationFailedException("pin.invalid.format");
        }
    }

    private String buildUniqueUsername(String email) {
        String base = email == null ? "googleuser" : email.split("@")[0];
        base = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._]", "");

        if (base.isBlank()) {
            base = "googleuser";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean isBanned(User user){
        if(user == null) return false;
        if(Boolean.TRUE.equals(user.getBannedForever())) return true;
        Long until = user.getBannedUntil();
        return until != null && until > System.currentTimeMillis();
    }
}
