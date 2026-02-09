package com.example.demo.agence.controller;

import com.example.demo.agence.dto.AgenceRequest;
import com.example.demo.agence.dto.AgenceResponse;
import com.example.demo.agence.service.AgenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agences")
@RequiredArgsConstructor
public class AgenceController {

    private final AgenceService agenceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgenceResponse> create(@RequestBody AgenceRequest request) {
        return ResponseEntity.ok(agenceService.createAgence(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BANCAIRE')")
    public ResponseEntity<List<AgenceResponse>> getAll() {
        return ResponseEntity.ok(agenceService.getAllAgences());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgenceResponse> update(@PathVariable Long id, @RequestBody AgenceRequest request) {
        return ResponseEntity.ok(agenceService.updateAgence(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agenceService.deleteAgence(id);
        return ResponseEntity.noContent().build();
    }
}
