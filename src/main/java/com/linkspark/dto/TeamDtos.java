package com.linkspark.dto;

public class TeamDtos {

    public static record InviteRequest(String email, String role) {}

    public static record ChangeRoleRequest(String role) {}

    public static record TeamMemberDto(String id, String email, String role, boolean pending) {}
}

