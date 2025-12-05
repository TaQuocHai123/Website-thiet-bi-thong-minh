package com.project.DuAnTotNghiep.controller.user;

import com.project.DuAnTotNghiep.dto.Account.AccountDto;
import com.project.DuAnTotNghiep.dto.Account.ChangePasswordDto;
import com.project.DuAnTotNghiep.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import com.project.DuAnTotNghiep.ghn.GhnConfig;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserProfileController {
    private final AccountService accountService;
    private final GhnConfig ghnConfig;
    @Value("${maps.api.key:}")
    private String mapsApiKey;

    public UserProfileController(AccountService accountService, GhnConfig ghnConfig) {
        this.accountService = accountService;
        this.ghnConfig = ghnConfig;
    }

    @GetMapping("/profile")
    public String viewProfilePage(Model model) {
        AccountDto accountDto = accountService.getAccountLogin();
        model.addAttribute("profile", accountDto);
        model.addAttribute("mapsApiKey", mapsApiKey);
        String token = ghnConfig.getToken();
        model.addAttribute("ghnEnabled", (token != null && !token.isEmpty() && !token.trim().isEmpty()));
        return "user/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(AccountDto accountDto, RedirectAttributes redirectAttributes) {
        try {
            accountService.updateProfile(accountDto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(ChangePasswordDto changePasswordDto, RedirectAttributes redirectAttributes) {
        try {
            accountService.changePassword(changePasswordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật mật khẩu thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}
