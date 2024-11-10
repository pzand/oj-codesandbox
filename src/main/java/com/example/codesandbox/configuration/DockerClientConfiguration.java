package com.example.codesandbox.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DockerClientConfiguration {
    @Value("${docker.host}")
    private String host;

    @Bean
    public DockerClient getDockerClient() {
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClientConfig custom = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .withDockerTlsVerify(false)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
//                .dockerHost(standard.getDockerHost())
                .dockerHost(custom.getDockerHost())
                .sslConfig(standard.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(standard, httpClient);
    }
}
