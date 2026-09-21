package com.eximee.los.dto;

public record AuthResponse(
    String token,
    UserDto user
) {}
