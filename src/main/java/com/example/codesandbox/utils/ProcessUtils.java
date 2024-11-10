package com.example.codesandbox.utils;

import com.example.codesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

// 进程工具类
public class ProcessUtils {

    private static final String SYSTEM_TERMINAL_NAME;
    private static final String SYSTEM_ROOT_PATH;

    static {
        String systemName = System.getProperty("os.name");
        boolean isWindows = systemName.toLowerCase().contains("windows");
        SYSTEM_TERMINAL_NAME = isWindows ? "cmd" : "bash";
        SYSTEM_ROOT_PATH = isWindows ? "/c" : "-c";
        if (isWindows) {
            try {
                new ProcessBuilder().command("cmd", "/c", "chcp 65001").start();
            } catch (IOException e) {
                throw new RuntimeException("切换字符集失败: " + e.getMessage());
            }
        }
    }

    /**
     * 执行进行并获取信息
     *
     * @param command
     * @return
     */
    public static ExecuteMessage runCommand(String command, String operName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 计时器
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            ProcessBuilder processBuilder = new ProcessBuilder();
            Process exec = processBuilder.command(SYSTEM_TERMINAL_NAME, SYSTEM_ROOT_PATH, command).start();
            int status = exec.waitFor();
            executeMessage.setExitValue(status);

            // 停止计时
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessage.setMemory(0L);

            if (status == 0) {
                System.out.println(operName + "成功");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream(), Charset.forName("GBK")));
                ArrayList<String> executeOutput = new ArrayList<>();

                String outputLine;
                while ((outputLine = bufferedReader.readLine()) != null) {
                    executeOutput.add(outputLine);
                }
                executeMessage.setMessage(StringUtils.join(executeOutput, "\n"));

            } else {
                System.out.println(operName + "失败，错误码：" + status);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exec.getErrorStream(), Charset.forName("GBK")));
                ArrayList<String> errorOutput = new ArrayList<>();

                String outputLine;
                while ((outputLine = bufferedReader.readLine()) != null) {
                    errorOutput.add(outputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutput, "\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("IO异常: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException("执行被中断");
        }

        return executeMessage;
    }

    public static ExecuteMessage runInteractCommand(String command, String operName, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            Process exec = processBuilder.command(SYSTEM_TERMINAL_NAME, SYSTEM_ROOT_PATH, command).start();
            try (OutputStream outputStream = exec.getOutputStream();
                 InputStream inputStream = exec.getInputStream();
                 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)
            ) {
                // 通过OutputStream向里面写内容
                outputStreamWriter.write(args);
                outputStreamWriter.write('\n');
                outputStreamWriter.flush();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("GBK")));
                StringBuilder compileOutput = new StringBuilder();
                String outputLine;
                while ((outputLine = bufferedReader.readLine()) != null) {
                    compileOutput.append(outputLine);
                }
                System.out.println(operName + "成功");
                executeMessage.setMessage(compileOutput.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("IO异常: " + e.getMessage());
        }

        return executeMessage;
    }
}
