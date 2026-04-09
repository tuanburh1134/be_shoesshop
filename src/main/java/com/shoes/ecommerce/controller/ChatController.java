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
            String endpoint = buildEndpointUrl();
            String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "key=" + geminiApiKey;
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

            ResponseEntity<String> res = restTemplate.postForEntity(url, entity, String.class);
            if(!res.getStatusCode().is2xxSuccessful()){
                String msg = "Gemini API error: " + res.getStatusCode();
                log.warn(msg + " body={} " , res.getBody());
                return ResponseEntity.status(res.getStatusCode())
                        .body(new ChatResponse(null, msg));
            }
            String body = res.getBody();
            String reply = extractText(body);
            return ResponseEntity.ok(new ChatResponse(reply, null));
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

    private String buildEndpointUrl(){
        String template = geminiEndpointTemplate == null || geminiEndpointTemplate.isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
                : geminiEndpointTemplate.trim();

        if(template.contains("%s")) return String.format(template, geminiModel);
        if(template.contains("{model}")) return template.replace("{model}", geminiModel);
        if(template.endsWith(":generateContent")) return template;
        if(template.endsWith("/")) return template + geminiModel + ":generateContent";
        return template + "/" + geminiModel + ":generateContent";
    }
}
