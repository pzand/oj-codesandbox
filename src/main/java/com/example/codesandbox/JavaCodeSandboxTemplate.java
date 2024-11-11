package com.example.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.codesandbox.model.ExecuteCodeRequest;
import com.example.codesandbox.model.ExecuteCodeResponse;
import com.example.codesandbox.model.ExecuteMessage;
import com.example.codesandbox.model.JudgeInfo;
import com.example.codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

// 使用模板方法 统一流程
// 1.
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    public static String CODE_HOME = "/www/wwwroot/code";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 给定一个指定的目录存放待执行的用户代码。用户代码统一使用Main作为类名方便管理
        // 为了防止不同的用户代码文件名冲突，因此需要隔离。使用UUID建一个子目录，存放不同的用户代码

        try {
            // 1）把用户代码保存为文件
            File userCodeFile = saveCode2File(code);

            // 2）编译执行用户代码
            log.debug("开始编译代码");
            ExecuteMessage compileMessage = compileFile(userCodeFile);

            // 3）执行代码
            log.debug("开始执行代码");
            List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

            // 4）整理输出结果
            log.debug("整理输出结果");
            ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

            // 5）删除文件
            log.debug("删除用户代码文件");
            boolean b = deleteFile(userCodeFile);
            if (!b) {
                log.error("删除文件失败: " + userCodeFile.getAbsolutePath());
            }
            return outputResponse;
        } catch (Exception e) {
            return getErrorResponse(e);
        }
    }

    /**
     * 1. 把用户代码保存为文件
     *
     * @param code 用户代码
     * @return 保存的文件
     */
    public File saveCode2File(String code) {
        // 获取当前的工作路径
        String codePathName = CODE_HOME;
        if (!FileUtil.exist(codePathName)) {
            FileUtil.mkdir(codePathName);
        }
        log.debug("代码保存路径: " + codePathName);

        String userCodeParentPathName = codePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPathName + File.separator;
        log.debug("用户代码路径: " + userCodePath + GLOBAL_JAVA_CLASS_NAME);
        return FileUtil.writeString(code, userCodePath + GLOBAL_JAVA_CLASS_NAME, StandardCharsets.UTF_8);
    }

    /**
     * 2. 编译执行用户代码
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        log.debug("用户代码是否存在：" + userCodeFile.exists());
        try {
            String cmdCompile = String.format("javac %s", userCodeFile.getAbsolutePath());
            ExecuteMessage compileMessage = ProcessUtils.runCommand(cmdCompile, "编译");
            if (compileMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误: " + compileMessage.getErrorMessage());
            }
            return compileMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. 执行文件获得执行结果列表
     *
     * @param codeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File codeFile, List<String> inputList) {
        String userCodePath = codeFile.getParentFile().getAbsolutePath();

        try {
            // 3）执行代码
            List<ExecuteMessage> executeMessages = new LinkedList<>();
            for (String input : inputList) {
                String cmdRun = String.format("java -cp %s Main %s", userCodePath, input);
                ExecuteMessage rumMessage = ProcessUtils.runCommand(cmdRun, "执行");

                // 测试系统输入的代码
//                String cmdRun = String.format("java -cp %s Main %s", userCodePath, "1 2");
//                ExecuteMessage rumMessage = ProcessUtils.runInteractCommand(cmdRun, "执行", "1 2");

                executeMessages.add(rumMessage);
            }
            return executeMessages;
        } catch (Exception e) {
            throw new RuntimeException("程序执行异常", e);
        }
    }

    /**
     * 4）收集整理输出信息
     * @param executeMessages
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessages) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new LinkedList<>();
        for (ExecuteMessage executeMessage : executeMessages) {
            String errorMessage = executeMessage.getErrorMessage();
            // 出现异常信息
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add((executeMessage.getMessage()));
        }
        // 正常执行
        if (outputList.size() == executeMessages.size()) {
            executeCodeResponse.setStatus(1);
        }
        // 设置output
        executeCodeResponse.setOutputList(outputList);
        // 设置时间
        long maxTime = 0L;
        long maxMemory = 0L;
        for (ExecuteMessage executeMessage : executeMessages) {
            if (executeMessage.getTime() != null) {
                maxTime = Math.max(maxTime, executeMessage.getTime());
            }
            if (executeMessage.getMemory() != null) {
                maxMemory = Math.max(maxMemory, executeMessage.getMemory());
            }
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5. 删除文件
     * @param codeFile
     * @return
     */
    public boolean deleteFile(File codeFile) {
        if (codeFile.exists()) {
            boolean del = FileUtil.del(codeFile.getParentFile().getAbsolutePath());
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new LinkedList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
