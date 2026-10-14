package com.campus.lostfound.controller;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemType;
import com.campus.lostfound.model.Role;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.ItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    // ---------------- Report a LOST item — STUDENTS ONLY ----------------

    @GetMapping("/report-lost")
    public String reportLostPage(HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        // This is the rule from your requirements: staff cannot report
        // a lost item, only students can.
        if (user.getRole() != Role.STUDENT) {
            return "redirect:/dashboard";
        }
        return "report-lost";
    }

    @PostMapping("/report-lost")
    public String submitLostItem(@RequestParam String title,
                                  @RequestParam String description,
                                  @RequestParam String location,
                                  @RequestParam(required = false) MultipartFile photo,
                                  HttpServletRequest request) throws IOException {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.STUDENT) return "redirect:/dashboard";

        Item item = new Item();
        item.setType(ItemType.LOST);
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(location);
        item.setPostedBy(user);
        item.setImageFileName(savePhotoIfPresent(photo));
        itemRepository.save(item);

        return "redirect:/dashboard";
    }

    // ------------- Report a FOUND item — STUDENTS AND STAFF -------------

    @GetMapping("/report-found")
    public String reportFoundPage(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        // Students and Staff can both post a found item; only Admin is excluded
        // here since Admin's job is managing the app, not posting items.
        if (user.getRole() == Role.ADMIN) return "redirect:/dashboard";
        return "report-found";
    }

    @PostMapping("/report-found")
    public String submitFoundItem(@RequestParam String title,
                                   @RequestParam String description,
                                   @RequestParam String location,
                                   @RequestParam(required = false) MultipartFile photo,
                                   HttpServletRequest request) throws IOException {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() == Role.ADMIN) return "redirect:/dashboard";

        Item item = new Item();
        item.setType(ItemType.FOUND);
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(location);
        item.setPostedBy(user);
        item.setImageFileName(savePhotoIfPresent(photo));
        itemRepository.save(item);

        return "redirect:/dashboard";
    }

    /**
     * Saves an uploaded photo into the ./uploads folder and returns its
     * saved filename (or null if no photo was attached).
     *
     * IMPORTANT: we convert the folder to an ABSOLUTE path first
     * (.toAbsolutePath()). Without this, Tomcat resolves a relative
     * path against its own internal temp folder instead of this
     * project's folder, which causes a FileNotFoundException on Windows.
     */
    private String savePhotoIfPresent(MultipartFile photo) throws IOException {
        if (photo == null || photo.isEmpty()) {
            return null;
        }

        // Only accept images. Anything else (a .exe, a .pdf, a renamed text
        // file...) is ignored and the report is saved without a photo.
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return null;
        }

        Path uploadsFolder = Path.of("uploads").toAbsolutePath();
        Files.createDirectories(uploadsFolder); // creates the folder if it doesn't exist yet

        // Add a random prefix so two people uploading "photo.jpg" don't overwrite each other.
        // Keep only safe characters from the original name (no slashes or "..").
        String originalName = photo.getOriginalFilename() == null ? "photo" : photo.getOriginalFilename();
        String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        String savedFileName = UUID.randomUUID() + "-" + safeName;
        Path destination = uploadsFolder.resolve(savedFileName);

        photo.transferTo(destination.toFile());
        return savedFileName;
    }
}
