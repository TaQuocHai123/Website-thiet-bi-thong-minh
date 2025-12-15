package com.project.DuAnTotNghiep.dto.BillReturn;

import lombok.Data;

@Data
public class ReturnCreateDto {
    // id of ProductDetail to return/exchange to
    private Long productDetailId;
    // quantity to return/exchange
    private Integer quantityReturn;
}
