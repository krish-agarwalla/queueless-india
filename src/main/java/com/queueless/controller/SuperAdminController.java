package com.queueless.controller;

import com.queueless.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final AdminService adminService;

    /**
     * POST /api/superadmin/create-org
     * Creates a new organisation AND its ORG_ADMIN user in one call.
     * Returns the generated email + password so the super-admin can hand them over.
     */
    @PostMapping("/create-org")
    public ResponseEntity<?> createOrg(@RequestBody Map<String, String> body) {
        String name   = body.get("name");
        String type   = body.get("type");
        String prefix = body.get("prefix");

        if (name == null || name.isBlank() ||
                type == null || type.isBlank() ||
                prefix == null || prefix.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "name, type and prefix are all required"));
        }

        Map<String, Object> result = adminService.createOrganisationWithAdmin(name, type, prefix);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/superadmin/orgs
     * Returns all organisations (for the Active Deployments list).
     */
    @GetMapping("/orgs")
    public ResponseEntity<?> getAllOrgs() {
        return ResponseEntity.ok(adminService.getAllOrganisations());
    }
}