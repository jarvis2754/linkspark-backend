package com.linkspark.repository;

import com.linkspark.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    boolean existsByAlias(String alias);
    Optional<Link> findByAlias(String alias);
}

