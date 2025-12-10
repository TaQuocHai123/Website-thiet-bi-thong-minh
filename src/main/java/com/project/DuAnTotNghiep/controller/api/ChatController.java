package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.service.GeminiService;
import com.project.DuAnTotNghiep.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.project.DuAnTotNghiep.entity.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final GeminiService geminiService;
    private final ProductRepository productRepository;

    public ChatController(GeminiService geminiService, ProductRepository productRepository) {
        this.geminiService = geminiService;
        this.productRepository = productRepository;
    }

    /**
     * Endpoint để gửi tin nhắn đến chatbot
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestParam String message) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (message == null || message.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập tin nhắn");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Chat message received: {}", message);

            // If user asks for contact methods, return shop contact directly
            String lower = message.toLowerCase();
            if (lower.contains("liên lạc") || lower.contains("lien lac") || lower.contains("hotline")
                    || lower.contains("số điện thoại") || lower.contains("email")
                    || lower.contains("phương thức liên lạc")) {
                String contact = "Bạn có thể liên hệ cửa hàng qua Hotline: 0364023659 hoặc Email: haihaupy123@gmail.com";
                response.put("success", true);
                response.put("message", contact);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            }

            // If the user asks for product recommendations inside chat (e.g., "sản phẩm
            // dưới 100k"),
            // forward to the recommendation handler so we can return internal products.
            boolean looksLikeRecommend = false;
            if (lower.contains("sản phẩm") || lower.contains("gợi ý") || lower.contains("gợi y")
                    || lower.contains("gợi")) {
                looksLikeRecommend = true;
            }
            // Also treat phrases that explicitly mention price/budget as recommendation
            if (!looksLikeRecommend) {
                if (lower.matches(".*(dưới|duoi|dưới|dưới giá|trên|từ|dưới)\\s*\\d.*")
                        || lower.matches(".*\\d+\\s*[kKmM].*")) {
                    looksLikeRecommend = true;
                }
            }

            if (looksLikeRecommend) {
                // reuse recommendation endpoint logic
                return getRecommendation(message);
            }

            // Gửi tin nhắn đến Gemini
            String aiResponse = geminiService.askGemini(message);

            logger.info("Chat response sent. Response length: {}", aiResponse.length());

            response.put("success", true);
            response.put("message", aiResponse);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in chat endpoint: ", e);
            response.put("success", false);
            response.put("message", "Xin lỗi, có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint để gợi ý sản phẩm
     */
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> getRecommendation(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (query == null || query.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập yêu cầu");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Product recommendation request: {}", query);
            // Detect numeric budget in query and support suffixes like k/K (thousand), m/M
            // (million)
            Double budget = null;
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+[\\.,]?\\d*)\\s*([kKmM]?)");
                java.util.regex.Matcher mm = p.matcher(query);
                if (mm.find()) {
                    String num = mm.group(1).replaceAll("\\.", "").replaceAll(",", "");
                    String suffix = mm.group(2).trim();
                    double base = Double.parseDouble(num);
                    if (suffix != null && !suffix.isEmpty() && suffix.equalsIgnoreCase("k")) {
                        base = base * 1000.0;
                    } else if (suffix != null && !suffix.isEmpty() && suffix.equalsIgnoreCase("m")) {
                        base = base * 1000000.0;
                    }
                    // Heuristic: if the number looks like 100 (and no suffix) but user likely meant
                    // 100k,
                    // we do NOT auto-scale. User should write '100k' or '100.000' for explicit
                    // thousands.
                    budget = base;
                    logger.info("Detected budget: {} from query: {}", budget, query);
                }
            } catch (Exception ex) {
                logger.warn("Failed to parse budget from query", ex);
                budget = null;
            }

            // Get candidate products and filter by budget if provided
            Pageable topFive = PageRequest.of(0, 10);
            java.util.List<Product> productEntities = productRepository.searchProductName(query, topFive).getContent();
            java.util.List<Product> filtered = new java.util.ArrayList<>();
            for (Product p : productEntities) {
                if (!p.isDeleteFlag()) {
                    if (budget == null || p.getPrice() <= budget) {
                        filtered.add(p);
                    }
                }
            }

            // If filtered is empty and budget specified, try fetching all and filter
            if (filtered.isEmpty() && budget != null) {
                java.util.List<Product> all = productRepository.findAllByDeleteFlagFalse(PageRequest.of(0, 50))
                        .getContent();
                for (Product p : all) {
                    if (p.getPrice() <= budget)
                        filtered.add(p);
                }
            }

            // Build simple product summaries
            java.util.List<String> summaries = new java.util.ArrayList<>();
            for (Product p : filtered) {
                String s = String.format("%s - code:%s - giá: %,.0f VNĐ", p.getName(), p.getCode(), p.getPrice());
                summaries.add(s);
            }
            // If we have internal filtered products, return them directly to the frontend
            if (!filtered.isEmpty()) {
                java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();
                for (Product p : filtered) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("code", p.getCode());
                    m.put("price", p.getPrice());
                    // image and url
                    String image = null;
                    try {
                        image = p.getImage().get(0).getLink();
                    } catch (Exception ex) {
                        image = "/images/no-image.png";
                    }
                    m.put("image", image);
                    m.put("url", "/product-detail/" + p.getId());
                    products.add(m);
                }

                // Ask Gemini to produce a strict JSON recommendation (only picking from these
                // products)
                try {
                    java.util.Optional<java.util.Map<String, Object>> structured = geminiService
                            .getProductRecommendationStructured(query, products);
                    if (structured.isPresent()) {
                        Map<String, Object> parsed = structured.get();
                        response.put("success", true);
                        response.put("structured", parsed);
                        response.put("products", products);
                        response.put("timestamp", System.currentTimeMillis());
                        return ResponseEntity.ok(response);
                    }
                } catch (Exception ex) {
                    logger.warn("Structured recommendation failed, falling back to simple list", ex);
                }

                // Fallback: return products and an HTML fragment for the chat
                StringBuilder html = new StringBuilder();
                html.append("<div class=\"chatbot-product-list\">");
                html.append("<p>Dưới đây là một số sản phẩm phù hợp từ cửa hàng của bạn:</p>");
                for (Map<String, Object> m : products) {
                    html.append("<div class=\"chatbot-product-item\">");
                    html.append("<a href=\"" + m.get("url") + "\" target=\"_blank\">");
                    html.append("<img src=\"" + m.get("image") + "\" alt=\"" + m.get("name")
                            + "\" style=\"width:60px;height:60px;object-fit:cover;margin-right:8px;\">");
                    html.append("</a>");
                    html.append("<div class=\"chatbot-product-meta\">");
                    html.append("<a href=\"" + m.get("url") + "\" target=\"_blank\"><strong>" + m.get("name")
                            + "</strong></a><br/>");
                    html.append("Mã: " + m.get("code") + " - Giá: "
                            + String.format("%,.0f VND", ((Number) m.get("price")).doubleValue()));
                    html.append("</div>");
                    html.append("</div>");
                }
                html.append("</div>");

                response.put("success", true);
                response.put("products", products);
                response.put("recommendation", html.toString());
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                // No internal matches, call AI without internal context
                String recommendation = geminiService.getProductRecommendation(query);
                response.put("success", true);
                response.put("recommendation", recommendation);
            }
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in recommendation endpoint", e);
            response.put("success", false);
            response.put("message", "Xin lỗi, không thể lấy gợi ý sản phẩm.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint để hỏi câu hỏi hỗ trợ khách hàng
     */
    @PostMapping("/support")
    public ResponseEntity<Map<String, Object>> getSupport(@RequestParam String question) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (question == null || question.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập câu hỏi");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Customer support question: {}", question);

            String answer = geminiService.getCustomerSupport(question);

            response.put("success", true);
            response.put("answer", answer);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in support endpoint", e);
            response.put("success", false);
            response.put("message", "Xin lỗi, không thể xử lý câu hỏi của bạn.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Return quick option labels for the chatbot UI.
     */
    @GetMapping("/options")
    public ResponseEntity<java.util.List<Map<String, String>>> getOptions() {
        java.util.List<Map<String, String>> options = new java.util.ArrayList<>();

        Map<String, String> opt1 = new HashMap<>();
        opt1.put("type", "order");
        opt1.put("label", "Phương thức đặt hàng");
        options.add(opt1);

        Map<String, String> opt2 = new HashMap<>();
        opt2.put("type", "contact");
        opt2.put("label", "Phương thức liên hệ");
        options.add(opt2);

        Map<String, String> opt3 = new HashMap<>();
        opt3.put("type", "address");
        opt3.put("label", "Địa chỉ cửa hàng");
        options.add(opt3);

        return ResponseEntity.ok(options);
    }

    /**
     * Return the predefined info for an option type.
     * type: order | contact | address
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getOptionInfo(@RequestParam String type) {
        Map<String, Object> response = new HashMap<>();
        try {
            String lower = type == null ? "" : type.toLowerCase();
            if (lower.contains("order") || lower.contains("đặt") || lower.contains("dat")) {
                String orderInfo = "Phương thức đặt hàng:\n1) Đặt trực tiếp trên website, chọn sản phẩm và thanh toán online hoặc khi nhận hàng (COD).\n2) Gọi hotline để đặt hàng: 0364023659.\n3) Gửi email đặt hàng: haihaupy123@gmail.com";
                response.put("success", true);
                response.put("message", orderInfo);
                return ResponseEntity.ok(response);
            } else if (lower.contains("contact") || lower.contains("liên") || lower.contains("lien")) {
                String contact = "Phương thức liên hệ:\nHotline: 0364023659\nEmail: haihaupy123@gmail.com\nFanpage: https://facebook.com/yourshop";
                response.put("success", true);
                response.put("message", contact);
                return ResponseEntity.ok(response);
            } else if (lower.contains("address") || lower.contains("địa") || lower.contains("dia")) {
                String addr = "Địa chỉ cửa hàng:\n- Địa chỉ 1: 364/45/27 Dương Quảng Hàm, Phường 5, Gò Vấp, TP Hồ Chí Minh\n- Địa chỉ 2: Xuân Thạnh 1, Hòa Tân Tây, Tây Hòa, Phú Yên";
                response.put("success", true);
                response.put("message", addr);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không nhận diện được loại thông tin. Vui lòng chọn một trong các tuỳ chọn.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server.");
            return ResponseEntity.status(500).body(response);
        }
    }
}
