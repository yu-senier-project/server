package com.example.cns.project.domain.repository;

import com.example.cns.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("SELECT p FROM Project p WHERE p.manager.id = :memberId")
    List<Project> findAllByMemberId(@Param("memberId") Long memberId);
}
