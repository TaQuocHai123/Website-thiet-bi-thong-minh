package com.project.DuAnTotNghiep.dto.BillReturn;

import com.project.DuAnTotNghiep.dto.Product.ProductDetailDto;
import lombok.Data;

import java.util.List;

@Data
public class BillReturnCreateDto {
    private Long billId;
    private String reason;
    private Boolean shipping;
    // percent fee charged for exchanges/refunds (0-100)
    private Integer percent;
    // Items to refund (from bill details)
    private List<RefundCreateDto> refundDtos;
    // Items to return/exchange (new product details)
    private List<ReturnCreateDto> returnDtos;
}
