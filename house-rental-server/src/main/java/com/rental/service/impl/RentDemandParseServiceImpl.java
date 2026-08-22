package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.client.DeepSeekDemandParseClient;
import com.rental.dto.RentDemandParseDTO;
import com.rental.entity.House;
import com.rental.mapper.HouseMapper;
import com.rental.service.RentDemandParseService;
import com.rental.vo.RentDemandParseVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RentDemandParseServiceImpl implements RentDemandParseService {

    private final DeepSeekDemandParseClient deepSeekDemandParseClient;

    private final HouseMapper houseMapper;

    public RentDemandParseServiceImpl(DeepSeekDemandParseClient deepSeekDemandParseClient,
                                      HouseMapper houseMapper) {
        this.deepSeekDemandParseClient = deepSeekDemandParseClient;
        this.houseMapper = houseMapper;
    }

    @Override
    public RentDemandParseVO parseDemand(RentDemandParseDTO dto) {
        String query = dto == null || dto.getQuery() == null ? "" : dto.getQuery().trim();

        RentDemandParseVO ruleResult = parseByRule(query);

        if (!Boolean.FALSE.equals(dto == null ? null : dto.getUseLlm())) {
            Optional<RentDemandParseVO> llmResult = deepSeekDemandParseClient.parseDemand(query);
            if (llmResult.isPresent()) {
                return mergeLlmAndRuleResult(llmResult.get(), ruleResult);
            }
        }

        return ruleResult;
    }

    /**
     * 本地规则解析。
     * 城市和区域不再写死，而是从 house 表现有房源中动态读取。
     */
    private RentDemandParseVO parseByRule(String query) {
        RentDemandParseVO vo = new RentDemandParseVO();
        vo.setQuery(query);
        vo.setMaxRent(extractMaxRent(query));
        vo.setMinSquare(extractMinSquare(query));
        vo.setCity(extractCity(query));
        vo.setArea(extractArea(query, vo.getCity()));
        vo.setHouseType(extractHouseType(query));
        vo.setParseType("RULE_PARSE");

        Set<String> scenes = new LinkedHashSet<>();
        Set<String> keywords = new LinkedHashSet<>();

        parseStudentScene(query, scenes, keywords, vo);
        parseTrafficScene(query, scenes, keywords, vo);
        parseDecorationScene(query, scenes, keywords, vo);
        parseQuietScene(query, scenes, keywords, vo);
        parseFamilyScene(query, scenes, keywords, vo);
        parseLocation(query, keywords);

        if (StringUtils.hasText(vo.getCity())) {
            keywords.add(vo.getCity());
        }

        if (StringUtils.hasText(vo.getArea())) {
            keywords.add(vo.getArea());
        }

        vo.setScenes(scenes.stream().toList());
        vo.setKeywords(keywords.stream().toList());

        return vo;
    }

    /**
     * DeepSeek 解析成功时，仍然使用本地规则结果补充城市、区域等字段。
     * 这样可以避免 DeepSeek 返回“北京市”，而数据库里保存的是“北京”导致筛选不到数据。
     */
    private RentDemandParseVO mergeLlmAndRuleResult(RentDemandParseVO llmResult, RentDemandParseVO ruleResult) {
        if (llmResult == null) {
            return ruleResult;
        }

        if (ruleResult == null) {
            return llmResult;
        }

        if (!StringUtils.hasText(llmResult.getQuery())) {
            llmResult.setQuery(ruleResult.getQuery());
        }

        // 城市、区域优先使用从数据库中动态匹配到的值，保证能和 house.city、house.area 精确对应。
        if (StringUtils.hasText(ruleResult.getCity())) {
            llmResult.setCity(ruleResult.getCity());
        }

        if (StringUtils.hasText(ruleResult.getArea())) {
            llmResult.setArea(ruleResult.getArea());
        }

        if (llmResult.getMaxRent() == null) {
            llmResult.setMaxRent(ruleResult.getMaxRent());
        }

        if (llmResult.getMinSquare() == null) {
            llmResult.setMinSquare(ruleResult.getMinSquare());
        }

        if (!StringUtils.hasText(llmResult.getHouseType())) {
            llmResult.setHouseType(ruleResult.getHouseType());
        }

        llmResult.setScenes(mergeStringList(llmResult.getScenes(), ruleResult.getScenes()));
        llmResult.setKeywords(mergeStringList(llmResult.getKeywords(), ruleResult.getKeywords()));

        llmResult.setNeedQuiet(Boolean.TRUE.equals(llmResult.getNeedQuiet()) || Boolean.TRUE.equals(ruleResult.getNeedQuiet()));
        llmResult.setNeedTraffic(Boolean.TRUE.equals(llmResult.getNeedTraffic()) || Boolean.TRUE.equals(ruleResult.getNeedTraffic()));
        llmResult.setNeedDecoration(Boolean.TRUE.equals(llmResult.getNeedDecoration()) || Boolean.TRUE.equals(ruleResult.getNeedDecoration()));
        llmResult.setStudentFriendly(Boolean.TRUE.equals(llmResult.getStudentFriendly()) || Boolean.TRUE.equals(ruleResult.getStudentFriendly()));
        llmResult.setFamilyFriendly(Boolean.TRUE.equals(llmResult.getFamilyFriendly()) || Boolean.TRUE.equals(ruleResult.getFamilyFriendly()));

        if (!StringUtils.hasText(llmResult.getParseType())) {
            llmResult.setParseType("LLM_PARSE");
        }

        return llmResult;
    }

    private List<String> mergeStringList(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();

        if (first != null) {
            for (String item : first) {
                if (StringUtils.hasText(item)) {
                    merged.add(item.trim());
                }
            }
        }

        if (second != null) {
            for (String item : second) {
                if (StringUtils.hasText(item)) {
                    merged.add(item.trim());
                }
            }
        }

        return new ArrayList<>(merged);
    }

    private void parseStudentScene(String query, Set<String> scenes, Set<String> keywords, RentDemandParseVO vo) {
        if (containsAny(query, "学生", "学校", "大学", "高校", "学院", "校区")) {
            scenes.add("学生");
            keywords.add("学校");
            keywords.add("大学");
            keywords.add("学生");
            vo.setStudentFriendly(true);
        }
    }

    private void parseTrafficScene(String query, Set<String> scenes, Set<String> keywords, RentDemandParseVO vo) {
        if (containsAny(query, "交通", "公交", "地铁", "车站", "主路", "通勤", "出行方便")) {
            scenes.add("交通");
            keywords.add("交通");
            keywords.add("公交");
            keywords.add("通勤");
            vo.setNeedTraffic(true);
        }
    }

    private void parseDecorationScene(String query, Set<String> scenes, Set<String> keywords, RentDemandParseVO vo) {
        if (containsAny(query, "精装", "装修", "拎包入住", "直接入住", "公寓", "电梯")) {
            scenes.add("装修");
            keywords.add("精装");
            keywords.add("装修");
            keywords.add("拎包入住");
            vo.setNeedDecoration(true);
        }
    }

    private void parseQuietScene(String query, Set<String> scenes, Set<String> keywords, RentDemandParseVO vo) {
        if (containsAny(query, "安静", "考研", "学习", "复习", "休息", "不吵")) {
            scenes.add("学习环境");
            keywords.add("安静");
            keywords.add("考研");
            keywords.add("学习");
            vo.setNeedQuiet(true);
        }
    }

    private void parseFamilyScene(String query, Set<String> scenes, Set<String> keywords, RentDemandParseVO vo) {
        if (containsAny(query, "家庭", "一家人", "孩子", "两室", "三室", "面积大", "配套齐全", "生活方便")) {
            scenes.add("家庭居住");
            keywords.add("家庭");
            keywords.add("一家人");
            keywords.add("生活方便");
            vo.setFamilyFriendly(true);
        }
    }

    private void parseLocation(String query, Set<String> keywords) {
        if (containsAny(query, "商圈", "购物", "超市")) {
            keywords.add("商圈");
            keywords.add("购物");
            keywords.add("超市");
        }

        if (containsAny(query, "医院")) {
            keywords.add("医院");
        }

        if (containsAny(query, "公园")) {
            keywords.add("公园");
        }
    }

    /**
     * 从数据库现有房源城市中动态识别城市。
     * 例如 house.city 中有“杭州”，用户输入“杭州地区的房子”即可识别为 city=杭州。
     */
    private String extractCity(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        List<String> cityList = queryDistinctCityList();
        return matchLocationName(query, cityList);
    }

    /**
     * 从数据库现有房源区域中动态识别区域。
     * 如果已经识别出城市，则优先在该城市下匹配区域，减少不同城市同名区域造成的干扰。
     */
    private String extractArea(String query, String city) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        List<String> areaList = queryDistinctAreaList(city);
        return matchLocationName(query, areaList);
    }

    private List<String> queryDistinctCityList() {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(House::getCity)
                .isNotNull(House::getCity)
                .ne(House::getCity, "")
                .groupBy(House::getCity);

        return houseMapper.selectList(wrapper).stream()
                .map(House::getCity)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> queryDistinctAreaList(String city) {
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(House::getArea)
                .isNotNull(House::getArea)
                .ne(House::getArea, "");

        if (StringUtils.hasText(city)) {
            wrapper.eq(House::getCity, city.trim());
        }

        wrapper.groupBy(House::getArea);

        return houseMapper.selectList(wrapper).stream()
                .map(House::getArea)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String matchLocationName(String query, List<String> locationList) {
        if (!StringUtils.hasText(query) || locationList == null || locationList.isEmpty()) {
            return null;
        }

        String normalizedQuery = normalizeLocationText(query);

        return locationList.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .filter(location -> locationMatched(normalizedQuery, location))
                .findFirst()
                .orElse(null);
    }

    private boolean locationMatched(String normalizedQuery, String location) {
        if (!StringUtils.hasText(normalizedQuery) || !StringUtils.hasText(location)) {
            return false;
        }

        for (String candidate : buildLocationCandidates(location)) {
            String normalizedCandidate = normalizeLocationText(candidate);
            if (normalizedCandidate.length() >= 2 && normalizedQuery.contains(normalizedCandidate)) {
                return true;
            }
        }

        return false;
    }

    private List<String> buildLocationCandidates(String location) {
        Set<String> candidates = new LinkedHashSet<>();
        String text = location == null ? "" : location.trim();

        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        candidates.add(text);

        String withoutCommonSuffix = text
                .replaceAll("(特别行政区|自治州|自治县|地区|新区|市辖区)$", "")
                .replaceAll("(省|市|区|县|州)$", "");

        if (StringUtils.hasText(withoutCommonSuffix)) {
            candidates.add(withoutCommonSuffix);
        }

        return new ArrayList<>(candidates);
    }

    private String normalizeLocationText(String text) {
        if (text == null) {
            return "";
        }

        return text.trim()
                .replaceAll("\\s+", "")
                .replace("地区", "")
                .replace("附近", "")
                .replace("周边", "")
                .replace("房屋", "")
                .replace("房子", "")
                .replace("租房", "");
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

    private BigDecimal extractMinSquare(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        Pattern pattern = Pattern.compile("(至少|不低于|大于|超过)?\\s*(\\d+(?:\\.\\d+)?)\\s*(平|平方|平方米|㎡)");
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            return new BigDecimal(matcher.group(2));
        }

        return null;
    }

    private String extractHouseType(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        if (query.contains("三室两厅")) {
            return "三室两厅";
        }

        if (query.contains("三室")) {
            return "三室";
        }

        if (query.contains("两室一厅")) {
            return "两室一厅";
        }

        if (query.contains("两室")) {
            return "两室";
        }

        if (query.contains("一室一厅")) {
            return "一室一厅";
        }

        if (query.contains("一室一卫")) {
            return "一室一卫";
        }

        if (query.contains("一室")) {
            return "一室";
        }

        if (query.contains("单间")) {
            return "单间";
        }

        if (query.contains("主卧")) {
            return "主卧";
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
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
}
