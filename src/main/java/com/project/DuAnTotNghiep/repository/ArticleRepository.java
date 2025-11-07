package com.project.DuAnTotNghiep.repository;

import com.project.DuAnTotNghiep.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
}
