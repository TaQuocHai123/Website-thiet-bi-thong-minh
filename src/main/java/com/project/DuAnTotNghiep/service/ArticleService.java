package com.project.DuAnTotNghiep.service;

import com.project.DuAnTotNghiep.entity.Article;

import java.util.List;

public interface ArticleService {
    Article save(Article article);

    List<Article> findAll();
    
    Article findById(Long id);
    
    void deleteById(Long id);
}
