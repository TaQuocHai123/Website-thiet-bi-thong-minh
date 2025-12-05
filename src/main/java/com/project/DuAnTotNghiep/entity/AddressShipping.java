package com.project.DuAnTotNghiep.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.springframework.stereotype.Service;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "AddressShipping")
public class AddressShipping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // private int provinceId;
    // private int districtId;
    // private int wardId;
    //
    //
    // private String street;

    @Nationalized
    @Column(nullable = false, length = 150)
    private String address;

    // Optional coordinates recorded via map selection
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Optionally store province/district/ward ids from GHN (or other shipper) so we
    // can compute GHN fees
    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "district_id")
    private Integer districtId;

    @Column(name = "ward_code")
    private String wardCode;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;
}
