package com.project.DuAnTotNghiep.config;

import com.project.DuAnTotNghiep.entity.DiscountCode;
import com.project.DuAnTotNghiep.repository.DiscountCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ScheduledApp {
    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // Run every 24 hours
    public void checkAndSetExpiredStatus() {
        Date currentDate = new Date();
        // Find discount codes that are active (status 1) and have endDate before now
        java.util.List<DiscountCode> expiredDiscountCodes = discountCodeRepository.findAllByStatusAndEndDateBefore(1,
                currentDate);
        if (expiredDiscountCodes == null || expiredDiscountCodes.isEmpty()) {
            return;
        }
        for (DiscountCode discountCode : expiredDiscountCodes) {
            if (currentDate.after(discountCode.getEndDate())) {
                discountCode.setStatus(3);
                discountCodeRepository.save(discountCode);
            }
        }
    }
}
