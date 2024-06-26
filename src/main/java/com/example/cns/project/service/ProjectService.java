package com.example.cns.project.service;

import com.example.cns.common.exception.BusinessException;
import com.example.cns.common.exception.ExceptionCode;
import com.example.cns.feed.post.dto.response.PostMember;
import com.example.cns.member.domain.Member;
import com.example.cns.member.domain.repository.MemberRepository;
import com.example.cns.member.dto.response.MemberSearchResponse;
import com.example.cns.project.domain.Project;
import com.example.cns.project.domain.ProjectParticipation;
import com.example.cns.project.domain.ProjectParticipationID;
import com.example.cns.project.domain.repository.ProjectParticipationRepository;
import com.example.cns.project.domain.repository.ProjectRepository;
import com.example.cns.project.dto.request.ProjectCreateRequest;
import com.example.cns.project.dto.request.ProjectInviteRequest;
import com.example.cns.project.dto.request.ProjectPatchRequest;
import com.example.cns.project.dto.response.ProjectInfoResponse;
import com.example.cns.project.dto.response.ProjectParticipantInfo;
import com.example.cns.project.dto.response.ProjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectParticipationRepository projectParticipationRepository;

    /*
    프로젝트 생성
     */
    @Transactional
    public void createProject(ProjectCreateRequest projectCreateRequest) {

        Member manager = isMemberExists(projectCreateRequest.managerId());

        Long projectId = projectRepository.save(projectCreateRequest.toProjectEntity(manager)).getId();

        List<ProjectParticipation> participationList = new ArrayList<>();

        ProjectParticipation projectManager = ProjectParticipation.builder()
                .project(projectId)
                .member(manager.getId())
                .build();
        participationList.add(projectManager);

        //초대된 인원이 서비스에 가입된 사람인지?
        List<Member> participants = memberRepository.findByIdIn(projectCreateRequest.memberList());

        if (participants.size() == projectCreateRequest.memberList().size()) {
            participants.forEach(
                    member -> {
                        ProjectParticipation participation = ProjectParticipation.builder()
                                .project(projectId)
                                .member(member.getId())
                                .build();
                        participationList.add(participation);
                    }
            );
        }

        projectParticipationRepository.saveAll(participationList);
    }

    /*
    프로젝트 수정
     */
    @Transactional
    public void patchProject(Long projectId, ProjectPatchRequest projectPatchRequest) {

        Project project = isProjectExists(projectId);
        project.updateProject(projectPatchRequest);
    }

    /*
    프로젝트 삭제
     */
    @Transactional
    public void deleteProject(Long projectId) {
        isProjectExists(projectId);
        projectParticipationRepository.deleteAllByProjectId(projectId); //프로젝트 참여자 삭제
        projectRepository.deleteById(projectId); //프로젝트 삭제, 게시글 삭제, 일정, 일정 참여자 삭제
    }

    /*
    프로젝트 나가기
     */
    @Transactional
    public void exitProject(Long memberId, Long projectId) {
        Project project = isProjectExists(projectId);

        if (Objects.equals(project.getManager().getId(), memberId))
            throw new BusinessException(ExceptionCode.MANAGER_CANNOT_LEAVE);
        else projectParticipationRepository.deleteById(memberId, projectId);
    }

    /*
    특정 프로젝트 조회
     */
    public ProjectResponse getSpecificProject(Long projectId) {
        Project project = isProjectExists(projectId);
        return ProjectResponse.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .detail(project.getDetail())
                .goal(project.getGoal())
                .start(project.getStart())
                .end(project.getEnd())
                .build();
    }

    /*
    프로젝트 목록 조회
     */
    public List<ProjectInfoResponse> getProjectListByMemberId(Long memberId) {

        List<Project> projectList = projectRepository.findProjectsByMemberId(memberId);
        List<ProjectInfoResponse> responses = new ArrayList<>();

        projectList.forEach(
                project -> {
                    responses.add(
                            ProjectInfoResponse.builder()
                                    .projectId(project.getId())
                                    .projectName(project.getProjectName())
                                    .detail(project.getDetail())
                                    .postMember(new PostMember(project.getManager().getId(), project.getManager().getNickname(), project.getManager().getUrl()))
                                    .build()
                    );
                }
        );

        return responses;
    }

    /*
    프로젝트 참여자 조회
     */
    public ProjectParticipantInfo getProjectParticipant(Long projectId) {

        Project project = isProjectExists(projectId);
        List<ProjectParticipation> participationList = projectParticipationRepository.findProjectParticipationsByProjectId(projectId);
        List<MemberSearchResponse> memberList = new ArrayList<>();

        participationList.forEach(
                participant -> {
                    Optional<Member> member = memberRepository.findById(participant.getMember());
                    if (member.isPresent()) {
                        memberList.add(
                                MemberSearchResponse.builder()
                                        .memberId(member.get().getId())
                                        .nickname(member.get().getNickname())
                                        .profile(member.get().getUrl())
                                        .build()
                        );
                    }
                }
        );

        ProjectParticipantInfo response = ProjectParticipantInfo.builder()
                .managerId(project.getManager().getId())
                .memberList(memberList)
                .build();

        return response;
    }

    /*
    프로젝트 참여자 수정 / 추가 초대
     */
    @Transactional
    public void patchProjectParticipant(Long projectId, ProjectInviteRequest projectInviteRequest) {

        Project project = isProjectExists(projectId);

        //현재 참여자 리스트
        List<Long> previousList = projectParticipationRepository.findProjectParticipationsIdByProjectId(projectId);

        //수정된 참여자 리스트
        List<Long> updateList = new ArrayList<>(projectInviteRequest.memberList());
        updateList.add(projectInviteRequest.managerId());

        //새로 추가된 인원
        List<Long> addedList = updateList.stream()
                .filter(memberId -> !previousList.contains(memberId))
                .toList();

        //삭제된 인원
        List<Long> removedList = previousList.stream()
                .filter(memberId -> !updateList.contains(memberId))
                .toList();

        Member manager = isMemberExists(projectInviteRequest.managerId());

        //담당자 교체
        project.setManager(manager);

        //새로추가된 인원
        List<ProjectParticipation> addList = new ArrayList<>();
        addedList.forEach(
                memberId -> {
                    ProjectParticipation member = ProjectParticipation.builder()
                            .project(projectId)
                            .member(memberId)
                            .build();
                    addList.add(member);
                }
        );

        projectParticipationRepository.saveAll(addList);

        //삭제된 인원
        List<ProjectParticipation> removeList = new ArrayList<>();
        removedList.forEach(
                memberId -> {
                    ProjectParticipation member = ProjectParticipation.builder()
                            .project(projectId)
                            .member(memberId)
                            .build();
                    removeList.add(member);
                }
        );

        projectParticipationRepository.deleteAllInBatch(removeList);

    }

    private Project isProjectExists(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(
                () -> new BusinessException(ExceptionCode.PROJECT_NOT_EXIST));
    }

    private Member isMemberExists(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new BusinessException(ExceptionCode.MEMBER_NOT_FOUND));
    }


    public void validateMemberInProject(Long memberId, Long projectId) {
        if (!projectParticipationRepository.existsById(new ProjectParticipationID(memberId, projectId)))
            throw new BusinessException(ExceptionCode.NOT_PROJECT_PARTICIPANTS);
    }
}
