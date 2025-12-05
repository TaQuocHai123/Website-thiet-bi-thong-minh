package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.entity.Payment;
import com.project.DuAnTotNghiep.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentStatusController {

    private final PaymentRepository paymentRepository;

    public PaymentStatusController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @RequestParam(name = "txnRef", required = false) String txnRef,
            @RequestParam(name = "vnp_TxnRef", required = false) String vnpTxnRef) {
        Map<String, Object> response = new HashMap<>();
        String id = (txnRef != null && !txnRef.isEmpty()) ? txnRef : vnpTxnRef;
        if (id == null || id.isEmpty()) {
            response.put("ok", false);
            response.put("message", "txnRef required");
            return ResponseEntity.badRequest().body(response);
        }
        Payment payment = paymentRepository.findByOrderId(id);
        if (payment == null) {
            response.put("ok", true);
            response.put("status", "unknown");
            response.put("message", "Transaction not found");
            return ResponseEntity.ok(response);
        }

        boolean success = "1".equals(payment.getOrderStatus());
        response.put("ok", true);
        response.put("status", success ? "success" : "pending");
        response.put("orderId", payment.getOrderId());
        response.put("amount", payment.getAmount());
        response.put("paymentDate", payment.getPaymentDate());
        return ResponseEntity.ok(response);
    }
}
