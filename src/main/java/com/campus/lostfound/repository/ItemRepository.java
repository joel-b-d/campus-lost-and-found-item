package com.campus.lostfound.repository;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // Used for the dashboard: "all lost items that are still open"
    List<Item> findByTypeAndStatus(ItemType type, ItemStatus status);

    // Used on the matching page, to list items staff can pick from
    List<Item> findByType(ItemType type);

    // Used so a student can see their own reports
    List<Item> findByPostedById(Long userId);
}
