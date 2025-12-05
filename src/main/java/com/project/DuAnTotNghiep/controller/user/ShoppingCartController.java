package com.project.DuAnTotNghiep.controller.user;

import com.project.DuAnTotNghiep.dto.AddressShipping.AddressShippingDto;
import com.project.DuAnTotNghiep.dto.Cart.CartDto;
import com.project.DuAnTotNghiep.dto.DiscountCode.DiscountCodeDto;
import com.project.DuAnTotNghiep.exception.NotFoundException;
import com.project.DuAnTotNghiep.service.AddressShippingService;
import com.project.DuAnTotNghiep.service.BillService;
import com.project.DuAnTotNghiep.service.CartService;
import com.project.DuAnTotNghiep.service.DiscountCodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import com.project.DuAnTotNghiep.ghn.GhnConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ShoppingCartController {
    private final CartService cartService;

    private final BillService billService;

    private final DiscountCodeService discountCodeService;

    private final AddressShippingService addressShippingService;
    private final GhnConfig ghnConfig;
    @Value("${maps.api.key:}")
    private String mapsApiKey;

    public ShoppingCartController(CartService cartService, BillService billService,
            DiscountCodeService discountCodeService, AddressShippingService addressShippingService,
            GhnConfig ghnConfig) {
        this.cartService = cartService;
        this.billService = billService;
        this.discountCodeService = discountCodeService;
        this.addressShippingService = addressShippingService;
        this.ghnConfig = ghnConfig;
    }

    @GetMapping("/shoping-cart")
    public String viewShoppingCart(Model model) {
        List<CartDto> cartDtoList = cartService.getAllCartByAccountId();
        Page<DiscountCodeDto> discountCodeList = discountCodeService.getAllAvailableDiscountCode(PageRequest.of(0, 15));
        List<AddressShippingDto> addressShippingDtos = addressShippingService.getAddressShippingByAccountId();
        model.addAttribute("discountCodes", discountCodeList.getContent());
        model.addAttribute("addressShippings", addressShippingDtos);
        model.addAttribute("carts", cartDtoList);
        model.addAttribute("mapsApiKey", mapsApiKey);
        model.addAttribute("ghnEnabled", (ghnConfig.getToken() != null && !ghnConfig.getToken().isEmpty()));
        return "user/shoping-cart";
    }

    @ResponseBody
    @PostMapping("/api/addToCart")
    public void addToCart(@RequestBody CartDto cartDto) throws NotFoundException {
        cartService.addToCart(cartDto);
    }

    @ResponseBody
    @PostMapping("/api/deleteCart/{id}")
    public void deleteCart(@PathVariable Long id) {
        cartService.deleteCart(id);
    }

    @ResponseBody
    @PostMapping("/api/updateCart")
    public void updateCart(@RequestBody CartDto cartDto) throws NotFoundException {
        cartService.updateCart(cartDto);
    }

}
