package com.queueless.controller;

import com.queueless.model.User;
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

    @PostMapping("/create-org")
    public ResponseEntity<?> createOrg(@RequestBody Map<String, String> req) {

        var admin = adminService.createOrganisationWithAdmin(
                req.get("name"),
                req.get("type"),
                req.get("prefix")
        );

        return ResponseEntity.ok(Map.of(
                "adminEmail", admin.getEmail(),
                "adminPassword", admin.getPassword(),
                "orgId", admin.getOrganisation().getId()
        ));
    }
}