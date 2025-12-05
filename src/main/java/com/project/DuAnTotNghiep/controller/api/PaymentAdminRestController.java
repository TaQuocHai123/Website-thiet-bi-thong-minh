package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.config.ConfigVNPay;
import com.project.DuAnTotNghiep.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import com.project.DuAnTotNghiep.repository.PaymentRepository;
import com.project.DuAnTotNghiep.entity.Payment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/admin/payment")
public class PaymentAdminRestController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentAdminRestController.class);

    private final PaymentRepository paymentRepository;
    private final CartService cartService;

    public PaymentAdminRestController(PaymentRepository paymentRepository, CartService cartService) {
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
    }

    @GetMapping(value = "/check-vnpay", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> checkVNPay(HttpServletRequest req) {
        Map<String, Object> data = new HashMap<>();
        // Config values
        String payUrl = ConfigVNPay.vnp_PayUrl;
        String tmn = ConfigVNPay.vnp_TmnCode;
        String secret = ConfigVNPay.secretKey;
        String returnUrl = ConfigVNPay.vnp_ReturnUrl;

        data.put("vnp_PayUrl", payUrl);
        data.put("vnp_TmnCode", tmn);
        data.put("vnp_ReturnUrl", returnUrl);
        data.put("vnp_SecretLength", (secret != null) ? secret.length() : 0);

        // Basic reachability checks
        data.put("payUrlReachable", httpGetStatus(payUrl));
        data.put("returnUrlReachable", httpGetStatus(returnUrl));

        // Build a test minimal VNPay request similar to the production code
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmn);
        vnp_Params.put("vnp_Amount", "1000");
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", ConfigVNPay.getRandomNumber(8));
        vnp_Params.put("vnp_OrderInfo", "VNPay test");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);

        // Date/time fields
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", createDate);
        cld.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", expireDate);

        // Build query and hash like the real flow
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        Map<String, Object> testResponse = new HashMap<>();
        try {
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    hashData.append(fieldName).append("=").append(fieldValue);
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                            .append("=")
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (java.io.UnsupportedEncodingException e) {
            logger.error("Encoding to ASCII failed in admin VNPay check: {}", e.getMessage());
            testResponse.put("ok", false);
            testResponse.put("error", "Encoding error: " + e.getMessage());
            data.put("testResponse", testResponse);
            return data;
        }
        String vnp_SecureHash = ConfigVNPay.hmacSHA512(secret, hashData.toString());
        String testUrl = payUrl + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;

        // Mask sensitive query values (hashed secure fields) before returning to admin
        // UI
        String safeTestUrl = testUrl.replaceAll("(vnp_SecureHash=[^&]+)", "vnp_SecureHash=***");
        data.put("testUrl", safeTestUrl);

        // Now perform a GET (not a redirect simulate) and check the content for VNPay
        // rejection messages
        try {
            java.net.URI uri = java.net.URI.create(testUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            int status = conn.getResponseCode();
            testResponse.put("statusCode", status);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            int read = 0;
            while ((line = in.readLine()) != null && read < 200) {
                content.append(line);
                read++;
            }
            in.close();
            String snippet = content.toString();
            testResponse.put("contentSnippet", snippet.substring(0, Math.min(snippet.length(), 400)));
            // Check for known error markers
            boolean notApproved = snippet.contains("chưa được phê duyệt") || snippet.contains("code=71")
                    || snippet.contains("Not approve");
            testResponse.put("notApprovedDetected", notApproved);
            testResponse.put("ok", true);
        } catch (Exception ex) {
            logger.error("VNPay check failed: {}", ex.getMessage());
            testResponse.put("ok", false);
            testResponse.put("error", ex.getMessage());
        }
        data.put("testResponse", testResponse);

        return data;
    }

    @GetMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getPayment(String orderId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (orderId == null || orderId.trim().isEmpty()) {
                resp.put("ok", false);
                resp.put("error", "orderId is required");
                return resp;
            }
            Payment p = paymentRepository.findByOrderId(orderId);
            if (p == null) {
                resp.put("ok", false);
                resp.put("error", "Payment not found");
                return resp;
            }
            resp.put("orderId", p.getOrderId());
            resp.put("amount", p.getAmount());
            resp.put("orderStatus", p.getOrderStatus());
            resp.put("billId", p.getBill() != null ? p.getBill().getId() : null);
            resp.put("hasOrderPayload", p.getOrderPayload() != null && !p.getOrderPayload().isEmpty());
            resp.put("ok", true);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getPendingPayments() {
        Map<String, Object> resp = new HashMap<>();
        try {
            java.util.List<com.project.DuAnTotNghiep.entity.Payment> payments = paymentRepository
                    .findAllByOrderStatusAndBillIsNull("1");
            java.util.List<Map<String, Object>> list = new ArrayList<>();
            for (com.project.DuAnTotNghiep.entity.Payment p : payments) {
                Map<String, Object> item = new HashMap<>();
                item.put("orderId", p.getOrderId());
                item.put("amount", p.getAmount());
                item.put("hasOrderPayload", p.getOrderPayload() != null && !p.getOrderPayload().isEmpty());
                item.put("paymentDate", p.getPaymentDate());
                list.add(item);
            }
            resp.put("ok", true);
            resp.put("pending", list);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    @PostMapping(value = "/create-bill-pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createBillFromPending() {
        Map<String, Object> resp = new HashMap<>();
        try {
            java.util.List<com.project.DuAnTotNghiep.entity.Payment> payments = paymentRepository
                    .findAllByOrderStatusAndBillIsNull("1");
            if (payments == null || payments.isEmpty()) {
                resp.put("ok", true);
                resp.put("message", "No pending payments to process");
                return resp;
            }
            ObjectMapper mapper = new ObjectMapper();
            int success = 0;
            int fail = 0;
            for (com.project.DuAnTotNghiep.entity.Payment p : payments) {
                try {
                    if (p.getOrderPayload() == null || p.getOrderPayload().isEmpty()) {
                        fail++;
                        continue;
                    }
                    java.util.Map<String, Object> payloadMap = mapper.readValue(p.getOrderPayload(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                    com.project.DuAnTotNghiep.utils.JsonPayloadUtils.sanitizeEmptyStringsInMap(payloadMap);
                    com.project.DuAnTotNghiep.dto.Order.OrderDto payloadOrder = com.project.DuAnTotNghiep.utils.JsonPayloadUtils
                            .convertMapToOrderDto(mapper, payloadMap);
                    Long accountId = null;
                    if (payloadOrder != null && payloadOrder.getAccountId() != null)
                        accountId = payloadOrder.getAccountId();
                    else if (payloadMap.get("accountId") != null)
                        accountId = Long.parseLong(payloadMap.get("accountId").toString());
                    cartService.orderUserFromPayload(payloadOrder, accountId);
                    success++;
                } catch (Exception e) {
                    logger.error("Failed to create bill from pending payment {}, error: {}", p.getOrderId(),
                            e.getMessage());
                    fail++;
                }
            }
            resp.put("ok", true);
            resp.put("success", success);
            resp.put("fail", fail);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    @PostMapping(value = "/create-bill", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createBillFromPayment(@RequestParam String orderId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (orderId == null || orderId.trim().isEmpty()) {
                resp.put("ok", false);
                resp.put("error", "orderId is required");
                return resp;
            }
            Payment p = paymentRepository.findByOrderId(orderId);
            if (p == null) {
                resp.put("ok", false);
                resp.put("error", "Payment not found");
                return resp;
            }
            if (p.getBill() != null) {
                resp.put("ok", true);
                resp.put("message", "Payment already attached to bill: " + p.getBill().getId());
                return resp;
            }
            if (p.getOrderPayload() == null || p.getOrderPayload().isEmpty()) {
                resp.put("ok", false);
                resp.put("error", "No orderPayload present on payment");
                return resp;
            }
            ObjectMapper mapper = new ObjectMapper();
            java.util.Map<String, Object> payloadMap = mapper.readValue(p.getOrderPayload(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                    });
            com.project.DuAnTotNghiep.utils.JsonPayloadUtils.sanitizeEmptyStringsInMap(payloadMap);
            OrderDto payloadOrder = com.project.DuAnTotNghiep.utils.JsonPayloadUtils.convertMapToOrderDto(mapper,
                    payloadMap);
            Long accountId = null;
            if (payloadOrder != null && payloadOrder.getAccountId() != null)
                accountId = payloadOrder.getAccountId();
            else if (payloadMap.get("accountId") != null)
                accountId = Long.parseLong(payloadMap.get("accountId").toString());
            cartService.orderUserFromPayload(payloadOrder, accountId);
            resp.put("ok", true);
            resp.put("message", "Bill created and payment attached if successful");
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    private Map<String, Object> httpGetStatus(String urlStr) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.net.URI uri2 = java.net.URI.create(urlStr);
            URL url = uri2.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(6000);
            con.setReadTimeout(6000);
            int status = con.getResponseCode();
            result.put("statusCode", status);
            result.put("ok", status >= 200 && status < 400);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
