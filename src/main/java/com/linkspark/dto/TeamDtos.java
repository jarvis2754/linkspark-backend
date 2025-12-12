package com.linkspark.dto;

import java.util.List;

public class TeamDtos {

    public record CreateTeamRequest(String name) {}

    public record InviteRequest(String email, String role) {}

    public record ChangeRoleRequest(String role) {}

    public record TeamMemberDto(
            String id,
            String userId,
            String name,
            String email,
            String role,
            boolean pending
    ) {}

    public record TeamDto(
            String id,
            String name,
            List<TeamMemberDto> members
    ) {}

    public static record TeamSummaryDto(
            String id,
            String name,
            String role
    ) {}

    public record InviteInfo(
            String token,
            String teamId,
            String teamName,
            String role,
            String expiresAt
    ) {}

}
