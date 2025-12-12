package com.linkspark.repository;

import com.linkspark.domain.Team;
import com.linkspark.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOwner(User owner);
}
