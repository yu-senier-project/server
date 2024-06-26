package com.example.cns.task.domain.repository;

import com.example.cns.member.domain.Member;
import com.example.cns.project.domain.Project;
import com.example.cns.task.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByIdAndMember(Long taskId, Member member);

    List<Task> findAllByMemberAndProject(Member member, Project project);

    @Query("select t from Task t join fetch t.member where t.project=:project ")
    List<Task> findAllByProject(@Param(value = "project") Project project);

    @Modifying
    @Query("UPDATE Task t set t.member = :member where t.member.id=:memberId and t.project.id = :projectId")
    void updateTasksByMemberId(@Param("member") Member member, @Param("memberId") Long memberId, @Param("projectId") Long projectId);
}
