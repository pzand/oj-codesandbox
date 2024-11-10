package com.example.codesandbox.codesandboxImp;

import cn.hutool.core.io.resource.ResourceUtil;
import com.example.codesandbox.JavaCodeSandboxTemplate;
import com.example.codesandbox.model.ExecuteCodeRequest;
import com.example.codesandbox.model.ExecuteMessage;
import com.example.codesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

@Component
public class JavaNativeCodeSandbox_input extends JavaCodeSandboxTemplate {

//    public static void main(String[] args) {
//        JavaNativeCodeSandbox_input javaNativeCodeSandbox = new JavaNativeCodeSandbox_input();
//        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
//        executeCodeRequest.setInputList(List.of("1 2", "2 3"));
//        String code = ResourceUtil.readStr("testCodeScanner/Main.java", StandardCharsets.UTF_8);
//        // 测试系统输入的代码
//        executeCodeRequest.setCode(code);
//        executeCodeRequest.setLanguage("java");
//
//        javaNativeCodeSandbox.executeCode(executeCodeRequest);
//    }

    @Override
    public List<ExecuteMessage> runFile(File codeFile, List<String> inputList) {
        String userCodePath = codeFile.getParentFile().getAbsolutePath();

        try {
            // 3）执行代码
            List<ExecuteMessage> executeMessages = new LinkedList<>();
                // 测试系统输入的代码
            for (String input : inputList) {
                String cmdRun = String.format("java -cp %s Main %s", userCodePath, input);
                ExecuteMessage rumMessage = ProcessUtils.runInteractCommand(cmdRun, "执行", input);

                executeMessages.add(rumMessage);
            }
            return executeMessages;
        } catch (Exception e) {
            throw new RuntimeException("程序执行异常", e);
        }
    }
}