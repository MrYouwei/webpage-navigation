package com.tanwb.navigation.service;

import com.tanwb.navigation.dto.SaveNavResponse;
import java.util.Map;

public interface NavDataOperations {
    Map<String, Object> getOrCreateData(Long userId);

    SaveNavResponse saveData(Long userId, Map<String, Object> navData);

    void flushDirtyNavData();
}
