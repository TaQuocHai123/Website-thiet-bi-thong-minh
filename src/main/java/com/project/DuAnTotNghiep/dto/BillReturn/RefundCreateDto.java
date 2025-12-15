package com.project.DuAnTotNghiep.dto.BillReturn;

import lombok.Data;

@Data
public class RefundCreateDto {
    // id of BillDetail being refunded
    private Long billDetailId;
    // quantity to refund
    private Integer quantityRefund;
}
