package com.example.codesandbox.codesandboxImp;

import cn.hutool.core.io.resource.ResourceUtil;
import com.example.codesandbox.JavaCodeSandboxTemplate;
import com.example.codesandbox.model.ExecuteCodeRequest;
import com.example.codesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JavaNativeCodeSandbox_args extends JavaCodeSandboxTemplate {
    @Override
    public List<ExecuteMessage> runFile(File codeFile, List<String> inputList) {
        return super.runFile(codeFile, inputList);
    }
}
// 1) 超时控制
// 创建一个守护线程，超时后自动中断process执行
// 2) 限制资源分配
// jvm参数可以限制java进程空间分配。-Xmx256m 最大堆空间为256m，-Xms256m 初始堆空间为256m
// 注意！-Xmx参数、JVM的堆内存限制，不等同于系统实际占用的资源空间，可能会超出
// 如果需要更严格的内存控制，需要在操作系统层面进行设置。Linux系统 可以使用cgroup实现限制
// 3) 限制代码 - 黑白名单，敏感词过滤 (麻烦，难以覆盖)
// 实现一个黑名单，比如禁止使用那些类，关键字。
// HuTool 字典树WordTree工具类：使用更少的空间存储词汇，实现高效的词汇查询
// 4) 限制用户的操作权限
// 比如限制对文件、内存、CPU、网络等资源的操作和访问
// Java安全管理器 是java提供保护 JVM、Java 安全的机制，可以轻松实现严格的操作限制
// 编写安全管理器，只需要继承 Security Manager。重写其中的方法，不让其执行默认的super方法即可

// 通过 System.setSecurityManager(...) 即可装填自定义的安全管理器
// 在运行java应用时指定 安全管理器："java -cp 运行应用路径;安全管理器路径 -Djava.security.manager=安全管理类名 应用类名"

// 优点：权限控制灵活，实现简单
// 缺点：粒度太细，难以精选化控制，容易出现一片都杀；本身也是Java代码，没有到系统层面
// 高版本17已经被弃用
