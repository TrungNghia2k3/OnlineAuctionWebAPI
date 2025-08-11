package com.ntn.auction.repository;

import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByItemIdOrderByAmountDesc(Long itemId);

    @Modifying
    @Query("UPDATE Bid b SET b.status = 'OUTBID' WHERE b.item.id = :itemId AND b.status = 'ACCEPTED'")
    void markPreviousBidsAsOutbid(@Param("itemId") Long itemId);

    @Modifying
    @Query("UPDATE Bid b SET b.highestBid = false WHERE b.item.id = :itemId")
    void resetHighestBidFlags(@Param("itemId") Long itemId);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.item.id = :itemId")
    Long countByItemId(@Param("itemId") Long itemId);

    Optional<Bid> findTopByItemOrderByAmountDesc(Item item);

    List<Bid> findByItem(Item item);
}
