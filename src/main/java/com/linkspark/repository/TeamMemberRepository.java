package com.linkspark.repository;

import com.linkspark.domain.Team;
import com.linkspark.domain.TeamMember;
import com.linkspark.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findByTeam(Team team);

    List<TeamMember> findByUser(User user);

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    Optional<TeamMember> findByTeamAndEmail(Team team, String email);
}
