package com.project.DuAnTotNghiep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;
import com.project.DuAnTotNghiep.entity.Payment;
import com.project.DuAnTotNghiep.repository.PaymentRepository;
import com.project.DuAnTotNghiep.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentRetryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryService.class);
    private final PaymentRepository paymentRepository;
    private final CartService cartService;

    public PaymentRetryService(PaymentRepository paymentRepository, CartService cartService) {
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
    }

    // Runs every 10 minutes (600000ms). Adjust as needed.
    @Scheduled(fixedDelay = 600000, initialDelay = 300000)
    public void retryPendingBills() {
        logger.info("PaymentRetryService: scanning for paid payments without a bill");
        List<Payment> pending = paymentRepository.findAllByOrderStatusAndBillIsNull("1");
        if (pending == null || pending.isEmpty()) {
            logger.info("PaymentRetryService: no pending payments found");
            return;
        }
        logger.info("PaymentRetryService: found {} pending payments", pending.size());
        ObjectMapper mapper = new ObjectMapper();
        for (Payment p : pending) {
            try {
                if (p.getOrderPayload() == null || p.getOrderPayload().isEmpty()) {
                    logger.warn("PaymentRetryService: payment {} has no orderPayload, skipping", p.getOrderId());
                    continue;
                }
                java.util.Map<String, Object> payloadMap = mapper.readValue(p.getOrderPayload(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                        });
                com.project.DuAnTotNghiep.utils.JsonPayloadUtils.sanitizeEmptyStringsInMap(payloadMap);
                OrderDto dto = com.project.DuAnTotNghiep.utils.JsonPayloadUtils.convertMapToOrderDto(mapper,
                        payloadMap);
                Long accountId = null;
                Object aId = dto.getAccountId();
                if (aId != null)
                    accountId = dto.getAccountId();
                else if (payloadMap.get("accountId") != null)
                    accountId = Long.parseLong(payloadMap.get("accountId").toString());
                logger.info("PaymentRetryService: trying to create bill from payment orderId={} accountId={}",
                        p.getOrderId(), accountId);
                cartService.orderUserFromPayload(dto, accountId);
                logger.info("PaymentRetryService: success creating bill for payment orderId={}", p.getOrderId());
            } catch (Exception e) {
                logger.error("PaymentRetryService: failed to create bill for payment orderId=" + p.getOrderId(), e);
            }
        }
    }
}
