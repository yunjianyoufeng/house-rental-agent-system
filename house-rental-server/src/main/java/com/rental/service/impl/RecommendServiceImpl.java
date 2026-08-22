package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.client.EmbeddingRecommendClient;
import com.rental.dto.EmbeddingRecommendHouseDTO;
import com.rental.dto.EmbeddingRecommendRequestDTO;
import com.rental.dto.EmbeddingRecommendResponseDTO;
import com.rental.dto.HouseRecommendDTO;
import com.rental.dto.RentDemandParseDTO;
import com.rental.entity.House;
import com.rental.entity.RecommendRecord;
import com.rental.mapper.HouseMapper;
import com.rental.mapper.RecommendRecordMapper;
import com.rental.service.RecommendService;
import com.rental.service.RentDemandParseService;
import com.rental.vo.HouseRecommendVO;
import com.rental.vo.RentDemandParseVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private final HouseMapper houseMapper;

    private final RecommendRecordMapper recommendRecordMapper;

    private final EmbeddingRecommendClient embeddingRecommendClient;

    private final RentDemandParseService rentDemandParseService;

    public RecommendServiceImpl(HouseMapper houseMapper,
                                RecommendRecordMapper recommendRecordMapper,
                                EmbeddingRecommendClient embeddingRecommendClient,
                                RentDemandParseService rentDemandParseService) {
        this.houseMapper = houseMapper;
        this.recommendRecordMapper = recommendRecordMapper;
        this.embeddingRecommendClient = embeddingRecommendClient;
        this.rentDemandParseService = rentDemandParseService;
    }

    @Override
    public List<HouseRecommendVO> recommendHouse(HouseRecommendDTO dto) {
        long startTime = System.currentTimeMillis();

        RentDemandParseVO parseVO = parseDemandSafely(dto);
        HouseRecommendDTO effectiveDto = buildEffectiveRecommendDTO(dto, parseVO);

        String modelType = normalizeModelType(effectiveDto.getModelType());
        int topK = effectiveDto.getTopK() == null || effectiveDto.getTopK() <= 0 ? 5 : Math.min(effectiveDto.getTopK(), 20);
        BigDecimal maxRent = effectiveDto.getMaxRent() != null ? effectiveDto.getMaxRent() : extractMaxRent(effectiveDto.getQuery());

        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getStatus, 1)
                .eq(House::getAuditStatus, 1)
                .orderByDesc(House::getId);

        if (StringUtils.hasText(effectiveDto.getCity())) {
            wrapper.eq(House::getCity, effectiveDto.getCity().trim());
        }

        if (StringUtils.hasText(effectiveDto.getArea())) {
            wrapper.eq(House::getArea, effectiveDto.getArea().trim());
        }

        if (StringUtils.hasText(effectiveDto.getHouseType())) {
            wrapper.like(House::getHouseType, effectiveDto.getHouseType().trim());
        }

        if (maxRent != null) {
            wrapper.le(House::getRentPrice, maxRent);
        }

        if (effectiveDto.getMinSquare() != null) {
            wrapper.ge(House::getSquare, effectiveDto.getMinSquare());
        }

        List<House> houseList = houseMapper.selectList(wrapper);

        List<HouseRecommendVO> result;
        if ("TFIDF".equals(modelType)) {
            result = recommendByTfidf(effectiveDto, houseList, topK);
        } else if ("EMBEDDING".equals(modelType)) {
            result = recommendByEmbedding(effectiveDto, houseList, topK);
        } else {
            result = recommendByRule(effectiveDto, houseList, maxRent, topK);
        }

        if (result.isEmpty() && !houseList.isEmpty() && allowFallbackDisplay(effectiveDto)) {
            result = houseList.stream()
                    .limit(topK)
                    .map(house -> {
                        HouseRecommendVO vo = new HouseRecommendVO();
                        vo.setHouse(house);
                        vo.setScore(1.0);
                        vo.setReason("暂无明显匹配结果，展示当前可租房源供参考");
                        vo.setModelType(modelType);
                        return vo;
                    })
                    .toList();
        }

        appendParseReason(result, parseVO);

        long responseTime = System.currentTimeMillis() - startTime;
        saveRecommendRecord(dto, result, responseTime, modelType);

        return result;
    }

    /**
     * 是否允许在没有明显匹配结果时展示兜底房源。
     *
     * 如果用户已经输入了明确筛选条件，例如城市、区域、户型、租金、面积，
     * 没有匹配结果时就不再展示其他地区或不符合条件的房源。
     */
    private boolean allowFallbackDisplay(HouseRecommendDTO effectiveDto) {
        if (effectiveDto == null) {
            return true;
        }

        boolean hasStrictFilter =
                StringUtils.hasText(effectiveDto.getCity())
                        || StringUtils.hasText(effectiveDto.getArea())
                        || StringUtils.hasText(effectiveDto.getHouseType())
                        || effectiveDto.getMaxRent() != null
                        || effectiveDto.getMinSquare() != null;

        if (hasStrictFilter) {
            return false;
        }

        String query = nullToEmpty(effectiveDto.getQuery());

        if (!StringUtils.hasText(query)) {
            return true;
        }

        return containsAny(query, new String[]{
                "随便",
                "都可以",
                "推荐几套",
                "推荐房源",
                "看看房源",
                "找几套",
                "有什么房子",
                "全部房源",
                "所有房源"
        });
    }
    /**
     * 安全解析租房需求。
     * 如果解析失败，不影响原推荐流程。
     */
    private RentDemandParseVO parseDemandSafely(HouseRecommendDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getQuery())) {
            return null;
        }

        try {
            RentDemandParseDTO parseDTO = new RentDemandParseDTO();
            parseDTO.setQuery(dto.getQuery());
            parseDTO.setUseLlm(!Boolean.FALSE.equals(dto.getUseLlmParse()));
            return rentDemandParseService.parseDemand(parseDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据解析结果补充推荐参数：
     * 1. 如果前端未传 city，则使用解析出的 city
     * 2. 如果前端未传 area，则使用解析出的 area
     * 3. 如果前端未传 maxRent，则使用解析出的 maxRent
     * 4. 如果前端未传 minSquare，则使用解析出的 minSquare
     * 5. 如果前端未传 houseType，则使用解析出的 houseType
     * 6. 把解析出的关键词和场景拼接到 query 后，提高 TFIDF/EMBEDDING 匹配效果
     */
    private HouseRecommendDTO buildEffectiveRecommendDTO(HouseRecommendDTO source, RentDemandParseVO parseVO) {
        HouseRecommendDTO target = new HouseRecommendDTO();

        target.setQuery(source.getQuery());
        target.setCity(source.getCity());
        target.setArea(source.getArea());
        target.setMaxRent(source.getMaxRent());
        target.setMinSquare(source.getMinSquare());
        target.setHouseType(source.getHouseType());
        target.setModelType(source.getModelType());
        target.setTopK(source.getTopK());

        if (parseVO == null) {
            return target;
        }

        if (!StringUtils.hasText(target.getCity()) && StringUtils.hasText(parseVO.getCity())) {
            target.setCity(parseVO.getCity());
        }

        if (!StringUtils.hasText(target.getArea()) && StringUtils.hasText(parseVO.getArea())) {
            target.setArea(parseVO.getArea());
        }

        if (target.getMaxRent() == null && parseVO.getMaxRent() != null) {
            target.setMaxRent(parseVO.getMaxRent());
        }

        if (target.getMinSquare() == null && parseVO.getMinSquare() != null) {
            target.setMinSquare(parseVO.getMinSquare());
        }

        if (!StringUtils.hasText(target.getHouseType()) && StringUtils.hasText(parseVO.getHouseType())) {
            target.setHouseType(parseVO.getHouseType());
        }

        target.setQuery(buildEnhancedQuery(source.getQuery(), parseVO));

        return target;
    }

    /**
     * 增强查询文本，让推荐模型能利用解析出的城市、区域、关键词和场景标签。
     */
    private String buildEnhancedQuery(String originalQuery, RentDemandParseVO parseVO) {
        List<String> parts = new ArrayList<>();
        parts.add(nullToEmpty(originalQuery));

        if (StringUtils.hasText(parseVO.getCity())) {
            parts.add(parseVO.getCity());
        }

        if (StringUtils.hasText(parseVO.getArea())) {
            parts.add(parseVO.getArea());
        }

        if (parseVO.getKeywords() != null && !parseVO.getKeywords().isEmpty()) {
            parts.add(String.join(" ", parseVO.getKeywords()));
        }

        if (parseVO.getScenes() != null && !parseVO.getScenes().isEmpty()) {
            parts.add(String.join(" ", parseVO.getScenes()));
        }

        if (Boolean.TRUE.equals(parseVO.getNeedQuiet())) {
            parts.add("安静 学习 考研 休息");
        }

        if (Boolean.TRUE.equals(parseVO.getNeedTraffic())) {
            parts.add("交通 公交 通勤 出行方便");
        }

        if (Boolean.TRUE.equals(parseVO.getNeedDecoration())) {
            parts.add("精装 装修 拎包入住 直接入住");
        }

        if (Boolean.TRUE.equals(parseVO.getStudentFriendly())) {
            parts.add("学生 学校 大学 高校");
        }

        if (Boolean.TRUE.equals(parseVO.getFamilyFriendly())) {
            parts.add("家庭 一家人 两室 三室 生活方便 配套齐全");
        }

        return parts.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    /**
     * 在推荐原因中体现“已结合需求解析结果”。
     */
    private void appendParseReason(List<HouseRecommendVO> result, RentDemandParseVO parseVO) {
        if (result == null || result.isEmpty() || parseVO == null) {
            return;
        }

        List<String> parseParts = new ArrayList<>();

        if (StringUtils.hasText(parseVO.getCity())) {
            parseParts.add("城市：" + parseVO.getCity());
        }

        if (StringUtils.hasText(parseVO.getArea())) {
            parseParts.add("区域：" + parseVO.getArea());
        }

        if (parseVO.getMaxRent() != null) {
            parseParts.add("预算≤" + parseVO.getMaxRent() + "元");
        }

        if (StringUtils.hasText(parseVO.getHouseType())) {
            parseParts.add("户型：" + parseVO.getHouseType());
        }

        if (parseVO.getScenes() != null && !parseVO.getScenes().isEmpty()) {
            parseParts.add("场景：" + String.join("、", parseVO.getScenes()));
        }

        if (parseParts.isEmpty()) {
            return;
        }

        String parseReason = "已结合需求解析结果（" + String.join("，", parseParts) + "）";

        for (HouseRecommendVO vo : result) {
            if (!StringUtils.hasText(vo.getReason())) {
                vo.setReason(parseReason);
            } else if (!vo.getReason().contains("已结合需求解析结果")) {
                vo.setReason(vo.getReason() + "；" + parseReason);
            }
        }
    }

    private List<HouseRecommendVO> recommendByRule(HouseRecommendDTO dto, List<House> houseList, BigDecimal maxRent, int topK) {
        return houseList.stream()
                .map(house -> buildRuleRecommendVO(dto, house, maxRent))
                .filter(vo -> vo.getScore() != null && vo.getScore() > 0)
                .sorted(Comparator.comparing(HouseRecommendVO::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private HouseRecommendVO buildRuleRecommendVO(HouseRecommendDTO dto, House house, BigDecimal maxRent) {
        ScoreResult scoreResult = calculateRuleScore(dto, house, maxRent);

        HouseRecommendVO vo = new HouseRecommendVO();
        vo.setHouse(house);
        vo.setScore(scoreResult.getScore());
        vo.setReason(String.join("；", scoreResult.getReasons()));
        vo.setModelType("RULE");
        return vo;
    }

    private ScoreResult calculateRuleScore(HouseRecommendDTO dto, House house, BigDecimal maxRent) {
        String query = nullToEmpty(dto.getQuery());
        String houseText = buildHouseText(house);

        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        if (maxRent != null && house.getRentPrice() != null && house.getRentPrice().compareTo(maxRent) <= 0) {
            score += 25;
            reasons.add("租金符合预算要求");
        }

        if (dto.getMinSquare() != null && house.getSquare() != null && house.getSquare().compareTo(dto.getMinSquare()) >= 0) {
            score += 10;
            reasons.add("面积符合需求");
        }

        if (StringUtils.hasText(dto.getCity()) && dto.getCity().trim().equals(house.getCity())) {
            score += 15;
            reasons.add("城市符合需求");
        }

        if (StringUtils.hasText(dto.getArea()) && dto.getArea().trim().equals(house.getArea())) {
            score += 15;
            reasons.add("区域符合需求");
        }

        if (StringUtils.hasText(dto.getHouseType()) && nullToEmpty(house.getHouseType()).contains(dto.getHouseType().trim())) {
            score += 15;
            reasons.add("户型符合需求");
        }

        if (matchKeywordGroup(query, houseText,
                new String[]{"学校", "大学", "高校", "学生", "考研"},
                new String[]{"学校", "大学", "高校", "学府", "学院", "校区", "学生"})) {
            score += 20;
            reasons.add("位置或描述符合学生/学校相关需求");
        }

        if (matchKeywordGroup(query, houseText,
                new String[]{"地铁", "公交", "交通", "通勤", "出行"},
                new String[]{"地铁", "公交", "交通", "通勤", "出行", "车站"})) {
            score += 20;
            reasons.add("交通条件较符合需求");
        }

        if (matchKeywordGroup(query, houseText,
                new String[]{"一个人", "单人", "独居", "自己住"},
                new String[]{"一室", "单间", "主卧", "一室一厅", "一室一卫"})) {
            score += 15;
            reasons.add("户型适合单人居住");
        }

        if (matchKeywordGroup(query, houseText,
                new String[]{"精装", "装修", "拎包入住", "直接入住"},
                new String[]{"精装", "装修", "拎包入住", "直接入住", "公寓"})) {
            score += 15;
            reasons.add("装修条件较符合需求");
        }

        if (matchKeywordGroup(query, houseText,
                new String[]{"安静", "学习", "考研", "休息"},
                new String[]{"安静", "学习", "考研", "高校", "学府"})) {
            score += 10;
            reasons.add("居住环境较符合安静学习需求");
        }

        if (matchKeywordGroup(query, houseText,
                new String[]{"商圈", "购物", "生活方便", "配套"},
                new String[]{"商圈", "购物", "生活方便", "配套", "超市"})) {
            score += 10;
            reasons.add("周边生活配套较符合需求");
        }

        if (query.length() > 0 && houseText.contains(query)) {
            score += 30;
            reasons.add("房源文本与输入需求直接匹配");
        }

        if (reasons.isEmpty() && score > 0) {
            reasons.add("房源基本条件与需求较为接近");
        }

        return new ScoreResult(score, reasons);
    }

    private List<HouseRecommendVO> recommendByTfidf(HouseRecommendDTO dto, List<House> houseList, int topK) {
        if (houseList == null || houseList.isEmpty()) {
            return List.of();
        }

        String query = nullToEmpty(dto.getQuery());
        List<String> queryTokens = tokenizeForTfidf(query);

        if (queryTokens.isEmpty()) {
            return List.of();
        }

        List<List<String>> documentTokens = houseList.stream()
                .map(house -> tokenizeForTfidf(buildHouseText(house)))
                .toList();

        Map<String, Double> idfMap = buildIdfMap(documentTokens);
        Map<String, Double> queryVector = buildTfidfVector(queryTokens, idfMap, documentTokens.size());

        List<HouseRecommendVO> result = new ArrayList<>();

        for (int i = 0; i < houseList.size(); i++) {
            House house = houseList.get(i);
            List<String> tokens = documentTokens.get(i);

            if (tokens.isEmpty()) {
                continue;
            }

            Map<String, Double> houseVector = buildTfidfVector(tokens, idfMap, documentTokens.size());
            double similarity = cosineSimilarity(queryVector, houseVector);
            double score = similarity * 100;

            if (score <= 0) {
                continue;
            }

            HouseRecommendVO vo = new HouseRecommendVO();
            vo.setHouse(house);
            vo.setScore(roundScore(score));
            vo.setReason(buildTfidfReason(query, house, score));
            vo.setModelType("TFIDF");
            result.add(vo);
        }

        return result.stream()
                .sorted(Comparator.comparing(HouseRecommendVO::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private List<HouseRecommendVO> recommendByEmbedding(HouseRecommendDTO dto, List<House> houseList, int topK) {
        if (houseList == null || houseList.isEmpty()) {
            return List.of();
        }

        List<EmbeddingRecommendHouseDTO> candidateHouses = houseList.stream()
                .map(house -> {
                    EmbeddingRecommendHouseDTO item = new EmbeddingRecommendHouseDTO();
                    item.setId(house.getId());
                    item.setText(buildHouseText(house));
                    return item;
                })
                .toList();

        EmbeddingRecommendRequestDTO requestDTO = new EmbeddingRecommendRequestDTO();
        requestDTO.setQuery(dto.getQuery());
        requestDTO.setTopK(topK);
        requestDTO.setHouses(candidateHouses);

        List<EmbeddingRecommendResponseDTO> embeddingResults = embeddingRecommendClient.recommend(requestDTO);

        if (embeddingResults == null || embeddingResults.isEmpty()) {
            return List.of();
        }

        Map<Long, House> houseMap = houseList.stream()
                .filter(house -> house.getId() != null)
                .collect(Collectors.toMap(House::getId, house -> house, (a, b) -> a));

        List<HouseRecommendVO> result = new ArrayList<>();

        for (EmbeddingRecommendResponseDTO embeddingResult : embeddingResults) {
            if (embeddingResult.getHouseId() == null || embeddingResult.getScore() == null) {
                continue;
            }

            House house = houseMap.get(embeddingResult.getHouseId());
            if (house == null) {
                continue;
            }

            double similarity = embeddingResult.getScore();
            double displayScore = similarity * 100;

            HouseRecommendVO vo = new HouseRecommendVO();
            vo.setHouse(house);
            vo.setScore(roundScore(displayScore));
            vo.setReason("中文语义向量模型认为该房源与租房需求语义相似；语义相似度：" + roundScore(similarity));
            vo.setModelType("EMBEDDING");
            result.add(vo);
        }

        return result.stream()
                .limit(topK)
                .toList();
    }

    private Map<String, Double> buildIdfMap(List<List<String>> documentTokens) {
        Map<String, Integer> dfMap = new HashMap<>();
        int documentCount = Math.max(documentTokens.size(), 1);

        for (List<String> tokens : documentTokens) {
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                dfMap.put(token, dfMap.getOrDefault(token, 0) + 1);
            }
        }

        Map<String, Double> idfMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : dfMap.entrySet()) {
            double idf = Math.log((documentCount + 1.0) / (entry.getValue() + 1.0)) + 1.0;
            idfMap.put(entry.getKey(), idf);
        }

        return idfMap;
    }

    private Map<String, Double> buildTfidfVector(List<String> tokens, Map<String, Double> idfMap, int documentCount) {
        Map<String, Integer> tfMap = new HashMap<>();
        for (String token : tokens) {
            if (StringUtils.hasText(token)) {
                tfMap.put(token, tfMap.getOrDefault(token, 0) + 1);
            }
        }

        Map<String, Double> vector = new HashMap<>();
        double defaultIdf = Math.log((documentCount + 1.0) / 1.0) + 1.0;

        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            double tf = 1.0 + Math.log(entry.getValue());
            double idf = idfMap.getOrDefault(entry.getKey(), defaultIdf);
            vector.put(entry.getKey(), tf * idf);
        }

        return vector;
    }

    private double cosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        if (vectorA == null || vectorA.isEmpty() || vectorB == null || vectorB.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (double value : vectorA.values()) {
            normA += value * value;
        }

        for (double value : vectorB.values()) {
            normB += value * value;
        }

        for (Map.Entry<String, Double> entry : vectorA.entrySet()) {
            double bValue = vectorB.getOrDefault(entry.getKey(), 0.0);
            dotProduct += entry.getValue() * bValue;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<String> tokenizeForTfidf(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String normalized = text.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[，。！？、；：,.!?;:()（）【】\\[\\]{}]", "");

        List<String> tokens = new ArrayList<>();

        String[] domainKeywords = {
                "学校", "大学", "高校", "学生", "考研", "学习", "安静", "休息",
                "地铁", "公交", "交通", "通勤", "出行", "车站", "主路",
                "精装", "装修", "拎包入住", "直接入住", "公寓", "电梯",
                "一个人", "单人", "独居", "自己住", "一室", "一室一厅", "一室一卫", "单间", "主卧",
                "两室", "两室一厅", "三室", "三室两厅", "家庭", "一家人", "面积大",
                "商圈", "购物", "生活方便", "配套", "超市", "医院", "低预算", "预算"
        };

        for (String keyword : domainKeywords) {
            if (normalized.contains(keyword)) {
                tokens.add(keyword);
                tokens.add(keyword);
            }
        }

        Matcher numberMatcher = Pattern.compile("[a-zA-Z0-9]+").matcher(normalized);
        while (numberMatcher.find()) {
            tokens.add(numberMatcher.group());
        }

        Matcher chineseMatcher = Pattern.compile("[\\u4e00-\\u9fa5]+").matcher(normalized);
        while (chineseMatcher.find()) {
            String chineseText = chineseMatcher.group();

            for (int i = 0; i < chineseText.length() - 1; i++) {
                tokens.add(chineseText.substring(i, i + 2));
            }

            for (int i = 0; i < chineseText.length() - 2; i++) {
                tokens.add(chineseText.substring(i, i + 3));
            }
        }

        return tokens;
    }

    private String buildTfidfReason(String query, House house, double score) {
        String houseText = buildHouseText(house);
        List<String> matchedKeywords = findMatchedKeywords(query, houseText);

        List<String> reasons = new ArrayList<>();

        if (!matchedKeywords.isEmpty()) {
            reasons.add("与需求关键词匹配：" + String.join("、", matchedKeywords));
        } else {
            reasons.add("房源文本与租房需求具有一定文本相似度");
        }

        reasons.add("TF-IDF相似度得分：" + roundScore(score));

        return String.join("；", reasons);
    }

    private List<String> findMatchedKeywords(String query, String houseText) {
        String[] keywords = {
                "学校", "大学", "高校", "学生", "考研", "学习", "安静",
                "地铁", "公交", "交通", "通勤", "车站",
                "精装", "装修", "拎包入住", "直接入住", "公寓", "电梯",
                "一个人", "单人", "独居", "一室", "单间", "主卧",
                "两室", "三室", "家庭", "一家人", "商圈", "购物", "生活方便", "医院"
        };

        List<String> matched = new ArrayList<>();

        for (String keyword : keywords) {
            if (query.contains(keyword) && houseText.contains(keyword)) {
                matched.add(keyword);
            }
        }

        return matched.stream().distinct().limit(6).toList();
    }

    private void saveRecommendRecord(HouseRecommendDTO dto, List<HouseRecommendVO> result, long responseTime, String modelType) {
        RecommendRecord record = new RecommendRecord();
        record.setUserId(null);
        record.setQueryText(dto.getQuery());
        record.setModelType(modelType);
        record.setResultHouseIds(buildResultHouseIds(result));
        record.setResponseTimeMs((int) responseTime);
        record.setCreateTime(LocalDateTime.now());

        recommendRecordMapper.insert(record);
    }

    private String buildResultHouseIds(List<HouseRecommendVO> result) {
        if (result == null || result.isEmpty()) {
            return "";
        }

        return result.stream()
                .filter(item -> item.getHouse() != null && item.getHouse().getId() != null)
                .map(item -> String.valueOf(item.getHouse().getId()))
                .collect(Collectors.joining(","));
    }

    private boolean matchKeywordGroup(String query, String houseText, String[] queryKeywords, String[] houseKeywords) {
        boolean queryMatched = containsAny(query, queryKeywords);
        boolean houseMatched = containsAny(houseText, houseKeywords);
        return queryMatched && houseMatched;
    }

    private boolean containsAny(String text, String[] keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String buildHouseText(House house) {
        return String.join(" ",
                nullToEmpty(house.getTitle()),
                nullToEmpty(house.getCity()),
                nullToEmpty(house.getArea()),
                nullToEmpty(house.getAddress()),
                nullToEmpty(house.getHouseType()),
                house.getSquare() == null ? "" : house.getSquare() + "平方米",
                house.getRentPrice() == null ? "" : house.getRentPrice() + "元",
                nullToEmpty(house.getFloor()),
                nullToEmpty(house.getDescription())
        );
    }

    private BigDecimal extractMaxRent(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        Pattern pattern = Pattern.compile("(不超过|不高于|低于|小于|少于|预算|最多|最高|以内)?\\s*(\\d+(?:\\.\\d+)?)\\s*(元|块|人民币)");
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            return new BigDecimal(matcher.group(2));
        }

        return null;
    }

    private String normalizeModelType(String modelType) {
        if (!StringUtils.hasText(modelType)) {
            return "RULE";
        }

        String value = modelType.trim().toUpperCase();
        if ("TF-IDF".equals(value)) {
            return "TFIDF";
        }

        return value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double roundScore(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class ScoreResult {

        private final double score;

        private final List<String> reasons;

        public ScoreResult(double score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }

        public double getScore() {
            return score;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}