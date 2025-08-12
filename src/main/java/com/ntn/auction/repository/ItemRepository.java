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

    /**
     * Find items by seller ID ordered by creation date (newest first)
     */
    List<Item> findBySellerIdOrderByAuctionStartDateDesc(String sellerId);

    /**
     * Find items by category ID and status ordered by auction end date
     */
    List<Item> findByCategoryIdAndStatusOrderByAuctionEndDateAsc(Long categoryId, Item.ItemStatus status);

    /**
     * Find active auctions (items that are currently active and within auction timeframe)
     */
    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.auctionStartDate <= :now AND i.auctionEndDate > :now")
    List<Item> findActiveAuctions(@Param("now") LocalDateTime now);

    /**
     * Find auctions ending soon (within specified time range)
     */
    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.auctionEndDate BETWEEN :now AND :endTime")
    List<Item> findEndingSoonAuctions(@Param("now") LocalDateTime now, @Param("endTime") LocalDateTime endTime);

    /**
     * Find items by status
     */
    List<Item> findByStatus(Item.ItemStatus status);

    /**
     * Find items by seller ID and status
     */
    List<Item> findBySellerIdAndStatus(String sellerId, Item.ItemStatus status);

    /**
     * Find expired auctions (auction end date has passed but status is still ACTIVE)
     */
    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.auctionEndDate < :now")
    List<Item> findExpiredAuctions(@Param("now") LocalDateTime now);

    /**
     * Find items starting soon (auction start date is in the near future)
     */
    @Query("SELECT i FROM Item i WHERE i.status = 'APPROVED' AND i.auctionStartDate BETWEEN :now AND :startTime")
    List<Item> findItemsStartingSoon(@Param("now") LocalDateTime now, @Param("startTime") LocalDateTime startTime);

    /**
     * Count items by seller ID
     */
    long countBySellerId(String sellerId);

    /**
     * Count items by category ID
     */
    long countByCategoryId(Long categoryId);

    /**
     * Find items by name containing (case insensitive)
     */
    List<Item> findByNameContainingIgnoreCase(String name);
}
