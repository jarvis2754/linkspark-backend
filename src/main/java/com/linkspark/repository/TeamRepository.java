package com.linkspark.repository;

import com.linkspark.model.TeamMember;
import com.linkspark.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<TeamMember, UUID> {
    List<TeamMember> findByOwner(User owner);
    Optional<TeamMember> findByOwnerAndEmail(User owner, String email);
}

