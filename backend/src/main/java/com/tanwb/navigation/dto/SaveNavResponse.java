package com.tanwb.navigation.dto;

public class SaveNavResponse {
    private String serverVersion;
    private String savedAt;

    public SaveNavResponse() {
    }

    public SaveNavResponse(String serverVersion, String savedAt) {
        this.serverVersion = serverVersion;
        this.savedAt = savedAt;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(String savedAt) {
        this.savedAt = savedAt;
    }
}
