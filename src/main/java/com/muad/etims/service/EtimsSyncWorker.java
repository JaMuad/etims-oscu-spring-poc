package com.muad.etims.service;

import com.muad.etims.entity.EtimsStatus;
import com.muad.etims.entity.Transaction;
import com.muad.etims.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtimsSyncWorker {

    private final TransactionRepository transactionRepository;
    private final KraInitializationService kraInitializationService;
    private final KraAlertService kraAlertService;

    private static final int MAX_RETRIES = 5;

    /**
     * Polls the database every 30 seconds for pending or failed transactions and attempts to sync them.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processPendingTransactions() {
        log.info("Checking for pending transactions...");
        List<Transaction> pendingTx = transactionRepository.findTop50ByEtimsStatusInOrderByCreatedAtAsc(
                List.of(EtimsStatus.PENDING, EtimsStatus.FAILED_RETRY)
        );

        if (pendingTx.isEmpty()) {
            return;
        }

        log.info("Found {} transactions to sync with KRA", pendingTx.size());

        try {
            // Step 1: Initialize device to check connectivity (and fetch cmcKey in the future)
            // If KRA is down, this will throw an exception and we abort the batch gracefully.
            kraInitializationService.initializeDevice();
            
            // If we succeed, KRA is UP!
            kraAlertService.recordSuccess();

            // Step 2: Process each transaction
            for (Transaction tx : pendingTx) {
                syncTransaction(tx);
            }

        } catch (Exception e) {
            log.error("KRA Sync Batch aborted due to connectivity issues: {}", e.getMessage());
            kraAlertService.recordFailure(e.getMessage());
        }
    }

    private void syncTransaction(Transaction tx) {
        try {
            tx.setLastAttemptAt(LocalDateTime.now());
            tx.setRetryCount(tx.getRetryCount() + 1);

            // TODO: Here is where we will map Transaction to KraInitializationRequest
            // and use RestClient to send it to the eTIMS initialization endpoint.
            
            // For now, in this PoC, we will just simulate a successful push
            log.info("Simulating successful sync for Invoice: {}", tx.getInvoiceNumber());
            
            tx.setEtimsStatus(EtimsStatus.SYNCED);
            tx.setKraReceiptNumber("KRA-" + System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("Failed to sync transaction {}: {}", tx.getId(), e.getMessage());
            
            if (tx.getRetryCount() >= MAX_RETRIES) {
                log.error("Transaction {} exhausted retries. Marking FATAL_ERROR.", tx.getId());
                tx.setEtimsStatus(EtimsStatus.FATAL_ERROR);
            } else {
                tx.setEtimsStatus(EtimsStatus.FAILED_RETRY);
            }
        } finally {
            transactionRepository.save(tx);
        }
    }
}
