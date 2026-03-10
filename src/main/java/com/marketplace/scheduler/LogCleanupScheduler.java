package com.marketplace.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    // по умолчанию: каждый день в 03:30
    @Scheduled(cron = "${logs.cleanup.cron:0 30 3 * * *}")
    public void clearOldLogs() {
        jdbcTemplate.update("CALL clear_old_logs()");
    }
}