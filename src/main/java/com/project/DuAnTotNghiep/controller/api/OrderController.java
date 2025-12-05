package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.dto.CheckOrderDto;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;
import com.project.DuAnTotNghiep.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class OrderController {
    private final CartService cartService;

    public OrderController(CartService cartService) {
        this.cartService = cartService;
    }

    @ResponseBody
    @PostMapping("/api/orderUser")
    public java.util.Map<String, String> order(@RequestBody OrderDto orderDto) {
        String orderId = cartService.orderUser(orderDto);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("orderId", orderId == null ? "" : orderId);
        return response;
    }

    @ResponseBody
    @PostMapping("/api/orderAdmin")
    public OrderDto orderAdmin(@RequestBody OrderDto orderDto) {
        return cartService.orderAdmin(orderDto);
    }

    @ResponseBody
    @PostMapping("/api/checkOrder")
    public List<CheckOrderDto> checkOrder(@RequestBody List<CheckOrderDto> checkOrderDtoList) {
        return null;
    }
}
