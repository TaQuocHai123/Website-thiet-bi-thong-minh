package com.project.DuAnTotNghiep.service.serviceImpl;

import com.project.DuAnTotNghiep.dto.BillReturn.*;
import com.project.DuAnTotNghiep.dto.CustomerDto.CustomerDto;
import com.project.DuAnTotNghiep.entity.*;
import com.project.DuAnTotNghiep.entity.enumClass.BillStatus;
import com.project.DuAnTotNghiep.exception.NotFoundException;
import com.project.DuAnTotNghiep.exception.ShopApiException;
import com.project.DuAnTotNghiep.repository.*;
import com.project.DuAnTotNghiep.repository.Specification.BillReturnSpecification;
import com.project.DuAnTotNghiep.service.BillReturnService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillReturnServiceImpl implements BillReturnService {

    private static final Logger logger = LoggerFactory.getLogger(BillReturnServiceImpl.class);

    private final BillReturnRepository billReturnRepository;
    private final BillRepository billRepository;
    private final BillDetailRepository billDetailRepository;
    private final ProductDiscountRepository productDiscountRepository;
    private final ProductDetailRepository  productDetailRepository;
    private final ReturnDetailRepository returnDetailRepository;

    public BillReturnServiceImpl(BillReturnRepository billReturnRepository, BillRepository billRepository, BillDetailRepository billDetailRepository, ProductDetailRepository productDetailRepository, ProductDiscountRepository productDiscountRepository, ProductRepository productRepository, CustomerRepository customerRepository, ProductDetailRepository productDetailRepository1, ReturnDetailRepository returnDetailRepository) {
        this.billReturnRepository = billReturnRepository;
        this.billRepository = billRepository;
        this.billDetailRepository = billDetailRepository;
        this.productDiscountRepository = productDiscountRepository;
        this.productDetailRepository = productDetailRepository1;
        this.returnDetailRepository = returnDetailRepository;
    }

    @Override
    public List<BillReturnDto> getAllBillReturns(SearchBillReturnDto searchBillReturnDto) {
        Specification<BillReturn> spec = new BillReturnSpecification(searchBillReturnDto);
        List<BillReturn> billReturns = billReturnRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "returnDate"));
        List<BillReturnDto> billReturnDtos = billReturns.stream().map(this::convertToDto).collect(Collectors.toList());
        return billReturnDtos;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BillReturnDto createBillReturn(BillReturnCreateDto billReturnCreateDto) {
        logger.info("createBillReturn called with billId={} percent={} refundDtos={} returnDtos={}",
            billReturnCreateDto == null ? null : billReturnCreateDto.getBillId(),
            billReturnCreateDto == null ? null : billReturnCreateDto.getPercent(),
            billReturnCreateDto == null || billReturnCreateDto.getRefundDtos() == null ? 0 : billReturnCreateDto.getRefundDtos().size(),
            billReturnCreateDto == null || billReturnCreateDto.getReturnDtos() == null ? 0 : billReturnCreateDto.getReturnDtos().size());

        BillReturn billReturnLast = billReturnRepository.findTopByOrderByIdDesc();
        int nextCode = (billReturnLast == null) ? 1 : Integer.parseInt(billReturnLast.getCode().substring(2)) + 1;
        String billReturnCode = "DT" + String.format("%03d", nextCode);

        BillReturn billReturn = new BillReturn();
        billReturn.setCode(billReturnCode);
        billReturn.setReturnReason(billReturnCreateDto.getReason());
        billReturn.setReturnDate(LocalDateTime.now());
        billReturn.setCancel(false);

        // persist percent fee for exchange/refund if provided
        if (billReturnCreateDto.getPercent() != null) {
            billReturn.setPercentFeeExchange(billReturnCreateDto.getPercent());
        } else {
            billReturn.setPercentFeeExchange(0);
        }

        Bill bill = billRepository.findById(billReturnCreateDto.getBillId()).orElseThrow(() -> new NotFoundException("Bill not found"));
        if(bill.getStatus() == BillStatus.TRA_HANG) {
            throw new ShopApiException(HttpStatus.BAD_REQUEST, "Đơn hàng đã được đổi trả");
        }
        bill.setReturnStatus(true);
        bill.setStatus(BillStatus.TRA_HANG);
        billRepository.save(bill);

        billReturn.setBill(bill);

        // Persist billReturn early so ReturnDetail entries can reference it
        billReturnRepository.save(billReturn);

        // Process refund and return items from DTO
        double totalRefund = 0.0;   // sum of refund items (money to be returned for refunded products)
        double totalExchange = 0.0; // sum of exchange items (money of items customer will receive)

        // Handle refunds (items returned for money)
        if (billReturnCreateDto.getRefundDtos() != null) {
            for (RefundCreateDto r : billReturnCreateDto.getRefundDtos()) {
                if (r == null || r.getBillDetailId() == null || r.getQuantityRefund() == null)
                    continue;
                BillDetail billDetail = billDetailRepository.findById(r.getBillDetailId())
                        .orElseThrow(() -> new NotFoundException("Bill detail not found: " + r.getBillDetailId()));

                int qty = r.getQuantityRefund();
                double price = billDetail.getMomentPrice() != null ? billDetail.getMomentPrice() : 0.0;

                // update return quantity on bill detail
                Integer existingReturnQty = billDetail.getReturnQuantity() == null ? 0 : billDetail.getReturnQuantity();
                billDetail.setReturnQuantity(existingReturnQty + qty);
                billDetailRepository.save(billDetail);

                // create a ReturnDetail record to represent this refunded item
                ReturnDetail rd = new ReturnDetail();
                rd.setBillReturn(billReturn);
                rd.setProductDetail(billDetail.getProductDetail());
                rd.setQuantityReturn(qty);
                rd.setMomentPriceRefund(price);
                returnDetailRepository.save(rd);

                totalRefund += price * qty;
            }
        }

        // Handle exchanges / return-to-other-product (items customer will receive)
        if (billReturnCreateDto.getReturnDtos() != null) {
            for (ReturnCreateDto rt : billReturnCreateDto.getReturnDtos()) {
                if (rt == null || rt.getProductDetailId() == null || rt.getQuantityReturn() == null)
                    continue;
                ProductDetail productDetail = productDetailRepository.findById(rt.getProductDetailId())
                        .orElseThrow(() -> new NotFoundException("Product detail not found: " + rt.getProductDetailId()));

                int qty = rt.getQuantityReturn();
                double price = productDetail.getPrice();

                ReturnDetail rd = new ReturnDetail();
                rd.setBillReturn(billReturn);
                rd.setProductDetail(productDetail);
                rd.setQuantityReturn(qty);
                rd.setMomentPriceRefund(price);
                returnDetailRepository.save(rd);

                totalExchange += price * qty;
            }
        }

        // Calculate fee (percent applied on refund total) and final money to return to customer
        int percent = billReturn.getPercentFeeExchange() == null ? 0 : billReturn.getPercentFeeExchange();
        double fee = Math.round(totalRefund * percent / 100.0);
        double moneyToCustomer = totalRefund - fee - totalExchange;
        if (moneyToCustomer < 0) moneyToCustomer = 0;

        billReturn.setReturnStatus(0); // trạng thái khởi tạo
        billReturn.setReturnMoney(moneyToCustomer);
        // Save again to persist computed money and any relations
        billReturnRepository.save(billReturn);

        return convertToDto(billReturn);
    }

    @Override
    public BillReturnDetailDto getBillReturnDetailById(Long id) {
        BillReturn billReturn = billReturnRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn trả lại id: " + id));
        Bill bill = billReturn.getBill();
        BillReturnDetailDto billReturnDetailDto = new BillReturnDetailDto();
        billReturnDetailDto.setBillId(bill.getId());
        billReturnDetailDto.setBillCode(bill.getCode());
        billReturnDetailDto.setReturnDate(billReturn.getReturnDate());

        if(bill.getCustomer() != null) {
            billReturnDetailDto.setCustomerDto(new CustomerDto(bill.getCustomer().getId(), bill.getCustomer().getCode(), bill.getCustomer().getName(), bill.getCustomer().getPhoneNumber(), null, null));
        }

        List<BillDetail> billDetails = bill.getBillDetail();
        List<RefundProductDto> refundProductDtos = new ArrayList<>();

        // Lấy hàng trả
        for (BillDetail billDetail:
             billDetails) {
           if(billDetail.getReturnQuantity() != null) {
               if(billDetail.getReturnQuantity() > 0 ) {
                   RefundProductDto refundProductDto = new RefundProductDto();
                   ProductDetail productDetail = billDetail.getProductDetail();
                   refundProductDto.setProductName(productDetail.getProduct().getName());
                   refundProductDto.setMomentPriceRefund(billDetail.getMomentPrice());
                   refundProductDto.setProductDetailId(productDetail.getId());
                   refundProductDto.setColor(productDetail.getColor().getName());
                   refundProductDto.setSize(productDetail.getSize().getName());
                   refundProductDto.setQuantityRefund(billDetail.getReturnQuantity());
                   refundProductDtos.add(refundProductDto);
               }
           }
        }

        List<ReturnProductDto> returnProductDtos = new ArrayList<>();
        // Lấy hàng đổi
        for(ReturnDetail returnDetail: billReturn.getReturnDetails()) {
            ReturnProductDto returnProductDto = new ReturnProductDto();
            returnProductDto.setProductDetailId(returnDetail.getProductDetail().getId());
            returnProductDto.setQuantityReturn(returnDetail.getQuantityReturn());
            returnProductDto.setMomentPriceExchange(returnDetail.getProductDetail().getPrice());
            returnProductDto.setProductName(returnDetail.getProductDetail().getProduct().getName());
            returnProductDto.setSize(returnDetail.getProductDetail().getSize().getName());
            returnProductDto.setColor(returnDetail.getProductDetail().getColor().getName());
            returnProductDtos.add(returnProductDto);
        }
        billReturnDetailDto.setReturnProductDtos(returnProductDtos);

        billReturnDetailDto.setRefundProductDtos(refundProductDtos);
        billReturnDetailDto.setBillReturnCode(billReturn.getCode());

        billReturnDetailDto.setId(billReturn.getId());
        billReturnDetailDto.setReturnMoney(billReturn.getReturnMoney());
        billReturnDetailDto.setPercentFeeExchange(billReturn.getPercentFeeExchange());
        billReturnDetailDto.setBillReturnStatus(billReturn.getReturnStatus());
        return billReturnDetailDto;
    }

    @Override
    public BillReturnDetailDto getBillReturnDetailByCode(String code) {
        BillReturn billReturn = billReturnRepository.findByCode(code).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn trả lại id: " + code));
        Bill bill = billReturn.getBill();
        BillReturnDetailDto billReturnDetailDto = new BillReturnDetailDto();
        billReturnDetailDto.setBillId(bill.getId());
        billReturnDetailDto.setBillCode(bill.getCode());
        billReturnDetailDto.setReturnDate(billReturn.getReturnDate());

        if(bill.getCustomer() != null) {
            billReturnDetailDto.setCustomerDto(new CustomerDto(bill.getCustomer().getId(), bill.getCustomer().getCode(), bill.getCustomer().getName(), bill.getCustomer().getPhoneNumber(), null, null));
        }

        List<BillDetail> billDetails = bill.getBillDetail();
        List<RefundProductDto> refundProductDtos = new ArrayList<>();

        // Lấy hàng trả
        for (BillDetail billDetail:
                billDetails) {
            if(billDetail.getReturnQuantity() != null) {
                if(billDetail.getReturnQuantity() > 0 ) {
                    RefundProductDto refundProductDto = new RefundProductDto();
                    ProductDetail productDetail = billDetail.getProductDetail();
                    refundProductDto.setProductName(productDetail.getProduct().getName());
                    refundProductDto.setMomentPriceRefund(billDetail.getMomentPrice());
                    refundProductDto.setProductDetailId(productDetail.getId());
                    refundProductDto.setColor(productDetail.getColor().getName());
                    refundProductDto.setSize(productDetail.getSize().getName());
                    refundProductDto.setQuantityRefund(billDetail.getReturnQuantity());
                    refundProductDtos.add(refundProductDto);
                }
            }
        }

        List<ReturnProductDto> returnProductDtos = new ArrayList<>();
        // Lấy hàng đổi
        for(ReturnDetail returnDetail: billReturn.getReturnDetails()) {
            ReturnProductDto returnProductDto = new ReturnProductDto();
            returnProductDto.setProductDetailId(returnDetail.getProductDetail().getId());
            returnProductDto.setQuantityReturn(returnDetail.getQuantityReturn());
            returnProductDto.setMomentPriceExchange(returnDetail.getProductDetail().getPrice());
            returnProductDto.setProductName(returnDetail.getProductDetail().getProduct().getName());
            returnProductDto.setSize(returnDetail.getProductDetail().getSize().getName());
            returnProductDto.setColor(returnDetail.getProductDetail().getColor().getName());
            returnProductDtos.add(returnProductDto);
        }
        billReturnDetailDto.setReturnProductDtos(returnProductDtos);

        billReturnDetailDto.setRefundProductDtos(refundProductDtos);
        billReturnDetailDto.setBillReturnCode(billReturn.getCode());

        billReturnDetailDto.setId(billReturn.getId());
        billReturnDetailDto.setReturnMoney(billReturn.getReturnMoney());
        billReturnDetailDto.setPercentFeeExchange(billReturn.getPercentFeeExchange());
        billReturnDetailDto.setBillReturnStatus(billReturn.getReturnStatus());
        return billReturnDetailDto;
    }

    @Override
    public String generateHtmlContent(Long billReturnId) {
        BillReturnDetailDto billReturnDetailDto = getBillReturnDetailById(billReturnId);
        return null;
    }

    @Override
    public BillReturnDto updateStatus(Long id, int returnStatus) {
        BillReturn billReturn = billReturnRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn trả lại id: " + id));
        billReturn.setReturnStatus(returnStatus);
        if(returnStatus == 4) {
            for (ReturnDetail returnDetail:
                    billReturn.getReturnDetails()) {
                ProductDetail productDetail = returnDetail.getProductDetail();
                int quantityReturn = returnDetail.getQuantityReturn();
                int beforeQuantity = productDetail.getQuantity();
                productDetail.setQuantity(beforeQuantity + quantityReturn);
                productDetailRepository.save(productDetail);
            }
        }
        return convertToDto(billReturnRepository.save(billReturn));
    }

    private BillReturnDto convertToDto(BillReturn billReturn) {
        BillReturnDto billReturnDto = new BillReturnDto();
        billReturnDto.setId(billReturn.getId());
        billReturnDto.setCode(billReturn.getCode());
        billReturnDto.setReturnDate(billReturn.getReturnDate());
        billReturnDto.setReturnReason(billReturn.getReturnReason());
        billReturnDto.setCancel(billReturn.isCancel());
        billReturnDto.setReturnMoney(billReturn.getReturnMoney());
        billReturnDto.setReturnStatus(billReturn.getReturnStatus());
        if(billReturn.getBill().getCustomer() != null) {
            Customer customer = billReturn.getBill().getCustomer();
            CustomerDto customerDto = new CustomerDto(customer.getId(), customer.getCode(), customer.getName(), customer.getPhoneNumber(), customer.getEmail(), null);
            billReturnDto.setCustomer(customerDto);
        }
        return billReturnDto;
    }

}
