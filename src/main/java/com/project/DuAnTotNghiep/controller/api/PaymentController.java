package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.config.ConfigVNPay;
import com.project.DuAnTotNghiep.dto.Payment.PaymentResultDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;
import com.project.DuAnTotNghiep.service.CartService;
import com.project.DuAnTotNghiep.repository.AccountRepository;
import com.project.DuAnTotNghiep.entity.Account;
import com.project.DuAnTotNghiep.entity.Payment;
import com.project.DuAnTotNghiep.repository.PaymentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    private final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final AccountRepository accountRepository;

    public PaymentController(PaymentRepository paymentRepository, CartService cartService,
            AccountRepository accountRepository) {
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/payment-result")
    public String viewPaymentResult(HttpServletRequest request, Model model) throws UnsupportedEncodingException {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
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
        logger.debug("VNPay computed signature: {} for data: {}", signValue, hashData.toString());
        logger.debug("VNPay received vnp_SecureHash: {}", vnp_SecureHash);
        PaymentResultDto paymentResultDto = new PaymentResultDto();
        String vnp_TxnRef = fields.get("vnp_TxnRef") != null ? fields.get("vnp_TxnRef").toString() : null;
        String vnp_Amount = fields.get("vnp_Amount") != null ? fields.get("vnp_Amount").toString() : "0";
        String vnp_BankCode = fields.get("vnp_BankCode") != null ? fields.get("vnp_BankCode").toString() : "";
        String vnp_PayDate = fields.get("vnp_PayDate") != null ? fields.get("vnp_PayDate").toString() : "";
        String vnp_ResponseCode = fields.get("vnp_ResponseCode") != null ? fields.get("vnp_ResponseCode").toString()
                : "";
        String vnp_TransactionStatus = fields.get("vnp_TransactionStatus") != null
                ? fields.get("vnp_TransactionStatus").toString()
                : "";
        paymentResultDto.setTxnRef(vnp_TxnRef);
        paymentResultDto.setAmount(String.valueOf(Double.parseDouble(vnp_Amount) / 100));
        paymentResultDto.setBankCode(vnp_BankCode);
        paymentResultDto.setDatePay(vnp_PayDate);
        paymentResultDto.setResponseCode(vnp_ResponseCode);
        paymentResultDto.setTransactionStatus(vnp_TransactionStatus);

        model.addAttribute("result", paymentResultDto);
        model.addAttribute("vnpResponseCode", paymentResultDto.getResponseCode());
        model.addAttribute("vnpSupportEmail", "hotrovnpay@vnpay.vn");
        model.addAttribute("vnpFriendlyMessage", "");

        if (signValue.equals(vnp_SecureHash)) {
            // Handle VNPay 'Website not approved' code 71 with a friendly message
            if ("71".equals(paymentResultDto.getResponseCode())) {
                model.addAttribute("status", "Giao dịch không thành công: Website chưa được VNPay phê duyệt (mã 71).");
                model.addAttribute("paymentSuccess", false);
                model.addAttribute("vnpFriendlyMessage",
                        "Vui lòng liên hệ đội hỗ trợ của VNPay (hotrovnpay@vnpay.vn) hoặc kiểm tra cấu hình Merchant (TMN) và Return URL. Nếu đang test local, hãy sử dụng ngrok và đăng ký URL trong cổng VNPay.");
                return "user/payment-result";
            }
            boolean checkOrderId = paymentRepository.existsByOrderId(paymentResultDto.getTxnRef()); // Giá trị của
                                                                                                    // vnp_TxnRef tồn
                                                                                                    // tại trong CSDL
                                                                                                    // của merchant
            boolean checkAmount = false; // De kiem tra amount
            boolean checkOrderStatus = false; // De kiem tra status co phai la da thanh toan hay khong
            if (checkOrderId) {
                Payment payment = paymentRepository.findByOrderId(paymentResultDto.getTxnRef());
                checkAmount = Double.parseDouble(paymentResultDto.getAmount()) == Double
                        .parseDouble(payment.getAmount());
                checkOrderStatus = payment.getOrderStatus().equals("0");
                if (checkAmount) {
                    if (checkOrderStatus) {
                        if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {
                            // Payment success: update status and create Bill if needed.
                            model.addAttribute("status", "Giao dịch thành công");
                            model.addAttribute("paymentSuccess", true);
                            model.addAttribute("orderId", paymentResultDto.getTxnRef());
                            updatePaymentStatus(payment);
                            // If the payment doesn't have an attached Bill but we have orderPayload saved,
                            // create the Bill
                            if (payment.getBill() == null && payment.getOrderPayload() != null
                                    && !payment.getOrderPayload().isEmpty()) {
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    java.util.Map<String, Object> payloadMap = mapper.readValue(
                                            payment.getOrderPayload(),
                                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                                            });
                                    com.project.DuAnTotNghiep.utils.JsonPayloadUtils
                                            .sanitizeEmptyStringsInMap(payloadMap);
                                    OrderDto payloadOrder = com.project.DuAnTotNghiep.utils.JsonPayloadUtils
                                            .convertMapToOrderDto(mapper, payloadMap);
                                    // Ensure payloadOrder contains orderId so orderUserFromPayload can attach
                                    // Payment
                                    payloadOrder.setOrderId(payment.getOrderId());
                                    Long accountId = null;
                                    try {
                                        if (payloadOrder.getAccountId() != null) {
                                            accountId = payloadOrder.getAccountId();
                                        } else if (payloadMap.get("accountId") != null) {
                                            accountId = Long.parseLong(payloadMap.get("accountId").toString());
                                        } else if (payloadOrder.getCustomer() != null
                                                && payloadOrder.getCustomer().getId() != null) {
                                            Long custId = payloadOrder.getCustomer().getId();
                                            Account account = accountRepository.findByCustomer_Id(custId);
                                            if (account != null)
                                                accountId = account.getId();
                                        }
                                    } catch (Exception ignored) {
                                    }
                                    logger.info("Payment redirect creating bill for txnRef={} orderId={} accountId={}",
                                            vnp_TxnRef, payloadOrder.getOrderId(), accountId);
                                    cartService.orderUserFromPayload(payloadOrder, accountId);
                                    logger.info("Created bill for txnRef={} orderId={}", vnp_TxnRef,
                                            payloadOrder.getOrderId());
                                } catch (Exception e) {
                                    logger.error("Failed to create order from saved payment payload", e);
                                }
                            }
                            // Render the local payment-result page (don't redirect to ngrok). Payment
                            // recorded and Bill created above.
                            return "user/payment-result";
                        } else {
                            model.addAttribute("status", "GD Không thành công");
                            model.addAttribute("vnpFriendlyMessage",
                                    "Giao dịch không thành công. Vui lòng thử lại hoặc liên hệ bộ phận hỗ trợ.");
                            model.addAttribute("paymentSuccess", false);
                        }
                    } else {
                        model.addAttribute("status", "Đơn hàng đã được thanh toán");
                        model.addAttribute("vnpFriendlyMessage",
                                "Đơn hàng này đã được ghi nhận là đã thanh toán. Nếu bạn tin rằng có lỗi, vui lòng liên hệ bộ phận hỗ trợ.");
                        model.addAttribute("paymentSuccess", false);
                    }
                } else {
                    model.addAttribute("status", "Số tiền không khớp");
                    model.addAttribute("vnpFriendlyMessage",
                            "Số tiền báo cáo từ VNPay không khớp với đơn hàng. Vui lòng liên hệ hỗ trợ.");
                    model.addAttribute("paymentSuccess", false);

                }
            } else {
                model.addAttribute("status", "Mã giao dịch không tồn tại");
                model.addAttribute("vnpFriendlyMessage",
                        "Giao dịch không tồn tại trong hệ thống. Nếu bạn đã thanh toán, vui lòng liên hệ bộ phận hỗ trợ với mã giao dịch.");
                model.addAttribute("paymentSuccess", false);

            }
        } else {
            model.addAttribute("status", "Invalid checksum");
            model.addAttribute("paymentSuccess", false);
            model.addAttribute("vnpFriendlyMessage",
                    "Chữ ký xác thực không hợp lệ (checksum). Hãy liên hệ bộ phận kỹ thuật của cửa hàng để kiểm tra vnp_TmnCode và secret key.");

        }
        return "user/payment-result";
    }

    private void updatePaymentStatus(Payment payment) {
        payment.setOrderStatus("1");
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}
