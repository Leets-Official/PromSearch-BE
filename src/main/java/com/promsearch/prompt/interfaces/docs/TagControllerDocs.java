package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.interfaces.dto.response.PromptTagListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Tag", description = "프롬프트 태그 조회 API")
public interface TagControllerDocs {

    String IMPLEMENTED_BY_RUCHAN04 = "**작업자: ruchan04 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[TAG-001] 프롬프트 태그 목록 조회",
            description = IMPLEMENTED_BY_RUCHAN04
                    + """
                    홈 화면의 직군 메뉴와 필터 드롭다운에 표시할 태그 목록을 조회합니다.
                    tagType을 생략하면 JOB, TASK, AI_MODEL 태그를 모두 반환하고, 지정하면 해당 타입만 반환합니다.
                    이 API는 #65 기준에 맞춰 새 태그를 생성하지 않고, tags 테이블에 이미 저장된 태그만 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프롬프트 태그 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 태그 타입 요청")
    })
    ApiResponse<PromptTagListResponse> listTags(
            @Parameter(description = "조회할 태그 타입. 생략하면 전체 조회", example = "JOB")
            @RequestParam(required = false) TagType tagType
    );
}
