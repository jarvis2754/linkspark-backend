package com.linkspark.service;

import com.linkspark.domain.User;
import com.linkspark.dto.TeamDtos;
import com.linkspark.model.TeamMember;
import com.linkspark.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepo;
    private final UserService userService;

    @Transactional
    public void invite(Authentication auth, TeamDtos.InviteRequest req) {
        User owner = (User) auth.getPrincipal();

        teamRepo.findByOwnerAndEmail(owner, req.email()).ifPresent(existing -> {
            throw new ResponseStatusException(BAD_REQUEST, "Invite already exists");
        });

        TeamMember member = new TeamMember();
        member.setOwner(owner);
        member.setEmail(req.email().toLowerCase());
        member.setRole(req.role());
        member.setPending(true);

        teamRepo.save(member);

    }

    public List<TeamDtos.TeamMemberDto> list(Authentication auth) {
        User owner = (User) auth.getPrincipal();
        return teamRepo.findByOwner(owner).stream()
                .map(m -> new TeamDtos.TeamMemberDto(
                        m.getId().toString(),
                        m.getEmail(),
                        m.getRole(),
                        m.isPending()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void remove(Authentication auth, UUID memberId) {
        User owner = (User) auth.getPrincipal();
        TeamMember member = teamRepo.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Member not found"));

        if (!member.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Not your workspace");
        }

        if ("owner".equalsIgnoreCase(member.getRole())) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot remove owner");
        }

        teamRepo.delete(member);
    }

    @Transactional
    public void changeRole(Authentication auth, UUID memberId, TeamDtos.ChangeRoleRequest req) {
        User owner = (User) auth.getPrincipal();
        TeamMember member = teamRepo.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Member not found"));

        if (!member.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Not your workspace");
        }

        member.setRole(req.role());
        teamRepo.save(member);
    }

    @Transactional
    public void acceptInvite(Authentication auth, String ownerEmail) {
        User user = (User) auth.getPrincipal();

        User owner = userService.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Owner not found"));

        TeamMember member = teamRepo.findByOwnerAndEmail(owner, user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Invite not found"));

        if (!member.isPending()) {
            throw new ResponseStatusException(BAD_REQUEST, "Already accepted");
        }

        member.setPending(false);
        teamRepo.save(member);
    }
}

