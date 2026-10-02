package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentSkillRepository extends JpaRepository<StudentSkill, UUID> {
    List<StudentSkill> findByPostulantProfileId(UUID postulantProfileId);
}