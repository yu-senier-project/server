package com.example.cns.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "프로젝트 초대 요청 DTO")
public record ProjectInviteRequest(

        @NotNull
        @Schema(description = "담당자 인덱스 값")
        Long managerId,
        @NotNull
        @Schema(description = "참여자 인덱스 값 리스트")
        List<Long> memberList
) {
}
