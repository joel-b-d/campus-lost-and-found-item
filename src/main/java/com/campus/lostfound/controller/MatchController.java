package com.campus.lostfound.controller;

import com.campus.lostfound.model.*;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.NotificationRepository;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.service.MatchingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The matching portal.
 *
 * - Staff/Admin see each STILL-OPEN lost item as a card. Next to it, the
 *   app lists every open found item ranked by how similar it looks
 *   (see MatchingService — TF-IDF + cosine similarity). Staff pick one,
 *   type the pickup info, and confirm. A found item stays available
 *   after being matched once, because several students could have lost
 *   similar items; it only closes when a student confirms pickup.
 *
 * - Students see their own items and, once matched, two buttons:
 *   "I got my item" (closes everything) or "I didn't get my item"
 *   (cancels the match and asks them to submit a new report).
 */
@Controller
public class MatchController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchingService matchingService;

    @GetMapping("/match")
    public String matchPage(HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        if (user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN) {
            // The algorithm does the first pass: each lost item comes with
            // its found-item candidates already sorted best-match-first.
            model.addAttribute("suggestions", matchingService.buildSuggestions(
                    itemRepository.findByTypeAndStatus(ItemType.LOST, ItemStatus.OPEN),
                    itemRepository.findByTypeAndStatus(ItemType.FOUND, ItemStatus.OPEN)));
        } else {
            model.addAttribute("myItems", itemRepository.findByPostedById(user.getId()));
        }

        return "match";
    }

    // Staff/Admin submit this form to confirm a match between one lost
    // item and one found item.
    @PostMapping("/match/create")
    public String createMatch(@RequestParam Long lostItemId,
                               @RequestParam Long foundItemId,
                               @RequestParam String pickupInfo,
                               HttpServletRequest request) {

        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
            return "redirect:/dashboard";
        }

        Item lostItem = itemRepository.findById(lostItemId).orElseThrow();
        Item foundItem = itemRepository.findById(foundItemId).orElseThrow();

        // Guard against a double-click or a stale page: only open items can be matched.
        if (lostItem.getStatus() != ItemStatus.OPEN || foundItem.getStatus() != ItemStatus.OPEN) {
            return "redirect:/match";
        }

        // Only the LOST item is marked MATCHED. The FOUND item deliberately
        // stays OPEN until the real owner confirms it's theirs.
        lostItem.setStatus(ItemStatus.MATCHED);
        lostItem.setMatchedItemId(foundItem.getId());
        lostItem.setPickupInfo(pickupInfo);
        itemRepository.save(lostItem);

        Notification note = new Notification();
        note.setUser(lostItem.getPostedBy());
        note.setMessage("Possible match for your lost item \"" + lostItem.getTitle() +
                "\". Pickup info: " + pickupInfo +
                ". Please confirm on the Matching Portal once you've collected it " +
                "(or tell us if it wasn't yours).");
        notificationRepository.save(note);

        return "redirect:/match";
    }

    // Student clicks "I got my item" to finalize things once they've
    // actually collected it from staff.
    @PostMapping("/match/{itemId}/confirm")
    public String confirmPickup(@PathVariable Long itemId, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Item lostItem = itemRepository.findById(itemId).orElseThrow();

        // Only the student who posted this lost item can confirm it,
        // and only if it's actually in the MATCHED state.
        if (!lostItem.getPostedBy().getId().equals(user.getId())) {
            return "redirect:/match";
        }
        if (lostItem.getStatus() != ItemStatus.MATCHED) {
            return "redirect:/match";
        }

        lostItem.setStatus(ItemStatus.COLLECTED);
        itemRepository.save(lostItem);

        // The real owner has confirmed, so close out the found item too —
        // it should stop being offered as a possible match to anyone else.
        if (lostItem.getMatchedItemId() != null) {
            itemRepository.findById(lostItem.getMatchedItemId()).ifPresent(foundItem -> {
                foundItem.setStatus(ItemStatus.COLLECTED);
                itemRepository.save(foundItem);
            });
        }

        return "redirect:/match";
    }

    // Student clicks "I didn't get my item": the match was wrong (or the
    // pickup failed). The lost report is cancelled, the student is told to
    // submit a fresh report, and staff are notified. The FOUND item is left
    // OPEN so staff can try matching it with someone else.
    @PostMapping("/match/{itemId}/reject")
    public String rejectMatch(@PathVariable Long itemId, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Item lostItem = itemRepository.findById(itemId).orElseThrow();

        if (!lostItem.getPostedBy().getId().equals(user.getId())) {
            return "redirect:/match";
        }
        if (lostItem.getStatus() != ItemStatus.MATCHED) {
            return "redirect:/match";
        }

        lostItem.setStatus(ItemStatus.CANCELLED);
        itemRepository.save(lostItem);

        // Tell the student what to do next.
        Notification toStudent = new Notification();
        toStudent.setUser(user);
        toStudent.setMessage("You said you did not receive \"" + lostItem.getTitle() +
                "\", so this report has been cancelled. Please submit a NEW lost report " +
                "(Report Lost) with as much detail as you can — colour, brand, marks, and a photo.");
        notificationRepository.save(toStudent);

        // Tell every staff member so they know the found item is free again.
        for (User staff : userRepository.findByRole(Role.STAFF)) {
            Notification toStaff = new Notification();
            toStaff.setUser(staff);
            toStaff.setMessage(user.getFullName() + " reported that the match for \"" +
                    lostItem.getTitle() + "\" was not their item. The found item is open for matching again.");
            notificationRepository.save(toStaff);
        }

        return "redirect:/match";
    }
}
