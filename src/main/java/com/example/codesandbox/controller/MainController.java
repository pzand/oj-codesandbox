package com.example.codesandbox.controller;

import com.example.codesandbox.CodeSandbox;
import com.example.codesandbox.model.ExecuteCodeRequest;
import com.example.codesandbox.model.ExecuteCodeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MainController {
    // 简单的流量染色 请求头
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Autowired
//    @Qualifier("javaNativeCodeSandbox_args")
    private CodeSandbox codeSandbox;

    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                    HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            System.out.println("鉴权失败");
            response.setStatus(403);
            return null;
        }

        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        log.debug("开始执行代码流程");
        return codeSandbox.executeCode(executeCodeRequest);
    }
}
