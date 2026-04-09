package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.dto.PayOsCreateRequest;
import com.shoes.ecommerce.entity.OrderEntity;
import com.shoes.ecommerce.service.OrderService;
import com.shoes.ecommerce.service.PayOsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final OrderService orderService;
    private final PayOsService payOsService;

    public PaymentController(OrderService orderService, PayOsService payOsService) {
        this.orderService = orderService;
        this.payOsService = payOsService;
    }

    @PostMapping("/payos/create")
    public ResponseEntity<?> createPayOsPayment(@RequestBody PayOsCreateRequest request, Principal principal) {
        if (request == null || request.getOrderId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu orderId"));
        }

        var opt = orderService.findById(request.getOrderId());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        OrderEntity order = opt.get();

        boolean admin = isAdmin();
        String caller = principal == null ? null : principal.getName();
        if (caller != null && !canAccessOrder(order, caller, admin)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        try {
            Map<String, Object> result = payOsService.createPaymentLink(order);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long orderId, Principal principal) {
        var opt = orderService.findById(orderId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        OrderEntity order = opt.get();

        boolean admin = isAdmin();
        String caller = principal == null ? null : principal.getName();
        if (caller != null && !canAccessOrder(order, caller, admin)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        Map<String, Object> result = payOsService.getOrderPaymentStatus(order);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/payos/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        boolean ok = payOsService.handleWebhook(payload == null ? Map.of() : payload);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", ok);
        return ResponseEntity.ok(out);
    }

    private boolean canAccessOrder(OrderEntity order, String caller, boolean admin) {
        if (admin) return true;
        if (order == null || order.getUser() == null) return false;
        return caller != null && caller.equals(order.getUser().getUsername());
    }

    private boolean isAdmin() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        } catch (Exception ex) {
            return false;
        }
    }
}
