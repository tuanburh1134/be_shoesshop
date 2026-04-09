package com.shoes.ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoes.ecommerce.dto.ChatRequest;
import com.shoes.ecommerce.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/ai", "/api"})
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Value("${gemini.api.key:${GOOGLE_API_KEY:}}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.api.endpoint-template:https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent}")
    private String geminiEndpointTemplate;

    @Value("${gemini.api.fallback-models:gemini-2.0-flash,gemini-1.5-flash}")
    private String geminiFallbackModels;

    @Value("${gemini.retry.max-attempts:3}")
    private int geminiMaxAttempts;

    @Value("${gemini.retry.initial-delay-ms:500}")
    private long geminiRetryInitialDelayMs;

    @Value("${gemini.system.instruction:Bạn là trợ lý AI của website bán giày. Trả lời ngắn gọn, chính xác, thân thiện bằng tiếng Việt. Ưu tiên tư vấn sản phẩm, size, màu, giá, cách đặt hàng và thanh toán. Nếu không chắc chắn, hãy nói rõ giới hạn và gợi ý người dùng liên hệ admin.}")
    private String systemInstruction;

    @Value("${gemini.temperature:0.7}")
    private double temperature;

    @Value("${gemini.max-output-tokens:512}")
    private int maxOutputTokens;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req){
        if(req == null || req.getMessage() == null || req.getMessage().trim().isEmpty()){
            return ResponseEntity.badRequest().body(new ChatResponse(null, "message is required"));
        }
        if(geminiApiKey == null || geminiApiKey.isBlank()){
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(new ChatResponse(null, "Chưa cấu hình GEMINI_API_KEY (hoặc gemini.api.key)"));
        }
        try{
            Map<String,Object> payload = new HashMap<>();
            Map<String,Object> part = new HashMap<>();
            part.put("text", req.getMessage());
            Map<String,Object> content = new HashMap<>();
            content.put("role", "user");
            content.put("parts", new Object[]{part});
            payload.put("contents", new Object[]{content});

            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", systemInstruction);
            Map<String, Object> sys = new HashMap<>();
            sys.put("parts", new Object[]{sysPart});
            payload.put("systemInstruction", sys);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", Math.max(0d, Math.min(2d, temperature)));
            generationConfig.put("maxOutputTokens", Math.max(64, Math.min(2048, maxOutputTokens)));
            payload.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String,Object>> entity = new HttpEntity<>(payload, headers);

            int attempts = Math.max(1, geminiMaxAttempts);
            long initialDelay = Math.max(100L, geminiRetryInitialDelayMs);
            List<String> models = buildCandidateModels();
            String lastError = null;

            for(int modelIdx = 0; modelIdx < models.size(); modelIdx++){
                String model = models.get(modelIdx);
                boolean hasNextModel = modelIdx < models.size() - 1;

                for(int attempt = 1; attempt <= attempts; attempt++){
                    String endpoint = buildEndpointUrl(model);
                    String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "key=" + geminiApiKey;

                    try{
                        ResponseEntity<String> res = restTemplate.postForEntity(url, entity, String.class);
                        if(!res.getStatusCode().is2xxSuccessful()){
                            lastError = "Gemini API error: " + res.getStatusCode();
                            log.warn("{} model={} body={}", lastError, model, res.getBody());
                            if(isRetryableStatus(res.getStatusCode().value()) && attempt < attempts){
                                sleepBeforeRetry(attempt, initialDelay);
                                continue;
                            }
                            if(isRetryableStatus(res.getStatusCode().value()) && hasNextModel){
                                break;
                            }
                            return ResponseEntity.status(res.getStatusCode())
                                    .body(new ChatResponse(null, lastError));
                        }

                        String body = res.getBody();
                        String reply = extractText(body);
                        return ResponseEntity.ok(new ChatResponse(reply, null));
                    }catch(HttpStatusCodeException ex){
                        int status = ex.getStatusCode().value();
                        String providerMessage = extractProviderErrorMessage(ex.getResponseBodyAsString());
                        lastError = "Gemini API error: " + ex.getStatusCode() + (providerMessage.isBlank() ? "" : " - " + providerMessage);
                        log.warn("Gemini call failed model={} attempt={}/{} status={} body={}", model, attempt, attempts, ex.getStatusCode(), ex.getResponseBodyAsString());

                        if(isRetryableStatus(status) && attempt < attempts){
                            sleepBeforeRetry(attempt, initialDelay);
                            continue;
                        }
                        if(isRetryableStatus(status) && hasNextModel){
                            break;
                        }
                        if(status == 503){
                            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                    .body(new ChatResponse(null, "Hệ thống AI đang quá tải tạm thời. Vui lòng thử lại sau ít phút."));
                        }
                        return ResponseEntity.status(ex.getStatusCode())
                                .body(new ChatResponse(null, lastError));
                    }catch(ResourceAccessException ex){
                        lastError = "Không kết nối được tới Gemini API: " + ex.getMessage();
                        log.warn("Cannot reach Gemini API model={} attempt={}/{}", model, attempt, attempts, ex);
                        if(attempt < attempts){
                            sleepBeforeRetry(attempt, initialDelay);
                            continue;
                        }
                        if(hasNextModel){
                            break;
                        }
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(new ChatResponse(null, lastError));
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse(null,
                            (lastError == null || lastError.isBlank())
                                    ? "Hệ thống AI đang bận. Vui lòng thử lại sau."
                                    : "Hệ thống AI đang bận. " + lastError));
        }catch(HttpStatusCodeException ex){
            String msg = "Gemini API error: " + ex.getStatusCode();
            log.error(msg + " body={} ", ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode())
                    .body(new ChatResponse(null, msg + " - " + ex.getResponseBodyAsString()));
        }catch(ResourceAccessException ex){
            log.error("Cannot reach Gemini API", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ChatResponse(null, "Không kết nối được tới Gemini API: " + ex.getMessage()));
        }catch(Exception ex){
            log.error("Chat error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse(null, "Server error: " + ex.getMessage()));
        }
    }

    private String extractText(String json){
        try{
            JsonNode root = mapper.readTree(json);
            JsonNode candidates = root.path("candidates");
            if(candidates.isArray() && candidates.size()>0){
                JsonNode textNode = candidates.get(0).path("content").path("parts");
                if(textNode.isArray() && textNode.size()>0){
                    return textNode.get(0).path("text").asText("Không có phản hồi.");
                }
            }
        }catch(Exception ignored){}
        return "Không có phản hồi từ AI.";
    }

    private List<String> buildCandidateModels(){
        List<String> models = new ArrayList<>();
        if(geminiModel != null && !geminiModel.isBlank()) models.add(geminiModel.trim());

        if(geminiFallbackModels != null && !geminiFallbackModels.isBlank()){
            String[] arr = geminiFallbackModels.split(",");
            for(String raw : arr){
                String m = raw == null ? "" : raw.trim();
                if(m.isEmpty()) continue;
                if(models.stream().noneMatch(x -> x.equalsIgnoreCase(m))){
                    models.add(m);
                }
            }
        }

        if(models.isEmpty()) models.add("gemini-1.5-flash");
        return models;
    }

    private boolean isRetryableStatus(int code){
        return code == 429 || code == 503 || code == 504 || code >= 500;
    }

    private void sleepBeforeRetry(int attempt, long initialDelayMs){
        long delay = initialDelayMs * Math.max(1, attempt);
        try{ Thread.sleep(delay); }catch(InterruptedException ie){ Thread.currentThread().interrupt(); }
    }

    private String extractProviderErrorMessage(String body){
        try{
            JsonNode root = mapper.readTree(body);
            JsonNode msg = root.path("error").path("message");
            if(msg.isTextual()) return msg.asText("");
        }catch(Exception ignored){}
        return "";
    }

    private String buildEndpointUrl(String model){
        String template = geminiEndpointTemplate == null || geminiEndpointTemplate.isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
                : geminiEndpointTemplate.trim();

        String finalModel = (model == null || model.isBlank()) ? "gemini-1.5-flash" : model.trim();

        if(template.contains("%s")) return String.format(template, finalModel);
        if(template.contains("{model}")) return template.replace("{model}", finalModel);
        if(template.endsWith(":generateContent")) return template;
        if(template.endsWith("/")) return template + finalModel + ":generateContent";
        return template + "/" + finalModel + ":generateContent";
    }
}
