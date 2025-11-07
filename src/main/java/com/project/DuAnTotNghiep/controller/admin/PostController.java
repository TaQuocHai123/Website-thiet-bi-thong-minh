package com.project.DuAnTotNghiep.controller.admin;

import com.project.DuAnTotNghiep.entity.Article;
import com.project.DuAnTotNghiep.service.ArticleService;
import com.project.DuAnTotNghiep.utils.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
public class PostController {

    // No manual encoding checks here. Let the HTTP encoding filter and DB handle
    // Unicode.

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    @GetMapping("/post-create")
    public String viewCreatePost(Model model) {
        Article article = new Article();
        model.addAttribute("article", article);
        model.addAttribute("action", "/admin/post-save");
        return "admin/post-create";
    }

    @PostMapping("/post-save")
    public String handleSavePost(Article article, @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate required fields
            if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tiêu đề không được để trống");
                return article.getId() == null ? "redirect:/admin/post-create"
                        : "redirect:/admin/post-edit?id=" + article.getId();
            }
            if (article.getAuthor() == null || article.getAuthor().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tác giả không được để trống");
                return article.getId() == null ? "redirect:/admin/post-create"
                        : "redirect:/admin/post-edit?id=" + article.getId();
            }

            // If creating new article (no id) require an image. If updating, image is
            // optional.
            if (article.getId() == null && (image == null || image.isEmpty())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ảnh đại diện");
                return "redirect:/admin/post-create";
            }

            // Do NOT perform manual encoding conversions here. The HTTP filter and JDBC
            // driver
            // are configured to use UTF-8. Save the content as-is.

            // Handle image upload. If updating and no new image provided, keep existing
            // image path.
            if (image != null && !image.isEmpty()) {
                String fileName = StringUtils.cleanPath(Objects.requireNonNull(image.getOriginalFilename()));
                String saved = FileUploadUtil.saveFile(uploadDirectory, fileName, image);
                article.setImagePath("uploads/" + saved);
            } else if (article.getId() != null) {
                // retain existing image path on update
                Article existing = articleService.findById(article.getId());
                article.setImagePath(existing.getImagePath());
            }

            // Generate slug if not provided
            if (article.getSlug() == null || article.getSlug().isEmpty()) {
                String slug = article.getTitle().toLowerCase()
                        .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                        .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                        .replaceAll("[ìíịỉĩ]", "i")
                        .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                        .replaceAll("[ùúụủũưừứựửữ]", "u")
                        .replaceAll("[ỳýỵỷỹ]", "y")
                        .replaceAll("[đ]", "d")
                        .replaceAll("[^a-z0-9\\s-]", "")
                        .trim()
                        .replaceAll("[\\s]+", "-");
                article.setSlug(slug);
            }

            // Save article
            articleService.save(article);
            redirectAttributes.addFlashAttribute("successMessage",
                    article.getId() == null ? "Tạo bài viết thành công" : "Cập nhật bài viết thành công");
            return "redirect:/admin/post-list";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi upload ảnh: " + e.getMessage());
            return article.getId() == null ? "redirect:/admin/post-create"
                    : "redirect:/admin/post-edit?id=" + article.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu bài viết: " + e.getMessage());
            return article.getId() == null ? "redirect:/admin/post-create"
                    : "redirect:/admin/post-edit?id=" + article.getId();
        }
    }

    @GetMapping("/post-edit")
    public String viewEditPost(@RequestParam("id") Long id, Model model) {
        Article article = articleService.findById(id);
        model.addAttribute("article", article);
        model.addAttribute("action", "/admin/post-save");
        return "admin/post-create";
    }

    @PostMapping("/post-delete")
    public String handleDeletePost(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            articleService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bài viết thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa bài viết: " + e.getMessage());
        }
        return "redirect:/admin/post-list";
    }

    @GetMapping("/post-list")
    public String viewPostList(Model model) {
        model.addAttribute("articles", articleService.findAll());
        return "admin/post-list";
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
