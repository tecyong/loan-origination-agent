package com.eximee.los.dto;

import com.eximee.los.domain.Role;

public record UserDto(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    Role role
) {}
