package com.ntn.auction.repository;

import com.ntn.auction.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByStatusAndAuctionStartDateBefore(Item.ItemStatus status, LocalDateTime dateTime);

    List<Item> findByStatusAndAuctionEndDateBefore(Item.ItemStatus status, LocalDateTime dateTime);

    List<Item> findBySellerIdOrderByAuctionStartDateDesc(String sellerId);

    List<Item> findByCategoryIdAndStatusOrderByAuctionEndDateAsc(Long categoryId, Item.ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.auctionStartDate <= :now AND i.auctionEndDate > :now")
    List<Item> findActiveAuctions(@Param("now") LocalDateTime now);

    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.auctionEndDate BETWEEN :now AND :endTime")
    List<Item> findEndingSoonAuctions(@Param("now") LocalDateTime now, @Param("endTime") LocalDateTime endTime);
}
