package com.muad.etims.config;

import com.muad.etims.entity.EtimsStatus;
import com.muad.etims.entity.Transaction;
import com.muad.etims.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataSeeder implements CommandLineRunner {

    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) {
        if (transactionRepository.count() == 0) {
            log.info("Database is empty. Seeding a mock PENDING transaction to test the async queue...");

            Transaction mockTx = Transaction.builder()
                    .tenantId("TENANT_001")
                    .invoiceNumber("INV-2026-0001")
                    .customerPin("P051234567M")
                    .customerName("Acme Corp")
                    .taxableAmount(new BigDecimal("1000.00"))
                    .taxAmount(new BigDecimal("160.00"))
                    .totalAmount(new BigDecimal("1160.00"))
                    .itemsPayload("[{\"name\": \"Web Hosting\", \"price\": 1000.00, \"taxRate\": 16}]")
                    .etimsStatus(EtimsStatus.PENDING)
                    .build();

            transactionRepository.save(mockTx);
            log.info("Mock transaction INV-2026-0001 inserted! The EtimsSyncWorker will pick it up soon.");
        }
    }
}
