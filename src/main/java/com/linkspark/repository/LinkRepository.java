package com.linkspark.repository;

import com.linkspark.domain.User;
import com.linkspark.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, Long> {
    boolean existsByAlias(String alias);
    Optional<Link> findByAlias(String alias);
    List<Link> findByOwner(User owner);
    List<Link> findByOwnerId(UUID ownerId);


}

