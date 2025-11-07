package com.project.DuAnTotNghiep.controller.user;

import com.project.DuAnTotNghiep.entity.Article;
import com.project.DuAnTotNghiep.entity.Comment;
import com.project.DuAnTotNghiep.service.ArticleService;
import com.project.DuAnTotNghiep.service.CategoryService;
import com.project.DuAnTotNghiep.service.CommentService;
import com.project.DuAnTotNghiep.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class BlogController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CommentService commentService;

    @GetMapping("getblog")
    public String getBlog(Model model) {
        List<Article> articles = articleService.findAll();
        // Process article content to preserve newlines
        articles.forEach(article -> {
            if (article.getSummary() != null) {
                // Replace both \r\n and \n with <br/> for HTML rendering
                String processed = article.getSummary()
                        .replace("\r\n", "<br/>")
                        .replace("\n", "<br/>");
                article.setSummary(processed);
            }
        });
        model.addAttribute("articles", articles);
        // Only expose blog category 'Tin tức' in the blog sidebar
        List<com.project.DuAnTotNghiep.entity.Category> blogCats = categoryService.getAll().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase("Tin tức"))
                .toList();
        model.addAttribute("categories", blogCats);
        // prepare latest articles for sidebar (sorted by createdAt desc)
        List<Article> latest = articles.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(3)
                .toList();
        model.addAttribute("latestArticles", latest);
        model.addAttribute("layoutUser", "user/blog");
        return "user/blog";
    }

    @GetMapping("getblogdetail/{id}")
    public String getBlogDetail(@PathVariable Long id, Model model) {
        Article article = articleService.findById(id);
        // Get comments for this article
        List<Comment> comments = commentService.getCommentsByArticleId(id);

        // Process content to preserve newlines
        if (article.getContent() != null) {
            article.setContent(article.getContent().replace("\n", "<br/>"));
        }
        if (article.getSummary() != null) {
            article.setSummary(article.getSummary().replace("\n", "<br/>"));
        }
        model.addAttribute("article", article);
        model.addAttribute("comments", comments);
        model.addAttribute("categories", categoryService.getAll());
        Pageable topThree = PageRequest.of(0, 3);
        model.addAttribute("featuredProducts", productService.getAllProductApi(topThree).getContent());
        // Removed duplicate layoutUser assignment
        return "user/blog-detail";
    }

    @PostMapping("getblogdetail/{id}/comment")
    public String addComment(@PathVariable Long id, Comment comment) {
        Article article = articleService.findById(id);
        comment.setArticle(article);
        commentService.save(comment);
        return "redirect:/getblogdetail/" + id;
    }
}
