package com.vaultdesk.server.model;

public record LoginResponse(Boolean success,String message,String rol,String fullname  ) {
}
