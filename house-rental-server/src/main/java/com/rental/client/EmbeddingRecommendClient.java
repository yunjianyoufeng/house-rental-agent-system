package com.rental.client;

import com.rental.dto.EmbeddingRecommendRequestDTO;
import com.rental.dto.EmbeddingRecommendResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class EmbeddingRecommendClient {

    private final RestTemplate restTemplate;

    @Value("${ai.recommend.embedding-url:http://localhost:9000/semantic-recommend}")
    private String embeddingUrl;

    public EmbeddingRecommendClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    public List<EmbeddingRecommendResponseDTO> recommend(EmbeddingRecommendRequestDTO requestDTO) {
        EmbeddingRecommendResponseDTO[] responseArray = restTemplate.postForObject(
                embeddingUrl,
                requestDTO,
                EmbeddingRecommendResponseDTO[].class
        );

        if (responseArray == null) {
            return List.of();
        }

        return Arrays.asList(responseArray);
    }
}