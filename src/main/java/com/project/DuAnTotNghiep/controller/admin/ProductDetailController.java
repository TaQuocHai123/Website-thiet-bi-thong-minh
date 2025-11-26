package com.project.DuAnTotNghiep.controller.admin;

import com.project.DuAnTotNghiep.entity.Color;
import com.project.DuAnTotNghiep.entity.Image;
import com.project.DuAnTotNghiep.entity.Product;
import com.project.DuAnTotNghiep.entity.Size;
import com.project.DuAnTotNghiep.service.ColorService;
import com.project.DuAnTotNghiep.service.ImageService;
import com.project.DuAnTotNghiep.service.ProductDetailService;
import com.project.DuAnTotNghiep.service.ProductService;
import com.project.DuAnTotNghiep.service.SizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ProductDetailController {

    private Product productInLine;
    private final List<Image> imageList = new ArrayList<>();
    private long idImage;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SizeService sizeService;
    @Autowired
    private ColorService colorService;

    @Autowired
    private ImageService imageService;

    @GetMapping("/admin/chi-tiet-san-pham/{id}")
    public String getProductDetailPage(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id).orElse(null);
        if (product != null) {
            model.addAttribute("product", product);
            model.addAttribute("productDetails", product.getProductDetails());
            return "admin/product-detail";
        }

        return "error/404";
    }

    // Backwards compatibility: allow code-based lookups but redirect to ID-based
    // admin route
    @GetMapping("/admin/chi-tiet-san-pham/code/{code}")
    public String redirectProductDetailByCode(@PathVariable String code) {
        Product product = productService.getProductByCode(code);
        if (product == null) {
            return "error/404";
        }
        return "redirect:/admin/chi-tiet-san-pham/" + product.getId();
    }

    @ModelAttribute("listSize")
    public List<Size> getSize() {
        return sizeService.getAll();
    }

    @ModelAttribute("listColor")
    public List<Color> getColor() {
        return colorService.findAll();
    }
}
