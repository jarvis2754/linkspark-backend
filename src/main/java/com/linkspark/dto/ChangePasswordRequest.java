package com.linkspark.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}