package com.tanwb.navigation.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NavDataFlushScheduler {
    private final NavDataOperations navDataService;

    public NavDataFlushScheduler(NavDataOperations navDataService) {
        this.navDataService = navDataService;
    }

    @Scheduled(fixedDelayString = "${app.nav.flush-interval-ms:5000}")
    public void flushDirtyNavData() {
        navDataService.flushDirtyNavData();
    }
}
