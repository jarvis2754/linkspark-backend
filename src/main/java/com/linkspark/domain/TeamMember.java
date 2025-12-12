package com.linkspark.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "team_members")
@Data
@NoArgsConstructor
public class TeamMember {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /**
     * Can be null while invite is pending.
     * Once invite is accepted this points to the actual user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * We keep the email on the member row so we can show pending invites
     * without requiring a User row to exist.
     */
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role; // owner/admin/editor/viewer

    @Column(nullable = false)
    private boolean pending = true;
}
