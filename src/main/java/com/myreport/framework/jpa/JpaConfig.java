package com.myreport.framework.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 配置：扫描实体与 Repository，并开启事务。
 * <p>
 * 表结构同步由 application.properties 中
 * spring.jpa.hibernate.ddl-auto=update 控制。
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EntityScan(basePackages = "com.myreport.entity")
@EnableJpaRepositories(basePackages = "com.myreport.repository")
public class JpaConfig {
}
