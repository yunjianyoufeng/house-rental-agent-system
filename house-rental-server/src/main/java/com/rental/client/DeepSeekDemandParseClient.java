package com.rental.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.vo.RentDemandParseVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Component
public class DeepSeekDemandParseClient {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-v4-flash}")
    private String model;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    public DeepSeekDemandParseClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    public Optional<RentDemandParseVO> parseDemand(String query) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(query)) {
            return Optional.empty();
        }

        try {
            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0);
            requestBody.put("max_tokens", 800);
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("messages", buildMessages(query));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            String content = extractContent(response.getBody());
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }

            RentDemandParseVO vo = parseJsonContent(content, query);
            return Optional.of(vo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<Map<String, String>> buildMessages(String query) {
        String systemPrompt = """
                你是一个房屋租赁系统中的租房需求解析助手。
                请把用户输入的中文租房需求解析为严格 JSON。
                不要输出 Markdown，不要输出解释，只输出一个 JSON 对象。

                JSON 字段要求如下：
                {
                  "maxRent": 最高租金，数字或 null,
                  "minSquare": 最小面积，数字或 null,
                  "city": 城市，字符串或 null,
                  "area": 区域，字符串或 null,
                  "houseType": 户型，例如 "一室一厅"、"两室一厅"、"三室两厅"、"单间"、"主卧"，无法判断则为 null,
                  "scenes": 场景标签数组，可选值包括 "学生"、"交通"、"装修"、"学习环境"、"家庭居住",
                  "keywords": 关键词数组，例如 ["学校","考研","安静"],
                  "needQuiet": 是否需要安静环境，true 或 false,
                  "needTraffic": 是否需要交通便利，true 或 false,
                  "needDecoration": 是否需要精装修或拎包入住，true 或 false,
                  "studentFriendly": 是否适合学生，true 或 false,
                  "familyFriendly": 是否适合家庭居住，true 或 false
                }

                示例：
                用户输入：我想找一套1500元以内，靠近学校，适合考研复习的一室一厅
                JSON输出：
                {
                  "maxRent": 1500,
                  "minSquare": null,
                  "city": null,
                  "area": null,
                  "houseType": "一室一厅",
                  "scenes": ["学生","学习环境"],
                  "keywords": ["学校","考研","学习","安静"],
                  "needQuiet": true,
                  "needTraffic": false,
                  "needDecoration": false,
                  "studentFriendly": true,
                  "familyFriendly": false
                }
                """;

        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "请输出 json。用户租房需求：" + query)
        );
    }

    private String extractContent(Map responseBody) {
        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }

        Object firstChoiceObj = choices.get(0);
        if (!(firstChoiceObj instanceof Map<?, ?> firstChoice)) {
            return null;
        }

        Object messageObj = firstChoice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return null;
        }

        Object contentObj = message.get("content");
        return contentObj == null ? null : String.valueOf(contentObj);
    }

    private RentDemandParseVO parseJsonContent(String content, String originalQuery) throws Exception {
        Map<String, Object> map = objectMapper.readValue(content, new TypeReference<>() {});

        RentDemandParseVO vo = new RentDemandParseVO();
        vo.setQuery(originalQuery);
        vo.setParseType("LLM_PARSE");

        vo.setMaxRent(toBigDecimal(map.get("maxRent")));
        vo.setMinSquare(toBigDecimal(map.get("minSquare")));
        vo.setCity(toStringOrNull(map.get("city")));
        vo.setArea(toStringOrNull(map.get("area")));
        vo.setHouseType(toStringOrNull(map.get("houseType")));

        vo.setScenes(toStringList(map.get("scenes")));
        vo.setKeywords(toStringList(map.get("keywords")));

        vo.setNeedQuiet(toBoolean(map.get("needQuiet")));
        vo.setNeedTraffic(toBoolean(map.get("needTraffic")));
        vo.setNeedDecoration(toBoolean(map.get("needDecoration")));
        vo.setStudentFriendly(toBoolean(map.get("studentFriendly")));
        vo.setFamilyFriendly(toBoolean(map.get("familyFriendly")));

        return vo;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text) || "null".equalsIgnoreCase(text)) {
            return null;
        }

        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text) || "null".equalsIgnoreCase(text)) {
            return null;
        }

        return text;
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }

        return result;
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}