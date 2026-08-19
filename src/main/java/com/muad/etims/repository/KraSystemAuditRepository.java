package com.muad.etims.repository;

import com.muad.etims.entity.KraSystemAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KraSystemAuditRepository extends JpaRepository<KraSystemAudit, UUID> {
}
