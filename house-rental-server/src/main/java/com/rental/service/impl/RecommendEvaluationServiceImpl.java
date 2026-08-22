package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.dto.HouseRecommendDTO;
import com.rental.dto.RecommendEvalDTO;
import com.rental.entity.RecommendEvalLabel;
import com.rental.entity.RecommendEvalQuery;
import com.rental.mapper.RecommendEvalLabelMapper;
import com.rental.mapper.RecommendEvalQueryMapper;
import com.rental.service.RecommendEvaluationService;
import com.rental.service.RecommendService;
import com.rental.vo.HouseRecommendVO;
import com.rental.vo.RecommendEvalDetailVO;
import com.rental.vo.RecommendEvalSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendEvaluationServiceImpl implements RecommendEvaluationService {

    private final RecommendEvalQueryMapper recommendEvalQueryMapper;

    private final RecommendEvalLabelMapper recommendEvalLabelMapper;

    private final RecommendService recommendService;

    public RecommendEvaluationServiceImpl(RecommendEvalQueryMapper recommendEvalQueryMapper,
                                          RecommendEvalLabelMapper recommendEvalLabelMapper,
                                          RecommendService recommendService) {
        this.recommendEvalQueryMapper = recommendEvalQueryMapper;
        this.recommendEvalLabelMapper = recommendEvalLabelMapper;
        this.recommendService = recommendService;
    }

    @Override
    public RecommendEvalSummaryVO evaluate(RecommendEvalDTO dto) {
        int topK = dto.getTopK() == null || dto.getTopK() <= 0 ? 5 : Math.min(dto.getTopK(), 20);
        String modelType = normalizeModelType(dto.getModelType());

        LambdaQueryWrapper<RecommendEvalQuery> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getSceneType())) {
            queryWrapper.eq(RecommendEvalQuery::getSceneType, dto.getSceneType().trim());
        }
        queryWrapper.orderByAsc(RecommendEvalQuery::getId);

        List<RecommendEvalQuery> queryList = recommendEvalQueryMapper.selectList(queryWrapper);

        List<RecommendEvalDetailVO> details = new ArrayList<>();

        for (RecommendEvalQuery evalQuery : queryList) {
            RecommendEvalDetailVO detailVO = evaluateOneQuery(evalQuery, modelType, topK);
            details.add(detailVO);
        }

        RecommendEvalSummaryVO summaryVO = new RecommendEvalSummaryVO();
        summaryVO.setModelType(modelType);
        summaryVO.setTopK(topK);
        summaryVO.setQueryCount(details.size());
        summaryVO.setDetails(details);

        summaryVO.setAvgPrecision(round(avg(details.stream().map(RecommendEvalDetailVO::getPrecision).toList())));
        summaryVO.setAvgRecall(round(avg(details.stream().map(RecommendEvalDetailVO::getRecall).toList())));
        summaryVO.setAvgF1(round(avg(details.stream().map(RecommendEvalDetailVO::getF1).toList())));
        summaryVO.setAvgNdcg(round(avg(details.stream().map(RecommendEvalDetailVO::getNdcg).toList())));
        summaryVO.setAvgResponseTimeMs(round(avg(details.stream()
                .map(item -> item.getResponseTimeMs() == null ? 0.0 : item.getResponseTimeMs().doubleValue())
                .toList())));

        return summaryVO;
    }

    private RecommendEvalDetailVO evaluateOneQuery(RecommendEvalQuery evalQuery, String modelType, int topK) {
        List<RecommendEvalLabel> labels = recommendEvalLabelMapper.selectList(
                new LambdaQueryWrapper<RecommendEvalLabel>()
                        .eq(RecommendEvalLabel::getQueryId, evalQuery.getId())
        );

        Map<Long, Integer> relevanceMap = labels.stream()
                .collect(Collectors.toMap(
                        RecommendEvalLabel::getHouseId,
                        item -> item.getRelevanceScore() == null ? 1 : item.getRelevanceScore(),
                        Math::max
                ));

        List<Long> relevantHouseIds = new ArrayList<>(relevanceMap.keySet());

        HouseRecommendDTO recommendDTO = new HouseRecommendDTO();
        recommendDTO.setQuery(evalQuery.getQueryText());
        recommendDTO.setModelType(modelType);
        recommendDTO.setTopK(topK);
        recommendDTO.setUseLlmParse(false);

        long startTime = System.currentTimeMillis();
        List<HouseRecommendVO> recommendResult = recommendService.recommendHouse(recommendDTO);
        long responseTime = System.currentTimeMillis() - startTime;

        List<Long> recommendedHouseIds = recommendResult.stream()
                .filter(item -> item.getHouse() != null && item.getHouse().getId() != null)
                .map(item -> item.getHouse().getId())
                .limit(topK)
                .toList();

        Set<Long> relevantSet = new HashSet<>(relevantHouseIds);

        int hitCount = 0;
        for (Long houseId : recommendedHouseIds) {
            if (relevantSet.contains(houseId)) {
                hitCount++;
            }
        }

        double precision = topK == 0 ? 0.0 : (double) hitCount / topK;
        double recall = relevantSet.isEmpty() ? 0.0 : (double) hitCount / relevantSet.size();
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
        double ndcg = calculateNdcg(recommendedHouseIds, relevanceMap, topK);

        RecommendEvalDetailVO detailVO = new RecommendEvalDetailVO();
        detailVO.setQueryId(evalQuery.getId());
        detailVO.setQueryText(evalQuery.getQueryText());
        detailVO.setSceneType(evalQuery.getSceneType());
        detailVO.setRecommendedHouseIds(recommendedHouseIds);
        detailVO.setRelevantHouseIds(relevantHouseIds);
        detailVO.setHitCount(hitCount);
        detailVO.setRelevantCount(relevantSet.size());
        detailVO.setPrecision(round(precision));
        detailVO.setRecall(round(recall));
        detailVO.setF1(round(f1));
        detailVO.setNdcg(round(ndcg));
        detailVO.setResponseTimeMs(responseTime);

        return detailVO;
    }

    private double calculateNdcg(List<Long> recommendedHouseIds, Map<Long, Integer> relevanceMap, int topK) {
        double dcg = 0.0;

        for (int i = 0; i < Math.min(topK, recommendedHouseIds.size()); i++) {
            Long houseId = recommendedHouseIds.get(i);
            int relevance = relevanceMap.getOrDefault(houseId, 0);
            dcg += gain(relevance) / log2(i + 2);
        }

        List<Integer> idealRelevanceList = relevanceMap.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(topK)
                .toList();

        double idcg = 0.0;
        for (int i = 0; i < idealRelevanceList.size(); i++) {
            idcg += gain(idealRelevanceList.get(i)) / log2(i + 2);
        }

        if (idcg == 0.0) {
            return 0.0;
        }

        return dcg / idcg;
    }

    private double gain(int relevance) {
        return Math.pow(2, relevance) - 1;
    }

    private double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    private double avg(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        return values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private Double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String normalizeModelType(String modelType) {
        if (!StringUtils.hasText(modelType)) {
            return "RULE";
        }
        return modelType.trim().toUpperCase();
    }
}