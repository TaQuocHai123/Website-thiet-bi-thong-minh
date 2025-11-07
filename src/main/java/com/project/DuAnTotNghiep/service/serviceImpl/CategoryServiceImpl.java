package com.project.DuAnTotNghiep.service.serviceImpl;

import com.project.DuAnTotNghiep.dto.Category.CategoryDto;
import com.project.DuAnTotNghiep.entity.Category;
import com.project.DuAnTotNghiep.entity.Color;
import com.project.DuAnTotNghiep.exception.ShopApiException;
import com.project.DuAnTotNghiep.repository.CategoryRepository;
import com.project.DuAnTotNghiep.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Page<Category> getAllCategory(Pageable pageable) {
        return categoryRepository.findAllByDeleteFlagFalse(pageable);
    }

    @Override
    public Category createCategory(Category category) {
        // If code is null or empty, generate new code
        if(category.getCode() == null || category.getCode().trim().isEmpty()) {
            Category lastCategory = categoryRepository.findFirstByOrderByIdDesc();
            Long nextId = (lastCategory == null) ? 1L : lastCategory.getId() + 1;
            String categoryCode = "LSP" + String.format("%04d", nextId);
            category.setCode(categoryCode);
        } else {
            String code = category.getCode().trim();
            Optional<Category> existing = categoryRepository.findByCode(code);
            if (existing.isPresent()) {
                Category ex = existing.get();
                // If existed but was soft-deleted, restore it and update name
                if (ex.getDeleteFlag() != null && ex.getDeleteFlag()) {
                    ex.setDeleteFlag(false);
                    ex.setName(category.getName());
                    ex.setStatus(1);
                    return categoryRepository.save(ex);
                }
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Mã loại " + code + " đã tồn tại");
            }
            category.setCode(code);
        }
        category.setStatus(1);
        category.setDeleteFlag(false);
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Category category) {
        Category existingCategory = categoryRepository.findById(category.getId())
            .orElseThrow(() -> new ShopApiException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục với id " + category.getId()));

        if (existingCategory.getDeleteFlag()) {
            throw new ShopApiException(HttpStatus.BAD_REQUEST, "Danh mục này đã bị xóa");
        }

        if(!existingCategory.getCode().equals(category.getCode())) {
            if(categoryRepository.existsByCode(category.getCode())) {
                throw new ShopApiException(HttpStatus.BAD_REQUEST, "Mã loại " + category.getCode() + " đã tồn tại");
            }
        }
        category.setDeleteFlag(false);
        return categoryRepository.save(category);
    }

    @Override
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ShopApiException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục với id " + id));

        if (category.getDeleteFlag()) {
            throw new ShopApiException(HttpStatus.BAD_REQUEST, "Danh mục này đã bị xóa");
        }

        category.setDeleteFlag(true);
        categoryRepository.save(category);
    }

    @Override
    public boolean existsById(Long id) {
        return categoryRepository.existsById(id);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id).map(category -> {
            if (category.getDeleteFlag() == null || !category.getDeleteFlag()) {
                return category;
            }
            return null;
        });
    }

    @Override
    public List<Category> getAll() {
        return categoryRepository.findAllByDeleteFlagFalse();
    }

    @Override
    public CategoryDto createCategoryApi(CategoryDto categoryDto) {
        if(categoryRepository.existsByCode(categoryDto.getCode())) {
            throw new ShopApiException(HttpStatus.BAD_REQUEST, "Mã loại đã tồn tại");
        }
        Category category = new Category(null, categoryDto.getCode(), categoryDto.getName(), 1, false);
        Category categoryNew = categoryRepository.save(category);
        return new CategoryDto(category.getId(), category.getCode(), category.getName());
    }
}
