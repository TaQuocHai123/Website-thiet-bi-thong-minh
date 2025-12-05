package com.project.DuAnTotNghiep.dto.AddressShipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressShippingDto {
    private Long id;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
}
