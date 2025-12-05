package com.project.DuAnTotNghiep.repository;

import com.project.DuAnTotNghiep.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderId(String orderId);

    Payment findByOrderId(String orderId);

    java.util.List<Payment> findAllByOrderStatusAndBillIsNull(String orderStatus);

}
