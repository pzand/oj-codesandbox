package com.example.codesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.example.codesandbox.model.ExecuteCodeRequest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

//@SpringBootTest
class JavaDockerCodeSandboxTest {

    @Resource
    private CodeSandbox dockerSandbox;

    @Test
    public void testDockerSandBox() {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(List.of("1 2", "2 3"));
        String code = ResourceUtil.readStr("testCode/Main.java", StandardCharsets.UTF_8);
        // 测试系统输入的代码
//        String code = ResourceUtil.readStr("testCodeScanner/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");

        dockerSandbox.executeCode(executeCodeRequest);
    }
}