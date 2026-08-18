package com.tanwb.navigation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanwb.navigation.dto.SaveNavResponse;
import com.tanwb.navigation.entity.NavData;
import com.tanwb.navigation.exception.ApiException;
import com.tanwb.navigation.mapper.NavDataMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavDataService implements NavDataOperations {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final NavDataMapper navDataMapper;
    private final ObjectMapper objectMapper;

    public NavDataService(NavDataMapper navDataMapper, ObjectMapper objectMapper) {
        this.navDataMapper = navDataMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public Map<String, Object> getOrCreateData(Long userId) {
        NavData navData = findByUserId(userId);
        if (navData == null) {
            Map<String, Object> defaultData = defaultNavData();
            saveNew(userId, defaultData);
            return defaultData;
        }
        return parseNavJson(navData.getNavJson());
    }

    @Transactional
    @Override
    public SaveNavResponse saveData(Long userId, Map<String, Object> navData) {
        Map<String, Object> normalized = normalizeNavData(navData);
        normalized.put("updatedAt", Instant.now().toString());
        String json = toJson(normalized);

        NavData existing = findByUserId(userId);
        if (existing == null) {
            NavData row = new NavData();
            row.setUserId(userId);
            row.setNavJson(json);
            LocalDateTime now = LocalDateTime.now();
            row.setCreateTime(now);
            row.setUpdateTime(now);
            navDataMapper.insert(row);
        } else {
            existing.setNavJson(json);
            existing.setUpdateTime(LocalDateTime.now());
            navDataMapper.updateById(existing);
        }
        return new SaveNavResponse(String.valueOf(normalized.get("version")), String.valueOf(normalized.get("updatedAt")));
    }

    private void saveNew(Long userId, Map<String, Object> navData) {
        NavData row = new NavData();
        row.setUserId(userId);
        row.setNavJson(toJson(navData));
        LocalDateTime now = LocalDateTime.now();
        row.setCreateTime(now);
        row.setUpdateTime(now);
        navDataMapper.insert(row);
    }

    private NavData findByUserId(Long userId) {
        return navDataMapper.selectOne(new LambdaQueryWrapper<NavData>()
                .eq(NavData::getUserId, userId)
                .last("LIMIT 1"));
    }

    private Map<String, Object> parseNavJson(String navJson) {
        try {
            return normalizeNavData(objectMapper.readValue(navJson, MAP_TYPE));
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "导航数据解析失败");
        }
    }

    private String toJson(Map<String, Object> navData) {
        try {
            return objectMapper.writeValueAsString(navData);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "导航数据格式错误");
        }
    }

    private Map<String, Object> defaultNavData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", "1.0");
        data.put("updatedAt", Instant.now().toString());
        data.put("groups", new ArrayList<>());
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeNavData(Map<String, Object> navData) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Object version = navData.getOrDefault("version", "1.0");
        Object updatedAt = navData.getOrDefault("updatedAt", Instant.now().toString());
        Object groups = navData.get("groups");
        if (groups == null && navData.get("children") instanceof Iterable<?>) {
            groups = navData.get("children");
        }
        if (groups == null) {
            groups = new ArrayList<>();
        }
        normalized.put("version", version);
        normalized.put("updatedAt", updatedAt);
        normalized.put("groups", groups);
        return normalized;
    }
}
