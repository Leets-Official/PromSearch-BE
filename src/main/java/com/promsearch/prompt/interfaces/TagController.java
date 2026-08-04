package com.promsearch.prompt.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.prompt.application.usecase.ListPromptTagsUseCase;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.interfaces.docs.TagControllerDocs;
import com.promsearch.prompt.interfaces.dto.response.PromptTagResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController implements TagControllerDocs {

    private final ListPromptTagsUseCase listPromptTagsUseCase;

    @GetMapping
    @SecurityRequirements
    @Override
    public ApiResponse<List<PromptTagResponse>> listTags(@RequestParam TagType type) {
        /*
         * 태그 목록은 로그인 전 홈 탐색에서도 필요하므로 공개 API로 둡니다.
         * Controller는 요청 타입만 받고, 태그 생성 여부 같은 정책은 application 계층에 위임합니다.
         */
        List<PromptTagResponse> result = listPromptTagsUseCase.listByType(type).stream()
                .map(PromptTagResponse::from)
                .toList();
        return ApiResponse.onSuccess(result);
    }
}
