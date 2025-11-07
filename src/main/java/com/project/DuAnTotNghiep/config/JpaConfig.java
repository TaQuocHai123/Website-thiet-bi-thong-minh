package com.project.DuAnTotNghiep.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.project.DuAnTotNghiep.entity")
@EnableJpaRepositories("com.project.DuAnTotNghiep.repository")
public class JpaConfig {
}