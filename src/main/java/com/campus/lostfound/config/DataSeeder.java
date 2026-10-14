package com.campus.lostfound.config;

import com.campus.lostfound.model.Role;
import com.campus.lostfound.model.User;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner means "run this once, automatically, right after
 * the app starts." We use it here to create one login for each role,
 * so you have something to test with immediately.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // already set up, don't duplicate

        createUser("System Admin", "admin@saintgits.org", "admin123", Role.ADMIN);
        createUser("Front Desk Staff", "staff@saintgits.org", "staff123", Role.STAFF);
        createUser("Johan", "johan@saintgits.org", "student123", Role.STUDENT);

        System.out.println("Demo accounts ready:");
        System.out.println("  Admin:   admin@saintgits.org / admin123");
        System.out.println("  Staff:   staff@saintgits.org / staff123");
        System.out.println("  Student: johan@saintgits.org / student123");
    }

    private void createUser(String name, String email, String rawPassword, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtil.hash(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}
