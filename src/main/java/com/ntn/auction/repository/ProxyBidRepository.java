package com.ntn.auction.repository;

import com.ntn.auction.entity.ProxyBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyBidRepository extends JpaRepository<ProxyBid, Long> {

    /**
     * Find active proxy bids for an item, ordered by max amount descending
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'ACTIVE' ORDER BY pb.maxAmount DESC")
    List<ProxyBid> findActiveProxyBidsForItem(@Param("itemId") Long itemId);

    /**
     * Find user's active proxy bid for a specific item
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.user.id = :userId AND pb.status = 'ACTIVE'")
    Optional<ProxyBid> findActiveProxyBidByUserAndItem(@Param("userId") String userId, @Param("itemId") Long itemId);

    /**
     * Find all proxy bids that can still bid (max amount > current bid amount)
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'ACTIVE' AND pb.maxAmount > :currentAmount ORDER BY pb.maxAmount DESC")
    List<ProxyBid> findEligibleProxyBids(@Param("itemId") Long itemId, @Param("currentAmount") BigDecimal currentAmount);

    /**
     * Find all proxy bids for a user
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId ORDER BY pb.createdDate DESC")
    List<ProxyBid> findByUserId(@Param("userId") String userId);

    /**
     * Find winning proxy bids for a user
     */
    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId AND pb.winning = true")
    List<ProxyBid> findWinningProxyBidsByUser(@Param("userId") String userId);
}
