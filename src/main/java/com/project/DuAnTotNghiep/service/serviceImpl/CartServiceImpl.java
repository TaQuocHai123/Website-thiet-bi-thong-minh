package com.project.DuAnTotNghiep.service.serviceImpl;

import com.project.DuAnTotNghiep.dto.Cart.CartDto;
import com.project.DuAnTotNghiep.dto.Order.OrderDetailDto;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;
import com.project.DuAnTotNghiep.dto.Product.ProductDetailDto;
import com.project.DuAnTotNghiep.dto.Cart.ProductCart;
import com.project.DuAnTotNghiep.entity.*;
import com.project.DuAnTotNghiep.entity.enumClass.BillStatus;
import com.project.DuAnTotNghiep.entity.enumClass.InvoiceType;
import com.project.DuAnTotNghiep.entity.enumClass.PaymentMethodName;
import com.project.DuAnTotNghiep.exception.NotFoundException;
import com.project.DuAnTotNghiep.exception.ShopApiException;
import com.project.DuAnTotNghiep.repository.*;
import com.project.DuAnTotNghiep.service.CartService;
import com.project.DuAnTotNghiep.utils.RandomUtils;
import com.project.DuAnTotNghiep.utils.UserLoginUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductDiscountRepository productDiscountRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final BillRepository billRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PaymentRepository paymentRepository;
    private final com.project.DuAnTotNghiep.service.ShippingService shippingService;
    private final com.project.DuAnTotNghiep.ghn.GhnService ghnService;
    private final PaymentMethodRepository paymentMethodRepository;
    private AtomicLong invoiceCounter = new AtomicLong(1);

    public CartServiceImpl(CartRepository cartRepository, ProductDiscountRepository productDiscountRepository,
            CustomerRepository customerRepository, AccountRepository accountRepository,
            ProductRepository productRepository, ProductDetailRepository productDetailRepository,
            BillRepository billRepository, DiscountCodeRepository discountCodeRepository,
            PaymentRepository paymentRepository, PaymentMethodRepository paymentMethodRepository,
            com.project.DuAnTotNghiep.service.ShippingService shippingService,
            com.project.DuAnTotNghiep.ghn.GhnService ghnService) {
        this.cartRepository = cartRepository;
        this.productDiscountRepository = productDiscountRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.productDetailRepository = productDetailRepository;
        this.billRepository = billRepository;
        this.discountCodeRepository = discountCodeRepository;
        this.paymentRepository = paymentRepository;
        this.shippingService = shippingService;
        this.ghnService = ghnService;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public List<CartDto> getAllCart() {
        List<Cart> carts = cartRepository.findAll();
        List<CartDto> cartDtos = new ArrayList<>();
        carts.forEach(cart -> {
            CartDto cartDto = new CartDto();
            cartDto.setId(cart.getId());
            cartDto.setQuantity(cart.getQuantity());
            cartDto.setCreateDate(cart.getCreateDate());
        });
        return cartDtos;
    }

    @Override
    public List<CartDto> getAllCartByAccountId() {
        Account account = UserLoginUtil.getCurrentLogin();
        List<Cart> cartList = cartRepository.findAllByAccount_Id(account.getId());
        List<CartDto> cartDtos = new ArrayList<>();
        cartList.forEach(cart -> {
            Product product = productRepository.findById(cart.getProductDetail().getProduct().getId()).orElseThrow();
            ProductCart productCart = new ProductCart();
            productCart.setProductId(product.getId());
            productCart.setName(product.getName());
            productCart.setCode(product.getCode());
            productCart.setDescribe(product.getDescribe());
            productCart.setImageUrl(product.getImage().get(0).getLink());
            ProductDetailDto productDetailDto = new ProductDetailDto();
            productDetailDto.setId(cart.getProductDetail().getId());
            productDetailDto.setProductId(product.getId());
            productDetailDto.setPrice(cart.getProductDetail().getPrice());
            productDetailDto.setSize(cart.getProductDetail().getSize());
            productDetailDto.setQuantity(cart.getProductDetail().getQuantity());
            productDetailDto.setColor(cart.getProductDetail().getColor());

            ProductDiscount productDiscount = productDiscountRepository
                    .findValidDiscountByProductDetailId(cart.getProductDetail().getId());
            if (productDiscount != null) {
                productDetailDto.setDiscountedPrice(productDiscount.getDiscountedAmount());
            }

            CartDto cartDto = new CartDto();
            cartDto.setId(cart.getId());
            cartDto.setQuantity(cart.getQuantity());
            cartDto.setCreateDate(cart.getCreateDate());
            cartDto.setAccountId(Long.parseLong("2"));
            cartDto.setProduct(productCart);
            cartDto.setDetail(productDetailDto);
            cartDtos.add(cartDto);
        });
        return cartDtos;
    }

    @Override
    public void addToCart(CartDto cartDto) throws NotFoundException {
        Cart cart = new Cart();
        Account account = UserLoginUtil.getCurrentLogin();
        cart.setAccount(account);

        ProductDetail productDetail = productDetailRepository.findById(cartDto.getDetail().getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        cart.setProductDetail(productDetail);
        int quantityAdding = cartDto.getQuantity();
        int quantityRemaining = productDetail.getQuantity();

        if (cartRepository.existsByProductDetail_IdAndAccount_Id(productDetail.getId(), account.getId())) {
            Cart existsCart = cartRepository.findByProductDetail_IdAndAccount_Id(productDetail.getId(),
                    account.getId());
            int currentQuantity = existsCart.getQuantity();
            int quantityNeedToAdd = currentQuantity + quantityAdding;

            existsCart.setQuantity(quantityNeedToAdd);
            existsCart.setUpdateDate(LocalDateTime.now());

            if (quantityRemaining == 0) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Sản phẩm có thuộc tính này đã hết hàng");
            }

            if (quantityRemaining < quantityNeedToAdd) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Số lượng thêm vào giỏ hàng lớn hơn số lượng tồn");
            }
            cartRepository.save(existsCart);
        } else {
            if (quantityRemaining < quantityAdding) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Số lượng thêm vào giỏ hàng lớn hơn số lượng tồn");
            }

            cart.setQuantity(quantityAdding);
            cart.setCreateDate(LocalDateTime.now());
            cart.setUpdateDate(LocalDateTime.now());
            cartRepository.save(cart);
        }

    }

    @Override
    public void updateCart(CartDto cartDto) throws NotFoundException {
        Cart cart = cartRepository.findById(cartDto.getId()).orElseThrow(() -> new NotFoundException("Cart not found"));
        int quantityAdding = cartDto.getQuantity();
        int quantityRemaining = cart.getProductDetail().getQuantity();
        if (quantityAdding > quantityRemaining) {
            throw new ShopApiException(HttpStatus.BAD_REQUEST,
                    "Xin lỗi, số lượng sản phẩm này chỉ còn: " + quantityRemaining);
        }
        cart.setQuantity(cartDto.getQuantity());
        cartRepository.save(cart);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public String orderUser(OrderDto orderDto) {
        Bill billCurrent = billRepository.findTopByOrderByIdDesc();
        int nextCode = (billCurrent == null) ? 1 : Integer.parseInt(billCurrent.getCode().substring(2)) + 1;
        String billCode = "HD" + String.format("%03d", nextCode);

        Bill bill = new Bill();
        bill.setBillingAddress(orderDto.getBillingAddress());
        bill.setCreateDate(LocalDateTime.now());
        bill.setUpdateDate(LocalDateTime.now());
        bill.setCode(billCode);
        bill.setInvoiceType(InvoiceType.ONLINE);
        bill.setStatus(BillStatus.CHO_XAC_NHAN);
        bill.setPromotionPrice(orderDto.getPromotionPrice());
        bill.setReturnStatus(false);
        if (UserLoginUtil.getCurrentLogin() != null) {
            Account account = UserLoginUtil.getCurrentLogin();
            bill.setCustomer(account.getCustomer());
        }
        Double total = Double.valueOf(0);
        List<BillDetail> billDetailList = new ArrayList<>();
        for (OrderDetailDto item : orderDto.getOrderDetailDtos()) {
            BillDetail billDetail = new BillDetail();
            billDetail.setBill(bill);
            billDetail.setQuantity(item.getQuantity());
            ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            billDetail.setProductDetail(productDetail);
            Product product = productRepository.findByProductDetail_Id(productDetail.getId());
            if (product.getStatus() == 2) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + productDetail.getProduct().getName() + "-" + productDetail.getSize().getName()
                                + "-" + productDetail.getColor().getName() + " đã ngừng bán");

            }
            if (productDetail.getQuantity() - item.getQuantity() < 0) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + productDetail.getProduct().getName() + "-" + productDetail.getSize().getName()
                                + "-" + productDetail.getColor().getName() + " chỉ còn lại "
                                + productDetail.getQuantity());
            }

            ProductDiscount productDiscount = productDiscountRepository
                    .findValidDiscountByProductDetailId(productDetail.getId());
            if (productDiscount != null) {
                billDetail.setMomentPrice(productDiscount.getDiscountedAmount());
                total += productDiscount.getDiscountedAmount() * item.getQuantity();
            } else {
                billDetail.setMomentPrice(productDetail.getPrice());
                total += productDetail.getPrice() * item.getQuantity();
            }

            productDetail.setQuantity(productDetail.getQuantity() - item.getQuantity());
            productDetailRepository.save(productDetail);
            billDetailList.add(billDetail);

        }

        if (orderDto.getVoucherId() != null) {
            DiscountCode discountCode = discountCodeRepository.findById(orderDto.getVoucherId())
                    .orElseThrow(() -> new ShopApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy voucher"));
            Integer currentQuaCode = discountCode.getMaximumUsage();
            if (currentQuaCode == 0) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết");
            }
            discountCode.setMaximumUsage(currentQuaCode - 1);
            discountCodeRepository.save(discountCode);
            bill.setDiscountCode(discountCode);
        }

        double shippingFee = 0.0;
        // If district+ward provided, try GHN lookup first for server-side validation
        if (orderDto.getShippingDistrictId() != null && orderDto.getShippingWardCode() != null) {
            try {
                java.util.Map<String, Object> ghnResult = ghnService.calculateFee(orderDto.getShippingDistrictId(),
                        orderDto.getShippingWardCode(), 1000);
                if (ghnResult != null && ghnResult.get("fee") != null) {
                    shippingFee = Double.parseDouble(String.valueOf(ghnResult.get("fee")));
                }
            } catch (Exception e) {
                // fallback to coordinate-based if available
                shippingFee = 0.0;
            }
        }
        // If GHN was not used or returned no fee, fallback to coordinate-based
        // calculation
        if (shippingFee == 0.0 && orderDto.getShippingLatitude() != null && orderDto.getShippingLongitude() != null) {
            try {
                shippingFee = shippingService.calculateShippingFee(orderDto.getShippingLatitude(),
                        orderDto.getShippingLongitude());
            } catch (Exception ex) {
                shippingFee = 0.0;
            }
        }
        bill.setShippingFee(shippingFee);
        bill.setShippingLatitude(orderDto.getShippingLatitude());
        bill.setShippingLongitude(orderDto.getShippingLongitude());
        bill.setShippingProvinceId(orderDto.getShippingProvinceId());
        bill.setShippingDistrictId(orderDto.getShippingDistrictId());
        bill.setShippingWardCode(orderDto.getShippingWardCode());
        // If client provided a shipping fee, validate it against server-calculated fee
        // and override when inconsistent
        if (orderDto.getShippingFee() != null) {
            double clientFee = orderDto.getShippingFee();
            if (Math.abs(clientFee - shippingFee) > 1000) { // > 1000 VND mismatch, override
                // log
                org.slf4j.LoggerFactory.getLogger(CartServiceImpl.class).warn(
                        "Client supplied shippingFee={} differs from server {}. Overriding to server value.", clientFee,
                        shippingFee);
                // override already set to shippingFee
            }
        }
        double promotion = orderDto.getPromotionPrice();
        double finalAmount = total + shippingFee - (promotion > 0 ? promotion : 0);
        if (finalAmount < 0)
            finalAmount = 0;
        bill.setAmount(finalAmount);
        bill.setBillDetail(billDetailList);
        PaymentMethod paymentMethod = paymentMethodRepository.findById(orderDto.getPaymentMethodId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        bill.setPaymentMethod(paymentMethod);

        Bill billNew = billRepository.save(bill);

        String resultOrderId = null;
        if (paymentMethod.getName() == PaymentMethodName.TIEN_MAT) {
            Payment payment = new Payment();
            payment.setPaymentDate(LocalDateTime.now());
            payment.setOrderStatus("1");
            payment.setBill(billNew);
            payment.setAmount(total.toString());
            payment.setOrderId(RandomUtils.generateRandomOrderId(8));
            payment.setStatusExchange(0);
            paymentRepository.save(payment);
            resultOrderId = payment.getOrderId();
        }

        if (paymentMethod.getName() == PaymentMethodName.CHUYEN_KHOAN) {
            Payment payment = null;
            if (orderDto.getOrderId() != null) {
                payment = paymentRepository.findByOrderId(orderDto.getOrderId());
            }
            if (payment == null) {
                // Create a placeholder payment and assign the generated or passed orderId
                payment = new Payment();
                String generatedOrderId = (orderDto.getOrderId() != null) ? orderDto.getOrderId()
                        : RandomUtils.generateRandomOrderId(8);
                payment.setOrderId(generatedOrderId);
                payment.setAmount(String.valueOf(billNew.getAmount()));
                payment.setOrderStatus("0");
                payment.setStatusExchange(0);
                paymentRepository.save(payment);
            }
            payment.setBill(billNew);
            payment.setStatusExchange(0);
            paymentRepository.save(payment);
            resultOrderId = payment.getOrderId();
        }

        cartRepository.deleteAllByAccount_Id(UserLoginUtil.getCurrentLogin().getId());
        return resultOrderId;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public String orderUserFromPayload(OrderDto orderDto, Long accountId) {
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CartServiceImpl.class);
        logger.info("orderUserFromPayload invoked for orderId={} accountId={}",
                orderDto != null ? orderDto.getOrderId() : null, accountId);
        try {
            // Avoid duplicate creation if payment already attached to a Bill (idempotent)
            if (orderDto != null && orderDto.getOrderId() != null) {
                Payment existingPayment = paymentRepository.findByOrderId(orderDto.getOrderId());
                if (existingPayment != null && existingPayment.getBill() != null) {
                    return existingPayment.getOrderId();
                }
            }
            Bill billCurrent = billRepository.findTopByOrderByIdDesc();
            int nextCode = (billCurrent == null) ? 1 : Integer.parseInt(billCurrent.getCode().substring(2)) + 1;
            String billCode = "HD" + String.format("%03d", nextCode);

            Bill bill = new Bill();
            bill.setBillingAddress(orderDto.getBillingAddress());
            bill.setCreateDate(LocalDateTime.now());
            bill.setUpdateDate(LocalDateTime.now());
            bill.setCode(billCode);
            bill.setInvoiceType(InvoiceType.ONLINE);
            bill.setStatus(BillStatus.CHO_XAC_NHAN);
            bill.setPromotionPrice(orderDto.getPromotionPrice());
            bill.setReturnStatus(false);
            if (accountId != null) {
                Account account = accountRepository.findById(accountId).orElse(null);
                if (account != null)
                    bill.setCustomer(account.getCustomer());
            }
            Double total = Double.valueOf(0);
            List<BillDetail> billDetailList = new ArrayList<>();
            for (OrderDetailDto item : orderDto.getOrderDetailDtos()) {
                BillDetail billDetail = new BillDetail();
                billDetail.setBill(bill);
                billDetail.setQuantity(item.getQuantity());
                ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId())
                        .orElseThrow(() -> new NotFoundException("Product not found"));
                billDetail.setProductDetail(productDetail);
                Product product = productRepository.findByProductDetail_Id(productDetail.getId());
                if (product.getStatus() == 2) {
                    throw new ShopApiException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm " + productDetail.getProduct().getName() + "-" + productDetail.getSize().getName()
                                    + "-" + productDetail.getColor().getName() + " đã ngừng bán");
                }
                if (productDetail.getQuantity() - item.getQuantity() < 0) {
                    throw new ShopApiException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm " + productDetail.getProduct().getName() + "-" + productDetail.getSize().getName()
                                    + "-" + productDetail.getColor().getName() + " chỉ còn lại "
                                    + productDetail.getQuantity());
                }
                ProductDiscount productDiscount = productDiscountRepository
                        .findValidDiscountByProductDetailId(productDetail.getId());
                if (productDiscount != null) {
                    billDetail.setMomentPrice(productDiscount.getDiscountedAmount());
                    total += productDiscount.getDiscountedAmount() * item.getQuantity();
                } else {
                    billDetail.setMomentPrice(productDetail.getPrice());
                    total += productDetail.getPrice() * item.getQuantity();
                }
                productDetail.setQuantity(productDetail.getQuantity() - item.getQuantity());
                productDetailRepository.save(productDetail);
                billDetailList.add(billDetail);
            }

            if (orderDto.getVoucherId() != null) {
                DiscountCode discountCode = discountCodeRepository.findById(orderDto.getVoucherId())
                        .orElseThrow(() -> new ShopApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy voucher"));
                Integer currentQuaCode = discountCode.getMaximumUsage();
                if (currentQuaCode == 0) {
                    throw new ShopApiException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết");
                }
                discountCode.setMaximumUsage(currentQuaCode - 1);
                discountCodeRepository.save(discountCode);
                bill.setDiscountCode(discountCode);
            }

            double shippingFee2 = 0.0;
            if (orderDto.getShippingLatitude() != null && orderDto.getShippingLongitude() != null) {
                try {
                    shippingFee2 = shippingService.calculateShippingFee(orderDto.getShippingLatitude(),
                            orderDto.getShippingLongitude());
                    bill.setShippingFee(shippingFee2);
                    bill.setShippingLatitude(orderDto.getShippingLatitude());
                    bill.setShippingLongitude(orderDto.getShippingLongitude());
                    bill.setShippingProvinceId(orderDto.getShippingProvinceId());
                    bill.setShippingDistrictId(orderDto.getShippingDistrictId());
                    bill.setShippingWardCode(orderDto.getShippingWardCode());
                } catch (Exception ex) {
                    shippingFee2 = 0.0;
                }
            }
            // Validate client-supplied shipping fee
            if (orderDto.getShippingFee() != null) {
                double clientFee = orderDto.getShippingFee();
                if (Math.abs(clientFee - shippingFee2) > 1000) {
                    org.slf4j.LoggerFactory.getLogger(CartServiceImpl.class).warn(
                            "Client supplied shippingFee={} differs from server {}. Overriding to server value.",
                            clientFee, shippingFee2);
                }
            }
            double promotion2 = orderDto.getPromotionPrice();
            double finalAmount2 = total + shippingFee2 - (promotion2 > 0 ? promotion2 : 0);
            if (finalAmount2 < 0)
                finalAmount2 = 0;
            bill.setAmount(finalAmount2);
            bill.setBillDetail(billDetailList);
            PaymentMethod paymentMethod = paymentMethodRepository.findById(orderDto.getPaymentMethodId())
                    .orElseThrow(() -> new NotFoundException("Payment not found"));
            bill.setPaymentMethod(paymentMethod);
            Bill billNew = billRepository.save(bill);
            logger.info("Created Bill id={}, code={}, amount={}", billNew.getId(), billNew.getCode(),
                    billNew.getAmount());

            if (paymentMethod.getName() == PaymentMethodName.TIEN_MAT) {
                Payment payment = new Payment();
                payment.setPaymentDate(LocalDateTime.now());
                payment.setOrderStatus("1");
                payment.setBill(billNew);
                payment.setAmount(String.valueOf(billNew.getAmount()));
                payment.setOrderId(RandomUtils.generateRandomOrderId(8));
                payment.setStatusExchange(0);
                paymentRepository.save(payment);
            }

            if (paymentMethod.getName() == PaymentMethodName.CHUYEN_KHOAN) {
                // Attach a matching Payment that has this orderId if exists
                Payment payment = paymentRepository.findByOrderId(orderDto.getOrderId());
                if (payment != null) {
                    payment.setBill(billNew);
                    payment.setStatusExchange(0);
                    payment.setAmount(String.valueOf(billNew.getAmount()));
                    paymentRepository.save(payment);
                    logger.info("Attached Payment orderId={} to billId={}", payment.getOrderId(), billNew.getId());
                }
            }

            // If we have an account id, clear the cart for that user
            if (accountId != null) {
                cartRepository.deleteAllByAccount_Id(accountId);
                logger.info("Cleared cart for accountId={}", accountId);
            }

            return (paymentMethod.getName() == PaymentMethodName.TIEN_MAT) ? RandomUtils.generateRandomOrderId(8)
                    : orderDto.getOrderId();
        } catch (Exception e) {
            logger.error("orderUserFromPayload failed for orderId={} accountId={}: {}",
                    orderDto != null ? orderDto.getOrderId() : null, accountId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public OrderDto orderAdmin(OrderDto orderDto) {
        Bill billCurrent = billRepository.findTopByOrderByIdDesc();
        int nextCode = (billCurrent == null) ? 1 : Integer.parseInt(billCurrent.getCode().substring(2)) + 1;
        String billCode = "HD" + String.format("%03d", nextCode);

        Bill bill = new Bill();
        bill.setBillingAddress(orderDto.getBillingAddress());
        bill.setCreateDate(LocalDateTime.now());
        bill.setUpdateDate(LocalDateTime.now());
        bill.setCode(billCode);
        bill.setInvoiceType(InvoiceType.OFFLINE);
        bill.setStatus(BillStatus.HOAN_THANH);
        bill.setPromotionPrice(orderDto.getPromotionPrice());
        bill.setReturnStatus(false);
        Customer customer = null;
        if (orderDto.getCustomer().getId() != null) {
            customer = customerRepository.findById(orderDto.getCustomer().getId())
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
        }
        bill.setCustomer(customer);
        Double total = Double.valueOf(0);
        List<BillDetail> billDetailList = new ArrayList<>();
        for (OrderDetailDto item : orderDto.getOrderDetailDtos()) {
            BillDetail billDetail = new BillDetail();
            billDetail.setBill(bill);
            billDetail.setQuantity(item.getQuantity());
            ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            billDetail.setProductDetail(productDetail);

            ProductDiscount productDiscount = productDiscountRepository
                    .findValidDiscountByProductDetailId(productDetail.getId());

            if (productDetail.getQuantity() - item.getQuantity() < -1) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + productDetail.getProduct().getName() + "-" + productDetail.getSize().getName()
                                + "-" + productDetail.getColor().getName() + " chỉ còn lại "
                                + productDetail.getQuantity());
            }

            if (productDiscount != null) {
                billDetail.setMomentPrice(productDiscount.getDiscountedAmount());
                total += productDiscount.getDiscountedAmount() * item.getQuantity();

            } else {
                billDetail.setMomentPrice(productDetail.getPrice());
                total += productDetail.getPrice() * item.getQuantity();

            }

            int beforeQuantity = productDetail.getQuantity();
            productDetail.setQuantity(beforeQuantity - item.getQuantity());
            productDetailRepository.save(productDetail);
            billDetailList.add(billDetail);
        }

        if (orderDto.getVoucherId() != null) {
            DiscountCode discountCode = discountCodeRepository.findById(orderDto.getVoucherId())
                    .orElseThrow(() -> new ShopApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy voucher"));
            Integer currentQuaCode = discountCode.getMaximumUsage();
            if (currentQuaCode == 0) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết");
            }
            discountCode.setMaximumUsage(currentQuaCode - 1);
            discountCodeRepository.save(discountCode);
            bill.setDiscountCode(discountCode);
        }

        double shippingFee = 0.0;
        if (orderDto.getShippingDistrictId() != null && orderDto.getShippingWardCode() != null) {
            try {
                java.util.Map<String, Object> ghnResult = ghnService.calculateFee(orderDto.getShippingDistrictId(),
                        orderDto.getShippingWardCode(), 1000);
                if (ghnResult != null && ghnResult.get("fee") != null) {
                    shippingFee = Double.parseDouble(String.valueOf(ghnResult.get("fee")));
                }
            } catch (Exception e) {
                shippingFee = 0.0;
            }
        }
        if (shippingFee == 0.0 && orderDto.getShippingLatitude() != null && orderDto.getShippingLongitude() != null) {
            try {
                shippingFee = shippingService.calculateShippingFee(orderDto.getShippingLatitude(),
                        orderDto.getShippingLongitude());
            } catch (Exception ex) {
                shippingFee = 0.0;
            }
        }
        bill.setShippingFee(shippingFee);
        bill.setShippingLatitude(orderDto.getShippingLatitude());
        bill.setShippingLongitude(orderDto.getShippingLongitude());
        bill.setShippingProvinceId(orderDto.getShippingProvinceId());
        bill.setShippingDistrictId(orderDto.getShippingDistrictId());
        bill.setShippingWardCode(orderDto.getShippingWardCode());
        double promotion = orderDto.getPromotionPrice();
        double finalAmount = total + shippingFee - (promotion > 0 ? promotion : 0);
        if (finalAmount < 0)
            finalAmount = 0;
        bill.setAmount(finalAmount);
        bill.setBillDetail(billDetailList);
        PaymentMethod paymentMethod = paymentMethodRepository.findById(orderDto.getPaymentMethodId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        bill.setPaymentMethod(paymentMethod);

        Bill billNew = billRepository.save(bill);

        Payment payment = new Payment();
        payment.setPaymentDate(LocalDateTime.now());
        payment.setOrderStatus("1");
        payment.setBill(billNew);
        payment.setAmount(String.valueOf(billNew.getAmount()));
        payment.setStatusExchange(0);
        payment.setOrderId(RandomUtils.generateRandomOrderId(8));
        paymentRepository.save(payment);

        OrderDto resp = new OrderDto();
        resp.setBillId(billNew.getId().toString());
        resp.setCustomer(orderDto.getCustomer());
        resp.setInvoiceType(billNew.getInvoiceType());
        resp.setBillStatus(billNew.getStatus());
        resp.setPaymentMethodId(billNew.getPaymentMethod().getId());
        resp.setBillingAddress(billNew.getBillingAddress());
        resp.setPromotionPrice(billNew.getPromotionPrice());
        resp.setVoucherId(null);
        resp.setOrderId(null);
        resp.setOrderDetailDtos(java.util.Collections.emptyList());
        resp.setShippingFee(billNew.getShippingFee());
        resp.setShippingLatitude(billNew.getShippingLatitude());
        resp.setShippingLongitude(billNew.getShippingLongitude());
        return resp;
    }

    @Override
    public void deleteCart(Long id) {
        cartRepository.deleteById(id);
    }

    // @Autowired
    // private CartRepository cartRepository;
    // @Override
    // public Page<Cart> carts(Pageable pageable) {
    // return cartRepository.findAll(pageable);
    // }
}
