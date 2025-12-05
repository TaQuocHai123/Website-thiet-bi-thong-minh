package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.dto.AddressShipping.AddressShippingDto;
import com.project.DuAnTotNghiep.entity.AddressShipping;
import com.project.DuAnTotNghiep.service.AddressShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AddressShippingController {

    private final AddressShippingService addressShippingService;

    public AddressShippingController(AddressShippingService addressShippingService) {
        this.addressShippingService = addressShippingService;
    }

    @PostMapping("api/public/addressShipping")
    public ResponseEntity<AddressShippingDto> createAddressShipping(
            @RequestBody AddressShippingDto addressShippingDto) {
        return ResponseEntity.ok(addressShippingService.saveAddressShippingUser(addressShippingDto));
    }

    @PutMapping("api/public/addressShipping")
    public ResponseEntity<AddressShippingDto> updateAddressShipping(
            @RequestBody AddressShippingDto addressShippingDto) {
        return ResponseEntity.ok(addressShippingService.saveAddressShippingUser(addressShippingDto));
    }

    @DeleteMapping("/api/deleteAddress/{id}")
    public void deleteAddressShipping(@PathVariable Long id) {
        addressShippingService.deleteAddressShipping(id);
    }

    @GetMapping("/api/public/addressShipping/{id}")
    public ResponseEntity<AddressShippingDto> getAddressShipping(@PathVariable Long id) {
        return ResponseEntity.ok(addressShippingService.getAddressShipping(id));
    }
}
