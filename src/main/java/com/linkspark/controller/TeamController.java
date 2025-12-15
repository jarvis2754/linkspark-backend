package com.linkspark.controller;

import com.linkspark.dto.TeamDtos;
import com.linkspark.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<?> createTeam(Authentication auth, @RequestBody TeamDtos.CreateTeamRequest req) {
        return ResponseEntity.ok(teamService.createTeam(auth, req));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    @GetMapping("/my-teams")
    public ResponseEntity<?> getMyTeams(Authentication auth) {
        return ResponseEntity.ok(teamService.getMyTeams(auth));
    }

    @PostMapping("/{teamId}/invite")
    public ResponseEntity<?> invite(
            Authentication auth,
            @PathVariable UUID teamId,
            @RequestBody TeamDtos.InviteRequest req
    ) {
        teamService.inviteUser(auth, teamId, req);
        return ResponseEntity.ok(Map.of("message", "Invite sent"));
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<?> acceptInvite(Authentication auth, @PathVariable String token) {
        teamService.acceptInvite(auth, token);
        return ResponseEntity.ok(Map.of("message", "Invite accepted"));
    }

    @DeleteMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> removeMember(
            Authentication auth,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId
    ) {
        teamService.removeMember(auth, teamId, memberId);
        return ResponseEntity.ok(Map.of("message", "Removed"));
    }

    @PatchMapping("/{teamId}/member/{memberId}/role")
    public ResponseEntity<?> changeRole(
            Authentication auth,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId,
            @RequestBody TeamDtos.ChangeRoleRequest req
    ) {
        teamService.changeRole(auth, teamId, memberId, req);
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<?> updateTeam(
            Authentication auth,
            @PathVariable UUID teamId,
            @RequestBody TeamDtos.CreateTeamRequest req
    ) {
        return ResponseEntity.ok(teamService.updateTeam(auth, teamId, req));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(Authentication auth, @PathVariable UUID teamId) {
        teamService.deleteTeam(auth, teamId);
        return ResponseEntity.ok(Map.of("message", "Team deleted"));
    }

    @GetMapping("/invites")
    public ResponseEntity<?> getMyInvites(Authentication auth) {
        return ResponseEntity.ok(teamService.getMyInvites(auth));
    }
}
