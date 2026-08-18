package com.tanwb.navigation.dto;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class SaveNavRequest {
    @NotNull(message = "导航数据不能为空")
    private Map<String, Object> navData;

    private String clientVersion;

    public Map<String, Object> getNavData() {
        return navData;
    }

    public void setNavData(Map<String, Object> navData) {
        this.navData = navData;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }
}
