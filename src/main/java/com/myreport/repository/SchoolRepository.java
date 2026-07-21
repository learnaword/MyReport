package com.myreport.repository;

import com.myreport.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 学校仓储。
 */
public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findFirstByName(String name);
}
