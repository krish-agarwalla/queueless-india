package com.queueless.controller;

import com.queueless.model.Organisation;
import com.queueless.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final AdminService adminService;

    @PostMapping("/create-org")
    public ResponseEntity<?> createOrg(@RequestBody Map<String, String> req) {

        var admin = adminService.createOrganisationWithAdmin(
                req.get("name"),
                req.get("type"),
                req.get("prefix")
        );

        return ResponseEntity.ok(Map.of(
                "adminEmail", admin.getEmail(),
                "adminPassword", "Use initial password set during creation",
                "orgId", admin.getOrganisation().getId()
        ));
    }

    // 🔐 Only SUPER_ADMIN should access
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/orgs")
    public ResponseEntity<List<Organisation>> getAllOrgs() {
        return ResponseEntity.ok(adminService.getAllOrganisations());
    }
}