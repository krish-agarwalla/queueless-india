package com.queueless.service;

import com.queueless.model.Organisation;
import com.queueless.model.User;
import com.queueless.model.enums.Role;
import com.queueless.repository.OrganisationRepository;
import com.queueless.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrganisationRepository orgRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public User createOrganisationWithAdmin(String name, String type, String prefix) {

        Organisation org = new Organisation();
        org.setName(name);
        org.setType(type);
        org.setPrefix(prefix);
        org = orgRepo.save(org);

        // Generate credentials
        String email = name.toLowerCase().replace(" ", "") + "@client.com";
        String rawPassword = UUID.randomUUID().toString().substring(0, 8);

        User admin = new User();
        admin.setName(name + " Admin");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ORG_ADMIN);
        admin.setOrganisation(org);

        userRepo.save(admin);

        // Attach raw password temporarily (for response)
        admin.setPassword(rawPassword);

        return admin;
    }
}