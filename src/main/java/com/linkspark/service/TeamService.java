package com.linkspark.service;

import com.linkspark.domain.Team;
import com.linkspark.domain.TeamInvite;
import com.linkspark.domain.TeamMember;
import com.linkspark.domain.User;
import com.linkspark.dto.TeamDtos;
import com.linkspark.repository.TeamInviteRepository;
import com.linkspark.repository.TeamMemberRepository;
import com.linkspark.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;
    private final TeamInviteRepository inviteRepo;
    private final UserService userService;

    @Transactional
    public Team createTeam(Authentication auth, TeamDtos.CreateTeamRequest req) {
        User owner = (User) auth.getPrincipal();

        Team team = new Team();
        team.setName(req.name());
        team.setOwner(owner);

        teamRepo.save(team);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(team);
        ownerMember.setUser(owner);
        ownerMember.setEmail(owner.getEmail());
        ownerMember.setRole("owner");
        ownerMember.setPending(false);

        memberRepo.save(ownerMember);

        return team;
    }

    public TeamDtos.TeamDto getTeam(UUID teamId) {
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<TeamMember> members = memberRepo.findByTeam(team);

        List<TeamDtos.TeamMemberDto> list = members.stream()
                .map(m -> new TeamDtos.TeamMemberDto(
                        m.getId().toString(),
                        // userId can be null for pending invites
                        m.getUser() != null ? m.getUser().getId().toString() : null,
                        // show name if user exists; otherwise fallback to email
                        m.getUser() != null ? m.getUser().getName() : m.getEmail(),
                        m.getUser() != null ? m.getUser().getEmail() : m.getEmail(),
                        m.getRole(),
                        m.isPending()
                ))
                .toList();

        return new TeamDtos.TeamDto(team.getId().toString(), team.getName(), list);
    }

    @Transactional
    public void inviteUser(Authentication auth, UUID teamId, TeamDtos.InviteRequest req) {
        User inviter = (User) auth.getPrincipal();

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        TeamMember inv = memberRepo.findByTeamAndUser(team, inviter)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (!inv.getRole().equals("owner") && !inv.getRole().equals("admin"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        String email = req.email().toLowerCase();

        memberRepo.findByTeamAndEmail(team, email).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member or invite already exists");
        });

        TeamInvite invite = new TeamInvite();
        invite.setTeam(team);
        invite.setEmail(email);
        invite.setRole(req.role());
        invite.setToken(UUID.randomUUID().toString());
        invite.setExpiresAt(Instant.now().plusSeconds(86400)); // 24 hrs

        inviteRepo.save(invite);

        TeamMember pendingMember = new TeamMember();
        pendingMember.setTeam(team);
        pendingMember.setUser(null);
        pendingMember.setEmail(email);
        pendingMember.setRole(req.role());
        pendingMember.setPending(true);

        memberRepo.save(pendingMember);
    }

    @Transactional
    public void acceptInvite(Authentication auth, String token) {
        User user = (User) auth.getPrincipal();

        TeamInvite invite = inviteRepo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (invite.getExpiresAt().isBefore(Instant.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite expired");

        TeamMember pending = memberRepo.findByTeamAndEmail(invite.getTeam(), invite.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending membership not found"));

        pending.setUser(user);
        pending.setPending(false);
        pending.setEmail(user.getEmail());

        memberRepo.save(pending);

        inviteRepo.delete(invite);
    }

    @Transactional
    public void removeMember(Authentication auth, UUID teamId, UUID memberId) {
        User requester = (User) auth.getPrincipal();

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        TeamMember reqMember = memberRepo.findByTeamAndUser(team, requester)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (!reqMember.getRole().equals("owner") && !reqMember.getRole().equals("admin"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        TeamMember target = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (target.getRole().equals("owner"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove owner");

        memberRepo.delete(target);
    }

    @Transactional
    public void changeRole(Authentication auth, UUID teamId, UUID memberId, TeamDtos.ChangeRoleRequest req) {
        User requester = (User) auth.getPrincipal();

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        TeamMember reqMember = memberRepo.findByTeamAndUser(team, requester)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (!reqMember.getRole().equals("owner"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        TeamMember target = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        target.setRole(req.role());
        memberRepo.save(target);
    }

    public List<TeamDtos.TeamSummaryDto> getMyTeams(Authentication auth) {
        User user = (User) auth.getPrincipal();

        List<TeamMember> memberships = memberRepo.findByUser(user);

        return memberships.stream()
                .map(m -> new TeamDtos.TeamSummaryDto(
                        m.getTeam().getId().toString(),
                        m.getTeam().getName(),
                        m.getRole()
                ))
                .toList();
    }

    @Transactional
    public Team updateTeam(Authentication auth, UUID teamId, TeamDtos.CreateTeamRequest req) {
        User requester = (User) auth.getPrincipal();

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!team.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can rename team");
        }

        team.setName(req.name());
        teamRepo.save(team);

        return team;
    }

    @Transactional
    public void deleteTeam(Authentication auth, UUID teamId) {
        User requester = (User) auth.getPrincipal();

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!team.getOwner().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can delete team");
        }

        List<TeamMember> members = memberRepo.findByTeam(team);
        memberRepo.deleteAll(members);

        inviteRepo.deleteAll(inviteRepo.findAll());

        teamRepo.delete(team);
    }

    public List<TeamDtos.InviteInfo> getMyInvites(Authentication auth) {
        User user = (User) auth.getPrincipal();

        List<TeamInvite> invites = inviteRepo.findByEmail(user.getEmail());

        return invites.stream()
                .map(inv -> new TeamDtos.InviteInfo(
                        inv.getToken(),
                        inv.getTeam().getId().toString(),
                        inv.getTeam().getName(),
                        inv.getRole(),
                        inv.getExpiresAt().toString()
                ))
                .toList();
    }
}
