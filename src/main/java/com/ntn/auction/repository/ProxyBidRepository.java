package com.ntn.auction.repository;

import com.ntn.auction.entity.ProxyBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProxyBidRepository extends JpaRepository<ProxyBid, Long> {

    /**
     * Find active proxy bids for a specific item
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'ACTIVE'")
    List<ProxyBid> findActiveProxyBidsForItem(@Param("itemId") Long itemId);

    /**
     * Find active proxy bid by user and item
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId AND pb.item.id = :itemId AND pb.status = 'ACTIVE'")
    ProxyBid findActiveByUserAndItem(@Param("userId") String userId, @Param("itemId") Long itemId);

    /**
     * Find all proxy bids by user ID
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId ORDER BY pb.createdDate DESC")
    List<ProxyBid> findByUserId(@Param("userId") String userId);

    /**
     * Find winning proxy bids by user
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId AND pb.winning = true")
    List<ProxyBid> findWinningProxyBidsByUser(@Param("userId") String userId);

    /**
     * Find eligible proxy bids for competitive bidding
     * (proxy bids that have max amount greater than current amount and are active)
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.maxAmount > :currentAmount AND pb.status = 'ACTIVE' ORDER BY pb.maxAmount DESC")
    List<ProxyBid> findEligibleProxyBids(@Param("itemId") Long itemId, @Param("currentAmount") BigDecimal currentAmount);

    /**
     * Find proxy bids by item ID
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId")
    List<ProxyBid> findByItemId(@Param("itemId") Long itemId);

    /**
     * Find proxy bids by status
     */
    List<ProxyBid> findByStatus(ProxyBid.ProxyBidStatus status);

    /**
     * Find proxy bids by user and status
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId AND pb.status = :status")
    List<ProxyBid> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") ProxyBid.ProxyBidStatus status);

    /**
     * Count active proxy bids for an item
     */
    @Query("SELECT COUNT(pb) FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'ACTIVE'")
    long countActiveProxyBidsForItem(@Param("itemId") Long itemId);

    /**
     * Count proxy bids by user
     */
    @Query("SELECT COUNT(pb) FROM ProxyBid pb WHERE pb.user.id = :userId")
    long countByUserId(@Param("userId") String userId);

    /**
     * Find exhausted proxy bids for an item
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'EXHAUSTED'")
    List<ProxyBid> findExhaustedProxyBidsForItem(@Param("itemId") Long itemId);

    /**
     * Find outbid proxy bids for an item
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'OUTBID'")
    List<ProxyBid> findOutbidProxyBidsForItem(@Param("itemId") Long itemId);
}
