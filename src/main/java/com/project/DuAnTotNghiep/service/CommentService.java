package com.project.DuAnTotNghiep.service;

import com.project.DuAnTotNghiep.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment save(Comment comment);
    List<Comment> getCommentsByArticleId(Long articleId);
}