package com.example.cns.plan.service;

import com.example.cns.common.exception.BusinessException;
import com.example.cns.common.exception.ExceptionCode;
import com.example.cns.member.domain.repository.MemberRepository;
import com.example.cns.plan.domain.Plan;
import com.example.cns.plan.domain.PlanParticipation;
import com.example.cns.plan.domain.repository.PlanParticipationRepository;
import com.example.cns.plan.domain.repository.PlanRepository;
import com.example.cns.plan.dto.request.PlanCreateRequest;
import com.example.cns.plan.dto.request.PlanDateEditRequest;
import com.example.cns.plan.dto.request.PlanInviteRequest;
import com.example.cns.plan.dto.response.MemberResponse;
import com.example.cns.plan.dto.response.PlanCreateResponse;
import com.example.cns.plan.dto.response.PlanDetailResponse;
import com.example.cns.plan.dto.response.PlanListResponse;
import com.example.cns.project.domain.Project;
import com.example.cns.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.cns.common.exception.ExceptionCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final ProjectRepository projectRepository;
    private final PlanRepository planRepository;
    private final PlanParticipationRepository planParticipationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PlanCreateResponse createPlan(Long projectId, PlanCreateRequest request) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException(ExceptionCode.PROJECT_NOT_EXIST));
        Plan save = planRepository.save(request.toPlanEntity(project));
        saveParticipant(request.inviteList(), save.getId());
        return new PlanCreateResponse(save.getId());
    }

    @Transactional
    public void editPlan(Long planId, PlanCreateRequest planEditRequest) {
        Plan plan = getPlan(planId);
        plan.updatePlan(planEditRequest);
        if (!planEditRequest.inviteList().isEmpty()) {
            planParticipationRepository.deleteByPlan(planId);
            saveParticipant(planEditRequest.inviteList(), planId);
        }
    }

    @Transactional(readOnly = true)
    public List<PlanListResponse> getPlanListByProject(Long projectId) {
        List<Plan> planList = planRepository.getAllByProject(projectId);
        return planList.stream()
                .map(plan -> new PlanListResponse(plan.getId(), plan.getPlanName(), plan.getStartedAt(), plan.getEndedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePlan(Long planId) {
        planRepository.deleteById(planId);
    }

    @Transactional(readOnly = true)
    public PlanDetailResponse getPlanDetails(Long planId) {
        Plan plan = getPlan(planId);
        List<PlanParticipation> allByPlanId = planParticipationRepository.findAllByPlanId(planId);
        List<MemberResponse> participants = allByPlanId.stream()
                .map(planParticipation -> memberRepository.findById(planParticipation.getMember())
                        .map(member -> new MemberResponse(
                                member.getId(),
                                member.getNickname(),
                                member.getUrl()))
                        .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND)))
                .collect(Collectors.toList());
        return toPlanDetailResponse(plan, participants);
    }

    private PlanDetailResponse toPlanDetailResponse(Plan plan, List<MemberResponse> participants) {
        return new PlanDetailResponse(
                plan.getId(),
                plan.getPlanName(),
                plan.getStartedAt(),
                plan.getEndedAt(),
                plan.getContent(),
                participants
        );
    }

    public void validateManager(Long memberId, Long planId) {
        Plan plan = getPlan(planId);
        if (!Objects.equals(plan.getProject().getManager().getId(), memberId))
            throw new BusinessException(ExceptionCode.ONLY_MANAGER);
    }

    private Plan getPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.PLAN_NOT_EXIST));
    }

    @Transactional
    public void inviteParticipant(Long planId, PlanInviteRequest inviteRequest) {
        saveParticipant(inviteRequest.memberList(), planId);
    }

    private void saveParticipant(List<Long> inviteRequest, Long planId) {
        for (Long memberId : inviteRequest) {
            planParticipationRepository.save(new PlanParticipation(memberId, planId));
        }
    }

    @Transactional
    public void exitPlan(Long planId, Long memberId) {
        planParticipationRepository.deleteByPlanAndMember(planId, memberId);
    }

    @Transactional
    public void editPlanSchedule(Long planId, PlanDateEditRequest dateEditRequest) {
        Plan plan = getPlan(planId);
        plan.updateSchedule(dateEditRequest);
    }
}
