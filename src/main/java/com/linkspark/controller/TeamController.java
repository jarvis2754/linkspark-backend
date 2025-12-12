package com.linkspark.controller;

import com.linkspark.dto.TeamDtos;
import com.linkspark.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/invite")
    public ResponseEntity<?> invite(@RequestBody TeamDtos.InviteRequest req, Authentication auth) {
        teamService.invite(auth, req);
        return ResponseEntity.ok(Map.of("message", "Invite sent"));
    }

    @GetMapping
    public ResponseEntity<List<TeamDtos.TeamMemberDto>> list(Authentication auth) {
        return ResponseEntity.ok(teamService.list(auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable UUID id, Authentication auth) {
        teamService.remove(auth, id);
        return ResponseEntity.ok(Map.of("message", "Removed"));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable UUID id, @RequestBody TeamDtos.ChangeRoleRequest req, Authentication auth) {
        teamService.changeRole(auth, id, req);
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestParam("ownerEmail") String ownerEmail, Authentication auth) {
        teamService.acceptInvite(auth, ownerEmail);
        return ResponseEntity.ok(Map.of("message", "Accepted"));
    }
}

