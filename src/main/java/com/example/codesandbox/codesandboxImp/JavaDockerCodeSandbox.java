package com.example.codesandbox.codesandboxImp;

import cn.hutool.core.util.ArrayUtil;
import com.example.codesandbox.JavaCodeSandboxTemplate;
import com.example.codesandbox.model.ExecuteCodeRequest;
import com.example.codesandbox.model.ExecuteCodeResponse;
import com.example.codesandbox.model.ExecuteMessage;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Primary
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    @Value("${docker.javaContains}")
    private String CONTAINER_NAME;

    @Resource
    private DockerClient dockerClient;

    private List<Container> containers;
    private Container container;


    private static final String SEPARATOR = "\\".equals(File.separator) ? "\\\\" : File.separator;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        if (containers == null || containers.size() == 0) {
            getContainer();
        }
        if (containers.size() == 0) {
            throw new RuntimeException("没有执行机");
        }
        container = containers.get(new Random().nextInt(containers.size()));
        CODE_HOME = "/www/wwwroot/code" + container.getNames()[0];

        // 指定这次的执行机
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public List<ExecuteMessage> runFile(File codeFile, List<String> inputList) {
        // 3）获取docker执行机ID
        String containerId = container.getId();
        List<ChangeLog> oldChangeLogs = dockerClient.containerDiffCmd(containerId).exec();

        // 记录数据
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        // docker exec mystifying_roentgen java -cp /app Main 1 3
        // 计时
        StopWatch stopWatch = new StopWatch();
        // 获取目录
        String[] arr = codeFile.getAbsolutePath().split(SEPARATOR);
        String sub = arr[arr.length - 2];
        for (String input : inputList) {
            // 创建需要执行的命令
            String[] argsArray = input.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app/" + sub, "Main"}, argsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
//            System.out.println("创建执行命令：" + execCreateCmdResponse);

            // 查看日志
            final String[] message = {null};
            final String[] errorMessage = {null};
            ResultCallbackTemplate<ResultCallback<Frame>, Frame> resultCallback = new ResultCallbackTemplate<>() {
                @Override
                public void onNext(Frame object) {
                    message[0] = new String(object.getPayload());
                    System.out.println("日志: " + new String(object.getPayload()));
                }
            };
            // 内存
            final Long[] maxMemory = {0L};
            ResultCallbackTemplate<ResultCallback<Statistics>, Statistics> exec = dockerClient.statsCmd(containerId).exec(new ResultCallbackTemplate<>() {
                @Override
                public void onNext(Statistics object) {
                    maxMemory[0] = Math.max(maxMemory[0], object.getMemoryStats().getUsage());
                }
            });

            // 开始执行命令
            try {
                exec.awaitStarted();
                stopWatch.start();
                dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(resultCallback).awaitCompletion();
                stopWatch.stop();
                exec.awaitCompletion(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("docker执行被中断指令中断");
            }

            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setMessage(message[0]);
//            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setMemory(maxMemory[0]);
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessageList.add(executeMessage);
        }

        List<ChangeLog> newChangeLogs = dockerClient.containerDiffCmd(containerId).exec();
//        checkNotChange(oldChangeLogs, newChangeLogs, container);

        return executeMessageList;
    }

    private void getContainer() {
        Pattern compile = Pattern.compile("^/" + CONTAINER_NAME + ".*");

        List<Container> exec = dockerClient.listContainersCmd().exec();
        containers = exec.stream().filter(container -> {
            for (String name : container.getNames()) {
                if (compile.matcher(name).find()) {
                    return true;
                }
            }
            return false;
        }).toList();
    }

    /**
     * 判断此容器前后状态是否一致，否则删除重建容器
     * @param oldChangeLogs
     * @param newChangeLogs
     * @param container
     */
    @Async
    void checkNotChange(List<ChangeLog> oldChangeLogs, List<ChangeLog> newChangeLogs, Container container) {
        if (oldChangeLogs == null || newChangeLogs == null) {
            return;
        }
        Set<String> oldPath = oldChangeLogs.stream().map(ChangeLog::getPath).collect(Collectors.toSet());
        Set<String> newPath = newChangeLogs.stream().map(ChangeLog::getPath).collect(Collectors.toSet());
        if (!oldPath.equals(newPath)) {
            // 移除就容器
            dockerClient.stopContainerCmd(container.getId()).exec();
            dockerClient.removeContainerCmd(container.getId()).exec();
            // 创建新容器
            HostConfig hostConfig = new HostConfig();
            hostConfig.setBinds(new Bind("/www/wwwroot/code" + container.getNames()[0], new Volume("/app")));
            hostConfig.withCpuCount(1L);
            hostConfig.withMemory(200L * 1000 * 1000);
            hostConfig.withDiskQuota(1000L * 1000 * 1000);
            CreateContainerResponse response = dockerClient.createContainerCmd(CONTAINER_NAME)
                    .withCmd("bash")
                    .withTty(true)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withName(container.getNames()[0])
                    .withNetworkDisabled(true)
                    .withHostConfig(hostConfig)
                    .exec();
            dockerClient.startContainerCmd(response.getId()).exec();
        }
    }
}