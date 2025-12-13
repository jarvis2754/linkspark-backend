package com.linkspark.repository;

import com.linkspark.domain.User;
import com.linkspark.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, Long> {
    boolean existsByAlias(String alias);
    Optional<Link> findByAlias(String alias);
    List<Link> findByOwner(User owner);
    List<Link> findByOwnerId(UUID ownerId);
    List<Link> findByTeamId(UUID teamId);

    @Query("""
   select l from Link l
   where l.owner.id = :userId
      or l.team.id in (
         select tm.team.id from TeamMember tm
         where tm.user.id = :userId
           and tm.pending = false
      )
""")
    List<Link> findAllAccessibleLinks(UUID userId);


}

