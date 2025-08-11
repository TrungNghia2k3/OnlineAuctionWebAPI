package com.ntn.auction.service;

import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.BidAuditLog;
import com.ntn.auction.repository.BidAuditLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidAuditService {

    BidAuditLogRepository bidAuditLogRepository;

    /**
     * Creates an immutable audit log entry for bid actions
     */
    @Transactional
    public void logBidAction(Bid bid, BidAuditLog.ActionType actionType, String ipAddress) {
        try {
            BidAuditLog auditLog = BidAuditLog.builder()
                    .bidId(bid.getId())
                    .itemId(bid.getItem().getId())
                    .userId(bid.getBuyer().getId())
                    .bidAmount(bid.getAmount())
                    .timestamp(LocalDateTime.now())
                    .actionType(actionType)
                    .ipAddress(ipAddress)
                    .proxyBid(bid.getProxyBid()) // Use the correct field name
                    .validationHash(generateValidationHash(bid, actionType))
                    .build();

            bidAuditLogRepository.save(auditLog);
            log.info("Audit log created for bid {} with action {}", bid.getId(), actionType);

        } catch (Exception e) {
            log.error("Failed to create audit log for bid {}: {}", bid.getId(), e.getMessage());
            // Don't throw exception as this shouldn't break the main flow
        }
    }

    private String generateValidationHash(Bid bid, BidAuditLog.ActionType actionType) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String dataToHash = String.format("%d-%s-%s-%s-%s",
                    bid.getId(),
                    bid.getAmount().toString(),
                    bid.getBidTime().toString(),
                    bid.getBuyer().getId(),
                    actionType.toString());

            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return "HASH_ERROR_" + System.currentTimeMillis();
        }
    }
}
