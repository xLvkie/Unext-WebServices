package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.JobRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRequiredSkillRepository extends JpaRepository<JobRequiredSkill, UUID> {
    List<JobRequiredSkill> findByJobOfferId(UUID jobOfferId);

    @Query("SELECT DISTINCT s.name FROM JobRequiredSkill s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.name")
    List<String> suggestNames(@Param("query") String query);
}
