package com.linkspark.dto;

import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String name,
        String company,
        String email,
        String provider
) {}

