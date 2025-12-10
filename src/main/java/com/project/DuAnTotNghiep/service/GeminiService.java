package com.project.DuAnTotNghiep.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model.name:gemini-pro}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi câu hỏi đến Gemini API và nhận câu trả lời
     */
    public String askGemini(String userMessage) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            logger.error("Gemini API key not configured");
            return "Xin lỗi, chatbot hiện không khả dụng. Vui lòng liên hệ quản trị viên.";
        }

        try {
            // Sử dụng model từ config, fallback là gemini-1.5-flash
            String actualModel = (modelName != null && !modelName.isEmpty()) ? modelName : "gemini-1.5-flash";
            String apiUrl = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    actualModel, geminiApiKey);

            logger.info("Calling Gemini API with model: {} and URL: {}", actualModel,
                    apiUrl.replaceAll("key=.*", "key=***"));

            // Tạo request body
            Map<String, Object> request = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", userMessage);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            request.put("contents", contents);

            // Gửi request
            String requestBody = objectMapper.writeValueAsString(request);
            logger.info("Gemini API Request: {}", requestBody);

            org.springframework.http.HttpEntity<String> httpEntity = new org.springframework.http.HttpEntity<>(
                    requestBody, getJsonHeaders());

            try {
                org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                        apiUrl, httpEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    String responseBody = response.getBody();
                    logger.debug("Gemini response: {}", responseBody);
                    return extractTextFromGeminiResponse(responseBody);
                } else {
                    logger.error("Gemini API error: {} - Body: {}", response.getStatusCode(), response.getBody());
                    return "Xin lỗi, có lỗi xảy ra khi xử lý câu hỏi của bạn (Lỗi: " + response.getStatusCode() + ").";
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                logger.error("HTTP Client Error ({}): {}", e.getStatusCode().value(), e.getResponseBodyAsString());
                if (e.getStatusCode().value() == 400) {
                    return "Xin lỗi, yêu cầu không hợp lệ. Vui lòng kiểm tra lại.";
                } else if (e.getStatusCode().value() == 401) {
                    return "Xin lỗi, API key không hợp lệ. Vui lòng liên hệ quản trị viên.";
                } else if (e.getStatusCode().value() == 429) {
                    return "Xin lỗi, quá nhiều yêu cầu. Vui lòng thử lại sau vài giây.";
                }
                return "Xin lỗi, lỗi HTTP " + e.getStatusCode().value() + " từ server AI.";
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                logger.error("HTTP Server Error ({}): {}", e.getStatusCode().value(), e.getResponseBodyAsString());
                return "Xin lỗi, server AI bị lỗi (" + e.getStatusCode().value() + "). Vui lòng thử lại sau.";
            }
        } catch (RestClientException e) {
            logger.error("Error calling Gemini API: {}", e.getMessage(), e);
            return "Xin lỗi, không thể kết nối đến dịch vụ AI. Vui lòng thử lại sau.";
        } catch (Exception e) {
            logger.error("Error processing Gemini request/response: {}", e.getMessage(), e);
            return "Xin lỗi, có lỗi xảy ra khi xử lý phản hồi. " + e.getMessage();
        }
    }

    /**
     * Trích xuất text từ response của Gemini
     */
    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode candidates = root.get("candidates");

            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    com.fasterxml.jackson.databind.JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode text = parts.get(0).get("text");
                        if (text != null) {
                            return text.asText();
                        }
                    }
                }
            }

            logger.warn("Could not extract text from Gemini response: {}", responseBody);
            return "Xin lỗi, không thể lấy được phản hồi từ AI.";
        } catch (Exception e) {
            logger.error("Error extracting text from Gemini response", e);
            return "Xin lỗi, có lỗi xảy ra khi xử lý phản hồi.";
        }
    }

    /**
     * Tạo header cho JSON request
     */
    private org.springframework.http.HttpHeaders getJsonHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * Gửi câu hỏi về sản phẩm (hỗ trợ recommendation)
     */
    public String getProductRecommendation(String userRequest) {
        String systemPrompt = "Bạn là một trợ lý bán hàng thân thiện và chuyên nghiệp cho một cửa hàng. " +
                "Bạn giúp khách hàng tìm sản phẩm phù hợp với nhu cầu của họ. " +
                "Hãy hỏi chi tiết về yêu cầu của khách hàng nếu cần thiết và đưa ra gợi ý sản phẩm cụ thể, " +
                "bao gồm các đặc điểm, lợi ích và lý do tại sao sản phẩm đó phù hợp. " +
                "Hãy trả lời bằng tiếng Việt một cách thân thiện và dễ hiểu.";
        String fullPrompt = systemPrompt + "\n\nYêu cầu của khách hàng: " + userRequest;
        return askGemini(fullPrompt);
    }

    /**
     * Gợi ý sản phẩm nhưng ưu tiên sử dụng danh sách sản phẩm nội bộ
     * (productSummaries).
     * productSummaries: mỗi phần tử là một chuỗi tóm tắt ngắn gọn của sản phẩm
     * (tên, mã, giá nếu có).
     */
    public String getProductRecommendation(String userRequest, List<String> productSummaries) {
        String systemPrompt = "Bạn là một trợ lý bán hàng thân thiện và chuyên nghiệp cho một cửa hàng. " +
                "Bạn giúp khách hàng tìm sản phẩm phù hợp với nhu cầu của họ. " +
                "Hãy hỏi chi tiết về yêu cầu của khách hàng nếu cần thiết và đưa ra gợi ý sản phẩm cụ thể, " +
                "bao gồm các đặc điểm, lợi ích và lý do tại sao sản phẩm đó phù hợp. " +
                "Hãy trả lời bằng tiếng Việt một cách thân thiện và dễ hiểu.";

        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");

        if (productSummaries != null && !productSummaries.isEmpty()) {
            sb.append("Danh sách sản phẩm nội bộ phù hợp (hãy ưu tiên đề xuất từ danh sách này):\n");
            int idx = 1;
            for (String s : productSummaries) {
                sb.append(idx++).append(". ").append(s).append("\n");
            }
            sb.append(
                    "\nQuan trọng: Chỉ đề xuất các sản phẩm từ danh sách ở trên nếu phù hợp. Nếu không có sản phẩm nào phù hợp, hãy báo rõ rằng không tìm thấy sản phẩm phù hợp trong kho và hỏi thêm thông tin từ khách hàng.\n\n");
        }

        sb.append("Yêu cầu của khách hàng: ").append(userRequest);
        String fullPrompt = sb.toString();
        return askGemini(fullPrompt);
    }

    /**
     * Strict recommendation: provide `products` (as maps with
     * id,name,code,price,url,image) and
     * require the model to return ONLY a JSON object with schema:
     * {
     * "matches": [ {"id": ..., "name": ..., "code": ..., "price": ..., "reason":
     * "..."}, ... ],
     * "message": "..."
     * }
     * If no product matches, return matches: [] and a friendly message.
     * This method will attempt to parse the model output as JSON and return the
     * JSON string.
     */
    public Optional<Map<String, Object>> getProductRecommendationStructured(String userRequest,
            List<Map<String, Object>> products) {
        if (products == null)
            products = Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a strict product recommender for a single store.\n");
        sb.append(
                "You MUST ONLY recommend products that are present in the provided list. Do NOT invent or mention any products not in the list.\n");
        sb.append("Return ONLY a single JSON object (no extra text) with the exact schema:\n");
        sb.append(
                "{ \"matches\": [ { \"id\": <id>, \"name\": <name>, \"code\": <code>, \"price\": <price>, \"reason\": <string> } , ... ], \"message\": <string> }\n");
        sb.append(
                "If there are no matching products, return {\"matches\": [], \"message\": \"No matches found in inventory\"} .\n");
        sb.append("Always respond in valid JSON only.\n\n");

        sb.append("Available products (JSON array):\n");
        try {
            sb.append(objectMapper.writeValueAsString(products));
        } catch (Exception e) {
            logger.warn("Could not serialize products to include in prompt", e);
            sb.append("[]");
        }
        sb.append("\n\nUser request: ").append(userRequest).append("\n");

        String prompt = sb.toString();

        String raw = askGemini(prompt);
        logger.info("Strict recommendation raw response: {}", raw);

        // Try to extract a JSON object from the raw response and parse it
        String jsonText = extractJsonFromText(raw);
        if (jsonText != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(jsonText,
                        new TypeReference<Map<String, Object>>() {
                        });
                return Optional.of(parsed);
            } catch (Exception e) {
                logger.warn("Failed to parse JSON from model response, will build local structured fallback", e);
            }
        } else {
            logger.warn("Could not find JSON in model response, building local structured fallback");
        }

        // Fallback: construct a structured response directly from DB products so the
        // chatbot always prioritizes internal items even when AI fails.
        Map<String, Object> fallback = new HashMap<>();
        java.util.List<Map<String, Object>> matches = new ArrayList<>();
        int limit = Math.min(5, products.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> p = products.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.get("id"));
            m.put("name", p.get("name"));
            m.put("code", p.get("code"));
            m.put("price", p.get("price"));
            Object priceObj = p.get("price");
            String reason = "Sản phẩm phù hợp từ kho";
            try {
                if (priceObj instanceof Number) {
                    double pr = ((Number) priceObj).doubleValue();
                    reason = String.format("Sản phẩm có giá %,.0fđ, phù hợp với yêu cầu.", pr);
                }
            } catch (Exception ex) {
                // ignore
            }
            m.put("reason", reason);
            matches.add(m);
        }
        fallback.put("matches", matches);
        fallback.put("message", "Gợi ý trực tiếp từ kho: hiển thị " + matches.size() + " sản phẩm.");
        return Optional.of(fallback);
    }

    /**
     * Attempt to extract a JSON object substring from a free-text response.
     */
    private String extractJsonFromText(String text) {
        if (text == null)
            return null;
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return text.substring(first, last + 1);
        }
        return null;
    }

    /**
     * Gửi câu hỏi hỗ trợ khách hàng
     */
    public String getCustomerSupport(String question) {
        String systemPrompt = "Bạn là một nhân viên hỗ trợ khách hàng chuyên nghiệp và thân thiện. " +
                "Bạn làm việc cho một cửa hàng bán hàng online. " +
                "Hãy trả lời các câu hỏi của khách hàng một cách chi tiết, hữu ích và thân thiện. " +
                "Nếu không biết câu trả lời, hãy đề nghị khách hàng liên hệ với bộ phận hỗ trợ. " +
                "Luôn trả lời bằng tiếng Việt.";
        String fullPrompt = systemPrompt + "\n\nCâu hỏi: " + question;
        return askGemini(fullPrompt);
    }
}
