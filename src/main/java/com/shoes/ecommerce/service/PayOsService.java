package com.shoes.ecommerce.service;

import com.shoes.ecommerce.entity.OrderEntity;
import com.shoes.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayOsService {
    private static final long EXPIRE_MS = 10L * 60L * 1000L;

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payos.base-url:https://api-merchant.payos.vn}")
    private String payosBaseUrl;

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payos.return-url:http://localhost:8081/cart.html}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:8081/cart.html}")
    private String cancelUrl;

    public PayOsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> createPaymentLink(OrderEntity order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }
        if (isBlank(clientId) || isBlank(apiKey) || isBlank(checksumKey)) {
            throw new IllegalStateException("Thiếu cấu hình PayOS (client/api/checksum key)");
        }

        refreshExpiredIfNeeded(order);
        if ("paid".equalsIgnoreCase(order.getPaymentStatus())) {
            return buildStatusResponse(order, null, null);
        }

        long orderCode = (order.getPayosOrderCode() != null) ? order.getPayosOrderCode() : generateOrderCode(order.getId());
        int amount = normalizeAmount(order.getTotal());
        long expiresAtMs = System.currentTimeMillis() + EXPIRE_MS;
        long expiredAtSeconds = expiresAtMs / 1000L;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", orderCode);
        payload.put("amount", amount);
        payload.put("description", buildDescription(order.getId()));
        payload.put("returnUrl", returnUrl);
        payload.put("cancelUrl", cancelUrl);
        payload.put("expiredAt", expiredAtSeconds);
        payload.put("items", buildItems(order));

        String signatureData = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + buildDescription(order.getId())
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        payload.put("signature", hmacSha256(signatureData, checksumKey));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                payosBaseUrl + "/v2/payment-requests",
                request,
                Map.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("PayOS không trả dữ liệu");
        }

        Object dataObj = response.getBody().get("data");
        Map<String, Object> data = (dataObj instanceof Map<?, ?> m)
                ? (Map<String, Object>) m
                : Collections.emptyMap();

        String checkoutUrl = asString(data.get("checkoutUrl"));
        String qrCode = asString(data.get("qrCode"));
        String paymentLinkId = asString(data.get("paymentLinkId"));

        order.setPayosOrderCode(orderCode);
        order.setPayosPaymentLinkId(paymentLinkId);
        order.setPaymentStatus("pending");
        order.setPaymentExpiresAt(expiresAtMs);
        orderRepository.save(order);

        return buildStatusResponse(order, checkoutUrl, qrCode);
    }

    public Map<String, Object> getOrderPaymentStatus(OrderEntity order) {
        refreshExpiredIfNeeded(order);
        orderRepository.save(order);
        return buildStatusResponse(order, null, null);
    }

    public boolean handleWebhook(Map<String, Object> payload) {
        if (payload == null) return false;
        Object dataObj = payload.get("data");
        if (!(dataObj instanceof Map<?, ?> dataRaw)) return false;

        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<?, ?> e : dataRaw.entrySet()) {
            if (e.getKey() != null) data.put(String.valueOf(e.getKey()), e.getValue());
        }

        String signature = asString(payload.get("signature"));
        if (!verifyWebhookSignature(data, signature)) {
            return false;
        }

        Long orderCode = asLong(data.get("orderCode"));
        if (orderCode == null) {
            return false;
        }

        Optional<OrderEntity> opt = orderRepository.findByPayosOrderCode(orderCode);
        if (opt.isEmpty()) return false;

        OrderEntity order = opt.get();
        refreshExpiredIfNeeded(order);

        boolean success = isSuccessWebhook(payload, data);
        if (success) {
            int amount = normalizeAmount(order.getTotal());
            int paidAmount = asInt(data.get("amount"));
            if (amount == paidAmount) {
                order.setPaymentStatus("paid");
                order.setPaidAt(System.currentTimeMillis());
            }
        }

        orderRepository.save(order);
        return true;
    }

    public void refreshExpiredIfNeeded(OrderEntity order) {
        if (order == null) return;
        if (!"pending".equalsIgnoreCase(order.getPaymentStatus())) return;
        Long expiresAt = order.getPaymentExpiresAt();
        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
            order.setPaymentStatus("expired");
        }
    }

    private Map<String, Object> buildStatusResponse(OrderEntity order, String checkoutUrl, String qrCode) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderId", order.getId());
        out.put("paymentStatus", order.getPaymentStatus());
        out.put("paymentMethod", order.getMethod());
        out.put("expiresAt", order.getPaymentExpiresAt());
        out.put("paidAt", order.getPaidAt());
        out.put("payosOrderCode", order.getPayosOrderCode());
        out.put("paymentLinkId", order.getPayosPaymentLinkId());
        if (checkoutUrl != null && !checkoutUrl.isBlank()) out.put("checkoutUrl", checkoutUrl);
        if (qrCode != null && !qrCode.isBlank()) out.put("qrCode", qrCode);
        return out;
    }

    private List<Map<String, Object>> buildItems(OrderEntity order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("name", "Don hang #" + order.getId());
            fallback.put("quantity", 1);
            fallback.put("price", normalizeAmount(order.getTotal()));
            return List.of(fallback);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        order.getItems().forEach(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", truncate(i.getName(), 25));
            m.put("quantity", i.getQty() == null || i.getQty() < 1 ? 1 : i.getQty());
            m.put("price", normalizeAmount(i.getPrice()));
            out.add(m);
        });
        return out;
    }

    private String buildDescription(Long orderId) {
        return truncate("DH" + orderId, 25);
    }

    private long generateOrderCode(Long orderId) {
        long now = System.currentTimeMillis();
        long suffix = (orderId == null ? 0L : (orderId % 100000));
        return now * 100000 + suffix;
    }

    private boolean verifyWebhookSignature(Map<String, Object> data, String signature) {
        if (isBlank(signature) || data == null || data.isEmpty()) return false;
        List<String> keys = data.keySet().stream()
                .filter(k -> !"signature".equalsIgnoreCase(k) && !"sign".equalsIgnoreCase(k))
                .sorted()
                .collect(Collectors.toList());
        String canonical = keys.stream()
                .map(k -> k + "=" + safeString(data.get(k)))
                .collect(Collectors.joining("&"));
        String expected = hmacSha256(canonical, checksumKey);
        return expected.equalsIgnoreCase(signature);
    }

    private boolean isSuccessWebhook(Map<String, Object> payload, Map<String, Object> data) {
        Object successObj = payload.get("success");
        if (successObj instanceof Boolean b && b) return true;
        String payloadCode = asString(payload.get("code"));
        String dataCode = asString(data.get("code"));
        if ("00".equals(payloadCode) || "00".equals(dataCode)) return true;
        String desc = (asString(payload.get("desc")) + " " + asString(data.get("desc"))).toLowerCase(Locale.ROOT);
        return desc.contains("success") || desc.contains("thanh cong");
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC", ex);
        }
    }

    private int normalizeAmount(Double amount) {
        if (amount == null) return 0;
        return (int) Math.round(amount);
    }

    private int normalizeAmount(Object amount) {
        if (amount == null) return 0;
        if (amount instanceof Number n) return n.intValue();
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(amount)));
        } catch (Exception ex) {
            return 0;
        }
    }

    private int asInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception ex) {
            return 0;
        }
    }

    private Long asLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception ex) {
            return null;
        }
    }

    private String asString(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private String safeString(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
