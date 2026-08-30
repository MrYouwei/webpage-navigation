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
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NavDataService implements NavDataOperations {
    private static final Logger log = LoggerFactory.getLogger(NavDataService.class);
    private static final String NAV_CACHE_KEY_PREFIX = "nav:";
    private static final String DIRTY_USERS_KEY = "nav:dirty-users";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final NavDataMapper navDataMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.nav.flush-silent-window-ms:10000}")
    private long flushSilentWindowMs;

    @Value("${app.nav.flush-batch-size:100}")
    private int flushBatchSize;

    public NavDataService(NavDataMapper navDataMapper, ObjectMapper objectMapper, StringRedisTemplate stringRedisTemplate) {
        this.navDataMapper = navDataMapper;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Map<String, Object> getOrCreateData(Long userId) {
        String cachedJson = getCachedNavJson(userId);
        if (cachedJson != null) {
            return parseNavJson(cachedJson);
        }

        NavData navData = findByUserId(userId);
        if (navData == null) {
            Map<String, Object> defaultData = defaultNavData();
            saveNew(userId, defaultData);
            cacheNavJson(userId, toJson(defaultData));
            return defaultData;
        }

        Map<String, Object> parsed = parseNavJson(navData.getNavJson());
        cacheNavJson(userId, toJson(parsed));
        return parsed;
    }

    @Override
    public SaveNavResponse saveData(Long userId, Map<String, Object> navData) {
        Map<String, Object> normalized = normalizeNavData(navData);
        normalized.put("updatedAt", Instant.now().toString());
        String json = toJson(normalized);

        if (!cacheLatestNavData(userId, json)) {
            upsertNavJson(userId, json);
        }
        return new SaveNavResponse(String.valueOf(normalized.get("version")), String.valueOf(normalized.get("updatedAt")));
    }

    @Override
    public void flushDirtyNavData() {
        long cutoff = System.currentTimeMillis() - flushSilentWindowMs;
        Set<ZSetOperations.TypedTuple<String>> dirtyUsers;
        try {
            dirtyUsers = stringRedisTemplate.opsForZSet()
                    .rangeByScoreWithScores(DIRTY_USERS_KEY, 0, cutoff, 0, flushBatchSize);
        } catch (Exception ex) {
            log.warn("Failed to scan dirty navigation users from Redis", ex);
            return;
        }

        if (dirtyUsers == null || dirtyUsers.isEmpty()) {
            return;
        }

        for (ZSetOperations.TypedTuple<String> dirtyUser : dirtyUsers) {
            flushDirtyNavData(dirtyUser);
        }
    }

    private void flushDirtyNavData(ZSetOperations.TypedTuple<String> dirtyUser) {
        String userIdText = dirtyUser.getValue();
        Double processedScore = dirtyUser.getScore();
        if (userIdText == null || processedScore == null) {
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdText);
        } catch (NumberFormatException ex) {
            log.warn("Invalid dirty navigation user id: {}", userIdText);
            stringRedisTemplate.opsForZSet().remove(DIRTY_USERS_KEY, userIdText);
            return;
        }

        String json = getCachedNavJson(userId);
        if (json == null) {
            removeDirtyUserIfUnchanged(userIdText, processedScore);
            return;
        }

        try {
            upsertNavJson(userId, toJson(parseNavJson(json)));
            removeDirtyUserIfUnchanged(userIdText, processedScore);
        } catch (Exception ex) {
            log.warn("Failed to flush navigation data for user {}", userId, ex);
        }
    }

    private boolean cacheLatestNavData(Long userId, String json) {
        try {
            stringRedisTemplate.opsForValue().set(navCacheKey(userId), json);
            stringRedisTemplate.opsForZSet().add(DIRTY_USERS_KEY, String.valueOf(userId), System.currentTimeMillis());
            return true;
        } catch (Exception ex) {
            log.warn("Failed to cache navigation data for user {}, fallback to database", userId, ex);
            return false;
        }
    }

    private void cacheNavJson(Long userId, String json) {
        try {
            stringRedisTemplate.opsForValue().set(navCacheKey(userId), json);
        } catch (Exception ex) {
            log.warn("Failed to cache navigation data for user {}", userId, ex);
        }
    }

    private String getCachedNavJson(Long userId) {
        try {
            return stringRedisTemplate.opsForValue().get(navCacheKey(userId));
        } catch (Exception ex) {
            log.warn("Failed to read navigation data cache for user {}", userId, ex);
            return null;
        }
    }

    private void removeDirtyUserIfUnchanged(String userIdText, Double processedScore) {
        try {
            Double currentScore = stringRedisTemplate.opsForZSet().score(DIRTY_USERS_KEY, userIdText);
            if (currentScore != null && Double.compare(currentScore, processedScore) == 0) {
                stringRedisTemplate.opsForZSet().remove(DIRTY_USERS_KEY, userIdText);
            }
        } catch (Exception ex) {
            log.warn("Failed to remove dirty navigation user {}", userIdText, ex);
        }
    }

    private String navCacheKey(Long userId) {
        return NAV_CACHE_KEY_PREFIX + userId;
    }

    private void upsertNavJson(Long userId, String json) {
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
