package com.muad.etims.service;

import com.muad.etims.entity.KraSystemAudit;
import com.muad.etims.entity.KraSystemEventType;
import com.muad.etims.repository.KraSystemAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class KraAlertService {

    private final KraSystemAuditRepository auditRepository;
    
    // Threshold for consecutive failures before alerting
    private static final int FAILURE_THRESHOLD = 3;
    
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private boolean isDown = false;

    @Transactional
    public void recordFailure(String errorMessage) {
        int failures = consecutiveFailures.incrementAndGet();
        log.warn("KRA API Failure recorded. Consecutive failures: {}", failures);

        if (failures >= FAILURE_THRESHOLD && !isDown) {
            isDown = true;
            String alertMsg = "🚨 KRA GavaConnect is DOWN! Threshold of " + FAILURE_THRESHOLD + " consecutive failures reached. Error: " + errorMessage;
            
            // 1. Save to PostgreSQL Audit Log
            KraSystemAudit audit = KraSystemAudit.builder()
                    .eventType(KraSystemEventType.DOWNTIME_DETECTED)
                    .message(alertMsg)
                    .build();
            auditRepository.save(audit);

            // 2. Dispatch Alert (Placeholder for Slack/Email webhook)
            sendSlackAlert(alertMsg);
        }
    }

    @Transactional
    public void recordSuccess() {
        // If it was down and now succeeded, trigger recovery!
        if (isDown) {
            isDown = false;
            String recoverMsg = "✅ KRA GavaConnect has RECOVERED! The system is processing the backlog.";
            
            // 1. Save to PostgreSQL Audit Log
            KraSystemAudit audit = KraSystemAudit.builder()
                    .eventType(KraSystemEventType.SYSTEM_RECOVERED)
                    .message(recoverMsg)
                    .build();
            auditRepository.save(audit);

            // 2. Dispatch Alert
            sendSlackAlert(recoverMsg);
        }
        
        // Reset counter
        consecutiveFailures.set(0);
    }

    private void sendSlackAlert(String message) {
        // TODO: Implement actual RestClient POST to Slack Webhook URL or JavaMailSender
        log.error("=================================================");
        log.error(">>> SLACK ALERT DISPATCHED: {}", message);
        log.error("=================================================");
    }
}
