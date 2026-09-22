package com.vaultdesk.server.security;

public class AuthContext {
    private static final ThreadLocal<TokenStore.TokenInfo> CURRENT = new ThreadLocal<>();

    public static void set(TokenStore.TokenInfo info) { CURRENT.set(info); }
    public static TokenStore.TokenInfo get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }

    /** Convenience: id of the currently authenticated user/employee, or 0 if none. */
    public static int currentSubjectId() {
        TokenStore.TokenInfo info = CURRENT.get();
        return info != null ? info.subjectId() : 0;
    }
}