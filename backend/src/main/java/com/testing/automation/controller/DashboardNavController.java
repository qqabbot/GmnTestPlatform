package com.testing.automation.controller;

import com.testing.automation.model.NavAddress;
import com.testing.automation.model.NavEnvironment;
import com.testing.automation.service.DashboardNavService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard-nav")
@CrossOrigin(origins = "*")
public class DashboardNavController {

    @Autowired
    private DashboardNavService dashboardNavService;

    @GetMapping("/tree")
    public ResponseEntity<List<NavEnvironment>> getTree() {
        return ResponseEntity.ok(dashboardNavService.getTree());
    }

    @GetMapping("/environments")
    public ResponseEntity<List<NavEnvironment>> getAllEnvironments() {
        return ResponseEntity.ok(dashboardNavService.getAllEnvironments());
    }

    @GetMapping("/environments/{id}")
    public ResponseEntity<NavEnvironment> getEnvironmentById(@PathVariable Long id) {
        NavEnvironment env = dashboardNavService.getEnvironmentById(id);
        if (env == null) {
            return ResponseEntity.notFound().build();
        }
        env.setAddresses(dashboardNavService.getAddressesByEnvironmentId(id));
        return ResponseEntity.ok(env);
    }

    @PostMapping("/environments")
    public ResponseEntity<NavEnvironment> createEnvironment(@RequestBody NavEnvironment env) {
        return ResponseEntity.ok(dashboardNavService.createEnvironment(env));
    }

    @PutMapping("/environments/{id}")
    public ResponseEntity<NavEnvironment> updateEnvironment(@PathVariable Long id, @RequestBody NavEnvironment env) {
        NavEnvironment updated = dashboardNavService.updateEnvironment(id, env);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/environments/{id}")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable Long id) {
        dashboardNavService.deleteEnvironment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/environments/{envId}/addresses")
    public ResponseEntity<List<NavAddress>> getAddresses(@PathVariable Long envId) {
        return ResponseEntity.ok(dashboardNavService.getAddressesByEnvironmentId(envId));
    }

    @PostMapping("/environments/{envId}/addresses")
    public ResponseEntity<NavAddress> createAddress(@PathVariable Long envId, @RequestBody NavAddress address) {
        return ResponseEntity.ok(dashboardNavService.createAddress(envId, address));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<NavAddress> updateAddress(@PathVariable Long id, @RequestBody NavAddress address) {
        NavAddress updated = dashboardNavService.updateAddress(id, address);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        dashboardNavService.deleteAddress(id);
        return ResponseEntity.ok().build();
    }
}
