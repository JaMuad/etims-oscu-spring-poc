package com.muad.etims.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // SaaS Business Fields
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "customer_pin")
    private String customerPin;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "taxable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // Using String for simplicity in POC, could map to JSONB dialect later
    @Column(name = "items_payload", columnDefinition = "TEXT")
    private String itemsPayload;

    // KRA Compliance Fields
    @Column(name = "kra_receipt_number")
    private String kraReceiptNumber;

    @Column(name = "control_code")
    private String controlCode;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "cmc_key")
    private String cmcKey;

    // Queue State Tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "etims_status", nullable = false)
    private EtimsStatus etimsStatus;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (etimsStatus == null) {
            etimsStatus = EtimsStatus.PENDING;
        }
    }
}
