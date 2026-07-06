package com.promsearch.test.interfaces;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    // 서버가 정상적으로 실행 중인지 확인하는 가장 가벼운 헬스체크 API입니다.
    @GetMapping("/health-check")
    public String healthCheck() {
        return "OK";
    }
}
