package com.project.DuAnTotNghiep.dto.Order;

import com.project.DuAnTotNghiep.dto.CustomerDto.CustomerDto;
import com.project.DuAnTotNghiep.entity.enumClass.BillStatus;
import com.project.DuAnTotNghiep.entity.enumClass.InvoiceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDto {
    private String billId;
    private CustomerDto customer;
    private InvoiceType invoiceType;
    private BillStatus billStatus;
    private Long paymentMethodId;
    private String billingAddress;
    private double promotionPrice;
    private Long voucherId;
    private Long accountId;
    private String orderId;
    private List<OrderDetailDto> orderDetailDtos;
    private Double shippingLatitude;
    private Double shippingLongitude;
    private Double shippingFee;
    private Integer shippingProvinceId;
    private Integer shippingDistrictId;
    private String shippingWardCode;
}
