package com.ntn.auction.repository;

import com.ntn.auction.entity.BidAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidAuditLogRepository extends JpaRepository<BidAuditLog, Long> {}
