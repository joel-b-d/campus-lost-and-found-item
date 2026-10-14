package com.campus.lostfound.repository;

import com.campus.lostfound.model.Role;
import com.campus.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * This is an interface with no code inside it — Spring reads the method
 * names and writes the actual database lookup code for you.
 *
 * "findByEmail" literally means: "find the row where the email column
 * matches." You get this for free just by naming the method correctly.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // Used to tell every staff member when a student reports a failed pickup.
    List<User> findByRole(Role role);
}
