package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentSkillRepository extends JpaRepository<StudentSkill, UUID> {
    List<StudentSkill> findByPostulantProfileId(UUID postulantProfileId);
    boolean existsByPostulantProfileIdAndNameIgnoreCase(UUID postulantProfileId, String name);

    @Query("SELECT DISTINCT s.name FROM StudentSkill s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.name")
    List<String> suggestNames(@Param("query") String query);
}