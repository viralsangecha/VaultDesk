package com.vaultdesk.server.model;
public record LoginResponse(Boolean success, String message, String role, String fullName) {}