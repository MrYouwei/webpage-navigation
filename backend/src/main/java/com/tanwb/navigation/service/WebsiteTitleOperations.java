package com.tanwb.navigation.service;

import java.util.Map;

public interface WebsiteTitleOperations {
    Map<String, String> fetchTitle(String url);
}
