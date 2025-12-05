package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.config.ConfigVNPay;
import com.project.DuAnTotNghiep.dto.Payment.PaymentDto;
import com.project.DuAnTotNghiep.dto.Payment.PaymentResultDto;
import com.project.DuAnTotNghiep.entity.Payment;
import com.project.DuAnTotNghiep.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentRestController {

    private final PaymentRepository paymentRepository;
    private static final Logger logger = LoggerFactory.getLogger(PaymentRestController.class);

    public PaymentRestController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping()
    public ResponseEntity<PaymentDto> createPayment(HttpServletRequest req) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        // long amount = Integer.parseInt(req.getParameter("amount"))*100;
        // String bankCode = req.getParameter("bankCode");

        long amount = Integer.parseInt(req.getParameter("amount")) * 100;

        String orderIdParam = req.getParameter("orderId");
        String vnp_TxnRef = (orderIdParam != null && !orderIdParam.isEmpty()) ? orderIdParam
                : ConfigVNPay.getRandomNumber(8);
        String vnp_IpAddr = ConfigVNPay.getIpAddress(req);

        String vnp_TmnCode = ConfigVNPay.vnp_TmnCode;
        if (orderIdParam != null && !orderIdParam.isEmpty()) {
            logger.info("VNPay.createPayment: using provided orderId as txnRef={}, tmnCode={}", vnp_TxnRef,
                    vnp_TmnCode);
        }

        // Save to db truoc
        // Optional payload that contains the order details as JSON (stored server-side
        // until payment success)
        String orderPayload = req.getParameter("orderPayload");
        // Enrich the order payload with accountId (if user is authenticated) and
        // assigned orderId
        try {
            if (orderPayload != null && !orderPayload.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> payloadMap = mapper.readValue(orderPayload, java.util.Map.class);
                // If user is logged in, attach account id to make server-side order creation
                // possible on notify
                try {
                    com.project.DuAnTotNghiep.entity.Account current = com.project.DuAnTotNghiep.utils.UserLoginUtil
                            .getCurrentLogin();
                    if (current != null && current.getId() != null)
                        payloadMap.put("accountId", current.getId());
                } catch (Exception ignored) {
                }
                // Add the generated txnRef so orderUserFromPayload can reference it
                payloadMap.put("orderId", vnp_TxnRef);
                // Remove empty string values for numeric fields to prevent deserialization
                // errors later
                com.project.DuAnTotNghiep.utils.JsonPayloadUtils.sanitizeEmptyStringsInMap(payloadMap);
                orderPayload = mapper.writeValueAsString(payloadMap);
            }
        } catch (Exception ignored) {
        }

        PaymentResultDto paymentResultDto = new PaymentResultDto();
        paymentResultDto.setTxnRef(vnp_TxnRef);
        paymentResultDto.setAmount(String.valueOf(amount / 100));
        savePaymentToDB(paymentResultDto, orderPayload);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", "NCB");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");

        // Allow an optional returnUrl override (useful for dev testing with ngrok)
        String overrideReturnUrl = req.getParameter("returnUrl");
        String effectiveReturnUrl = ConfigVNPay.vnp_ReturnUrl;
        boolean overrideUsed = false;
        if (ConfigVNPay.allowReturnOverride && overrideReturnUrl != null && !overrideReturnUrl.isEmpty()) {
            // Very basic validation: only accept http/https values
            if (overrideReturnUrl.startsWith("http://") || overrideReturnUrl.startsWith("https://")) {
                effectiveReturnUrl = overrideReturnUrl;
                overrideUsed = true;
                logger.info("VNPay.createPayment: Using override returnUrl: {}", effectiveReturnUrl);
            } else {
                logger.warn("VNPay.createPayment: Ignoring invalid override returnUrl: {}", overrideReturnUrl);
            }
        } else if (overrideReturnUrl != null && !overrideReturnUrl.isEmpty()) {
            // override provided but not allowed
            logger.debug("VNPay.createPayment: override returnUrl provided but disallowed by config: {}",
                    overrideReturnUrl);
        }
        vnp_Params.put("vnp_ReturnUrl", effectiveReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = ConfigVNPay.hmacSHA512(ConfigVNPay.secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = ConfigVNPay.vnp_PayUrl + "?" + queryUrl;

        // Log parameters (don't log secretKey)
        // Mask secure hash in the logged paymentUrl so hashes/secrets are not printed
        // in logs
        String safePaymentUrl = paymentUrl.replaceAll("(?i)(vnp_SecureHash=[^&]+)", "vnp_SecureHash=***");
        logger.info("VNPay createPayment: tmnCode={}, returnUrl={}, paymentUrl={}, overrideUsed={}", vnp_TmnCode,
                effectiveReturnUrl, safePaymentUrl, overrideUsed);
        // Debug log for full params; still doesn't include secretKey, so it's safe but
        // keep it at DEBUG level
        logger.debug("VNPay createPayment params: {}", vnp_Params);

        // If you're seeing the VNPay error "Website này chưa được phê duyệt" (code 71),
        // it usually means the
        // merchant code (vnp_TmnCode) is not approved for sandbox or the return URL is
        // not configured in the merchant portal.
        // We'll return the paymentUrl and an info field that helps debugging in
        // front-end.

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setStatus("OK");
        paymentDto.setMessage("success");
        paymentDto.setUrl(paymentUrl);
        if (vnp_TmnCode == null || vnp_TmnCode.isEmpty()) {
            paymentDto.setInfo("VNPay: vnp_TmnCode not set. Check ConfigVNPay or application.properties.");
        } else {
            paymentDto.setInfo("VNPay: using returnUrl=" + effectiveReturnUrl + (overrideUsed ? " (override)" : ""));
        }

        return ResponseEntity.ok(paymentDto);
    }

    // Replaced duplicate sanitize method with JsonPayloadUtils

    private void savePaymentToDB(PaymentResultDto paymentResultDto, String orderPayload) {
        Payment payment = paymentRepository.findByOrderId(paymentResultDto.getTxnRef());
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(paymentResultDto.getTxnRef());
            payment.setAmount(paymentResultDto.getAmount());
            payment.setOrderStatus("0");
            payment.setStatusExchange(0);
            payment.setOrderPayload(orderPayload);
            paymentRepository.save(payment);
        } else {
            // Update amount in case it differs
            payment.setAmount(paymentResultDto.getAmount());
            if (orderPayload != null && !orderPayload.isEmpty())
                payment.setOrderPayload(orderPayload);
            paymentRepository.save(payment);
        }
    }
}
