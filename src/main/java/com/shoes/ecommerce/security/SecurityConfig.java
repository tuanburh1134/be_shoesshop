package com.shoes.ecommerce.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable();
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/products/**", "GET")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/ai/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/products/**", "POST")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/api/products/**", "PUT")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/api/products/**", "DELETE")).hasRole("ADMIN")
            .requestMatchers(new AntPathRequestMatcher("/api/orders", "POST")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/payments/payos/create", "POST")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/payments/*/status", "GET")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/payments/payos/webhook", "POST")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
            .anyRequest().authenticated()
        );

        // Allow H2 console frames when using the embedded H2 web servlet
        http.headers().frameOptions().disable();

        http.httpBasic();
        return http.build();
    }
}
