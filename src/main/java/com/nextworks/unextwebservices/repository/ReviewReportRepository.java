package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.ReviewReport;
import com.nextworks.unextwebservices.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {
    boolean existsByReviewIdAndStatus(UUID reviewId, ReportStatus status);
}
