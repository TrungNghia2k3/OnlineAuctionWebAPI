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
}
