package com.muad.etims.repository;

import com.muad.etims.entity.EtimsStatus;
import com.muad.etims.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findTop50ByEtimsStatusInOrderByCreatedAtAsc(List<EtimsStatus> statuses);
}
