package io.mertkaniscan.automation_engine.services;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) {
        // Replace this with your own user fetching logic from the database
        if ("user".equals(username)) {
            return new User("user", "password", Collections.emptyList());
        } else {
            throw new UsernameNotFoundException("User not found.");
        }
    }
}