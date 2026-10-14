package com.campus.lostfound.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/**
 * One lost or found report. type tells you which one it is.
 *
 * matchedItemId / pickupInfo stay empty (null) until a staff member
 * pairs this item up with its opposite (see MatchController).
 */
@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ItemType type;       // LOST or FOUND

    private String title;
    private String description;
    private String location;     // where it was lost / found

    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.OPEN;

    @ManyToOne                   // many items can be posted by one user
    private User postedBy;

    private Long matchedItemId;  // id of the paired item, once matched (otherwise null)
    private String pickupInfo;   // staff fills this in: where/when to collect it

    private String imageFileName; // filename of an uploaded photo, saved in the uploads/ folder (null if no photo)

    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- getters and setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public User getPostedBy() { return postedBy; }
    public void setPostedBy(User postedBy) { this.postedBy = postedBy; }

    public Long getMatchedItemId() { return matchedItemId; }
    public void setMatchedItemId(Long matchedItemId) { this.matchedItemId = matchedItemId; }

    public String getPickupInfo() { return pickupInfo; }
    public void setPickupInfo(String pickupInfo) { this.pickupInfo = pickupInfo; }

    public String getImageFileName() { return imageFileName; }
    public void setImageFileName(String imageFileName) { this.imageFileName = imageFileName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
