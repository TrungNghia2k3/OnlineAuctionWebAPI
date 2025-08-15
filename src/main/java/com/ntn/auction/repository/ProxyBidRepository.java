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

    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.status = 'ACTIVE'")
    List<ProxyBid> findActiveProxyBidsForItem(@Param("itemId") Long itemId);

    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId AND pb.item.id = :itemId AND pb.status = 'ACTIVE'")
    ProxyBid findActiveByUserAndItem(@Param("userId") String userId, @Param("itemId") Long itemId);

    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId ORDER BY pb.createdDate DESC")
    List<ProxyBid> findByUserId(@Param("userId") String userId);

    @Query("SELECT pb FROM ProxyBid pb WHERE pb.user.id = :userId AND pb.winning = true")
    List<ProxyBid> findWinningProxyBidsByUser(@Param("userId") String userId);

    @Query("SELECT pb FROM ProxyBid pb WHERE pb.item.id = :itemId AND pb.maxAmount > :currentAmount AND pb.status = 'ACTIVE' ORDER BY pb.maxAmount DESC")
    List<ProxyBid> findEligibleProxyBids(@Param("itemId") Long itemId, @Param("currentAmount") BigDecimal currentAmount);
}
