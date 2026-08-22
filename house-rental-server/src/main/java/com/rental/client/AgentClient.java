package com.rental.client;

import com.rental.dto.AgentChatRequestDTO;
import com.rental.dto.AgentChatResponseDTO;
import com.rental.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class AgentClient {

    private final RestTemplate restTemplate;

    @Value("${ai.agent.chat-url:http://127.0.0.1:8001/api/agent/chat}")
    private String chatUrl;

    public AgentClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(90))
                .build();
    }

    public AgentChatResponseDTO chat(AgentChatRequestDTO requestDTO, String authorization) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
            HttpEntity<AgentChatRequestDTO> requestEntity = new HttpEntity<>(requestDTO, headers);
            ResponseEntity<AgentChatResponseDTO> responseEntity = restTemplate.exchange(
                    chatUrl,
                    HttpMethod.POST,
                    requestEntity,
                    AgentChatResponseDTO.class
            );
            AgentChatResponseDTO response = responseEntity.getBody();
            if (response == null || response.getAnswer() == null || response.getAnswer().isBlank()) {
                throw new BusinessException("智能租房助手未返回有效内容");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException("智能租房助手服务暂不可用，请稍后重试");
        }
    }
}
