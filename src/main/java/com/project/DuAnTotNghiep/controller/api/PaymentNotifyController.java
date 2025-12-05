package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.config.ConfigVNPay;
import com.project.DuAnTotNghiep.entity.Payment;
import com.project.DuAnTotNghiep.repository.PaymentRepository;
import com.project.DuAnTotNghiep.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;
import com.project.DuAnTotNghiep.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import com.project.DuAnTotNghiep.entity.Account;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentNotifyController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentNotifyController.class);

    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final AccountRepository accountRepository;

    public PaymentNotifyController(PaymentRepository paymentRepository, CartService cartService,
            AccountRepository accountRepository) {
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notifyPayment(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();

        // Collect all parameters; VNPay may send as form params
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            fields.put(name, value);
        }

        String vnp_SecureHash = fields.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            logger.warn("VNPay notify missing vnp_SecureHash");
            return ResponseEntity.badRequest().body("Missing vnp_SecureHash");
        }

        // Remove secure hash fields before computing
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // Build hash data using the same URL encoding rules as the createPayment flow
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    hashData.append(fieldName).append('=');
                    hashData.append(java.net.URLEncoder.encode(fieldValue,
                            java.nio.charset.StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    hashData.append(fieldName).append('=');
                    hashData.append(fieldValue);
                }
            }
            if (itr.hasNext()) {
                hashData.append('&');
            }
        }
        String signValue = ConfigVNPay.hmacSHA512(ConfigVNPay.secretKey, hashData.toString());
        logger.debug("VNPay computed signature (notify): {} for data: {}", signValue, hashData.toString());
        logger.debug("VNPay notify received vnp_SecureHash: {}", vnp_SecureHash);
        if (!signValue.equals(vnp_SecureHash)) {
            logger.warn("VNPay notify invalid secure hash for vnp_TxnRef={}", fields.get("vnp_TxnRef"));
            return ResponseEntity.ok("INVALID_HASH");
        }

        // Check status and update payment
        String txnRef = fields.get("vnp_TxnRef");
        String responseCode = fields.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = fields.getOrDefault("vnp_TransactionStatus", "");
        String amountStr = fields.getOrDefault("vnp_Amount", "0");

        logger.info("VNPay notify received txnRef={}, responseCode={}, status={}", txnRef, responseCode,
                transactionStatus);

        if (txnRef == null) {
            return ResponseEntity.badRequest().body("Missing txnRef");
        }

        Payment payment = paymentRepository.findByOrderId(txnRef);
        if (payment == null) {
            logger.warn("VNPay notify: transaction not found {}", txnRef);
            return ResponseEntity.ok("NOT_FOUND");
        }

        // VNPay response codes: 00 -> success
        if ("00".equals(responseCode) || "00".equals(transactionStatus)) {
            // Check amount matches
            String dbAmountStr = payment.getAmount();
            try {
                // In many implementations amount is in VND * 100 in VNPay, so adjust if
                // necessary
                double vnPayAmount = Double.parseDouble(amountStr) / 100.0;
                double dbAmount = Double.parseDouble(dbAmountStr);
                if (Math.abs(vnPayAmount - dbAmount) < 0.0001) {
                    payment.setOrderStatus("1");
                    payment.setPaymentDate(LocalDateTime.now());
                    paymentRepository.save(payment);
                    // If no bill attached yet and we have the order payload, attempt to create the
                    // Bill
                    if (payment.getBill() == null && payment.getOrderPayload() != null
                            && !payment.getOrderPayload().isEmpty()) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            java.util.Map<String, Object> payloadMap = mapper.readValue(payment.getOrderPayload(),
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                                    });
                            com.project.DuAnTotNghiep.utils.JsonPayloadUtils.sanitizeEmptyStringsInMap(payloadMap);
                            OrderDto payloadOrder = com.project.DuAnTotNghiep.utils.JsonPayloadUtils
                                    .convertMapToOrderDto(mapper, payloadMap);
                            payloadOrder.setOrderId(payment.getOrderId());
                            Long accountId = null;
                            try {
                                if (payloadOrder.getAccountId() != null) {
                                    accountId = payloadOrder.getAccountId();
                                } else if (payloadMap.get("accountId") != null) {
                                    accountId = Long.parseLong(payloadMap.get("accountId").toString());
                                } else if (payloadOrder.getCustomer() != null
                                        && payloadOrder.getCustomer().getId() != null) {
                                    Account account = accountRepository
                                            .findByCustomer_Id(payloadOrder.getCustomer().getId());
                                    if (account != null)
                                        accountId = account.getId();
                                }
                            } catch (Exception ignored) {
                            }
                            logger.info("Payment notify creating bill for txnRef={} orderId={} accountId={}", txnRef,
                                    payloadOrder.getOrderId(), accountId);
                            cartService.orderUserFromPayload(payloadOrder, accountId);
                            logger.info("Created bill from notify for txnRef={} orderId={}", txnRef,
                                    payloadOrder.getOrderId());
                        } catch (Exception e) {
                            logger.error("Failed to create order from saved payment payload on notify", e);
                        }
                    }
                    logger.info("VNPay notify: updated payment status to paid for txnRef={}", txnRef);
                    return ResponseEntity.ok("OK");
                } else {
                    logger.warn("VNPay notify amount mismatch for txnRef={} (vnpay: {}, db: {})", txnRef, vnPayAmount,
                            dbAmount);
                    return ResponseEntity.ok("AMOUNT_MISMATCH");
                }
            } catch (NumberFormatException e) {
                logger.error("VNPay notify parse amount failed: {}", amountStr);
                return ResponseEntity.ok("INVALID_AMOUNT");
            }
        }

        // Not successful – still acknowledge
        logger.info("VNPay notify: not successful vnp_ResponseCode={} txnRef={}", responseCode, txnRef);
        return ResponseEntity.ok("NOT_SUCCESS");
    }
}
