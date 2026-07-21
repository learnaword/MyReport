package com.myreport.repository;

import com.myreport.entity.GradEmploymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 毕业生就业去向仓储。
 */
public interface GradEmploymentRecordRepository extends JpaRepository<GradEmploymentRecord, Long> {

    Optional<GradEmploymentRecord> findBySchoolIdAndStudentNoAndGraduationYear(
            Long schoolId, String studentNo, Integer graduationYear);

    Page<GradEmploymentRecord> findBySchoolId(Long schoolId, Pageable pageable);

    List<GradEmploymentRecord> findBySchoolId(Long schoolId);
}
