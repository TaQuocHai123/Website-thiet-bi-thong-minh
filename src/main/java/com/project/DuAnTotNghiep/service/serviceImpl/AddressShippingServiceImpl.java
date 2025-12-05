package com.project.DuAnTotNghiep.service.serviceImpl;

import com.project.DuAnTotNghiep.dto.AddressShipping.AddressShippingDto;
import com.project.DuAnTotNghiep.dto.AddressShipping.AddressShippingDtoAdmin;
import com.project.DuAnTotNghiep.entity.Account;
import com.project.DuAnTotNghiep.entity.AddressShipping;
import com.project.DuAnTotNghiep.entity.Customer;
import com.project.DuAnTotNghiep.exception.NotFoundException;
import com.project.DuAnTotNghiep.exception.ShopApiException;
import com.project.DuAnTotNghiep.repository.AddressShippingRepository;
import com.project.DuAnTotNghiep.repository.CustomerRepository;
import com.project.DuAnTotNghiep.security.CustomUserDetails;
import com.project.DuAnTotNghiep.service.AddressShippingService;
import com.project.DuAnTotNghiep.service.GeocodeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AddressShippingServiceImpl implements AddressShippingService {

    private final AddressShippingRepository addressShippingRepository;
    private final CustomerRepository customerRepository;
    private final GeocodeService geocodeService;

    public AddressShippingServiceImpl(AddressShippingRepository addressShippingRepository,
            CustomerRepository customerRepository,
            GeocodeService geocodeService) {
        this.addressShippingRepository = addressShippingRepository;
        this.customerRepository = customerRepository;
        this.geocodeService = geocodeService;
    }

    @Override
    public List<AddressShippingDto> getAddressShippingByAccountId() {
        List<AddressShipping> addressShippings = addressShippingRepository
                .findAllByCustomer_Account_Id(getCurrentLogin().getId());
        List<AddressShippingDto> addressShippingDtos = new ArrayList<>();
        addressShippings.forEach(item -> {
            AddressShippingDto addressShippingDto = new AddressShippingDto();
            addressShippingDto.setId(item.getId());
            addressShippingDto.setAddress(item.getAddress());
            addressShippingDto.setLatitude(item.getLatitude());
            addressShippingDto.setLongitude(item.getLongitude());
            addressShippingDto.setProvinceId(item.getProvinceId());
            addressShippingDto.setDistrictId(item.getDistrictId());
            addressShippingDto.setWardCode(item.getWardCode());
            addressShippingDtos.add(addressShippingDto);
        });
        return addressShippingDtos;
    }

    @Override
    public AddressShippingDto saveAddressShippingUser(AddressShippingDto addressShippingDto) {
        List<AddressShipping> addressShippings = addressShippingRepository
                .findAllByCustomer_Account_Id(getCurrentLogin().getId());
        // limit to 5 addresses per user (only on create)
        if (addressShippingDto.getId() == null && addressShippings.size() > 5) {
            throw new ShopApiException(HttpStatus.BAD_REQUEST, "Bạn chỉ được thêm tối đa 5 địa chỉ");
        }
        AddressShipping addressShipping = new AddressShipping();
        // if id present, update existing
        if (addressShippingDto.getId() != null) {
            addressShipping = addressShippingRepository.findById(addressShippingDto.getId())
                    .orElseThrow(() -> new NotFoundException("Address not found"));
            // Ensure this belongs to current user
            Account current = getCurrentLogin();
            if (current == null || addressShipping.getCustomer() == null
                    || !current.getId().equals(addressShipping.getCustomer().getAccount().getId())) {
                throw new ShopApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa địa chỉ này");
            }
        }
        addressShipping.setAddress(addressShippingDto.getAddress());
        addressShipping.setProvinceId(addressShippingDto.getProvinceId());
        addressShipping.setDistrictId(addressShippingDto.getDistrictId());
        addressShipping.setWardCode(addressShippingDto.getWardCode());

        // If lat/lng not provided, auto-generate from districtId+wardCode
        if ((addressShippingDto.getLatitude() == null || addressShippingDto.getLatitude() == 0) &&
                addressShippingDto.getDistrictId() != null) {
            Map<String, Double> coords = geocodeService.getCoordinatesForDistrictWard(
                    addressShippingDto.getDistrictId(),
                    addressShippingDto.getWardCode());
            addressShipping.setLatitude(coords.get("latitude"));
            addressShipping.setLongitude(coords.get("longitude"));
        } else {
            addressShipping.setLatitude(addressShippingDto.getLatitude());
            addressShipping.setLongitude(addressShippingDto.getLongitude());
        }

        Customer customer = new Customer();
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            customer = getCurrentLogin().getCustomer();
            addressShipping.setCustomer(customer);
        }

        AddressShipping addressShippingNew = addressShippingRepository.save(addressShipping);
        AddressShippingDto res = new AddressShippingDto();
        res.setId(addressShippingNew.getId());
        res.setAddress(addressShippingNew.getAddress());
        res.setLatitude(addressShippingNew.getLatitude());
        res.setLongitude(addressShippingNew.getLongitude());
        res.setProvinceId(addressShippingNew.getProvinceId());
        res.setDistrictId(addressShippingNew.getDistrictId());
        res.setWardCode(addressShippingNew.getWardCode());
        return res;
    }

    @Override
    public AddressShippingDto saveAddressShippingAdmin(AddressShippingDtoAdmin addressShippingDto) {
        AddressShipping addressShipping = new AddressShipping();
        addressShipping.setAddress(addressShippingDto.getAddress());
        addressShipping.setLatitude(addressShippingDto.getLatitude());
        addressShipping.setLongitude(addressShippingDto.getLongitude());
        addressShipping.setProvinceId(addressShippingDto.getProvinceId());
        addressShipping.setDistrictId(addressShippingDto.getDistrictId());
        addressShipping.setWardCode(addressShippingDto.getWardCode());
        Customer customer = customerRepository.findById(addressShippingDto.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        addressShipping.setCustomer(customer);

        AddressShipping addressShippingNew = addressShippingRepository.save(addressShipping);
        AddressShippingDto res = new AddressShippingDto();
        res.setId(addressShippingNew.getId());
        res.setAddress(addressShippingNew.getAddress());
        res.setLatitude(addressShippingNew.getLatitude());
        res.setLongitude(addressShippingNew.getLongitude());
        res.setProvinceId(addressShippingNew.getProvinceId());
        res.setDistrictId(addressShippingNew.getDistrictId());
        res.setWardCode(addressShippingNew.getWardCode());
        return res;
    }

    @Override
    public void deleteAddressShipping(Long id) {
        AddressShipping addressShipping = addressShippingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        addressShippingRepository.delete(addressShipping);
    }

    @Override
    public AddressShippingDto getAddressShipping(Long id) {
        AddressShipping addressShipping = addressShippingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        AddressShippingDto dto = new AddressShippingDto();
        dto.setId(addressShipping.getId());
        dto.setAddress(addressShipping.getAddress());
        dto.setLatitude(addressShipping.getLatitude());
        dto.setLongitude(addressShipping.getLongitude());
        dto.setProvinceId(addressShipping.getProvinceId());
        dto.setDistrictId(addressShipping.getDistrictId());
        dto.setWardCode(addressShipping.getWardCode());
        return dto;
    }

    private Account getCurrentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            return customUserDetails.getAccount();
        }

        // Handle the case where the principal is not a CustomUserDetails
        return null; // or throw an exception, depending on your use case
    }

}
