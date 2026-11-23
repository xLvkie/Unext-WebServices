package com.nextworks.unextwebservices.agent.tools;

import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.function.Supplier;

@Component
public class AgentRuleGuard {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    public User extractAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Debe iniciar sesión con un token JWT válido");
        }
        return user;
    }

    public User requireRole(Authentication authentication, Role... allowedRoles) {
        User user = extractAuthenticatedUser(authentication);
        boolean allowed = Arrays.stream(allowedRoles).anyMatch(r -> r == user.getRole());
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos para ejecutar esta funcionalidad de IA con el rol " + user.getRole());
        }
        return user;
    }

    public User requireCurrentUserWithRole(Role expectedRole) {
        User user = resolveCurrentUser();
        if (user.getRole() != expectedRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acceso denegado: herramienta exclusiva para el segmento " + expectedRole.name());
        }
        return user;
    }

    public User resolveCurrentUserOptional() {
        User threadUser = CURRENT_USER.get();
        if (threadUser != null) {
            return threadUser;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    public <T> T executeWithUser(User user, Supplier<T> action) {
        User previous = CURRENT_USER.get();
        CURRENT_USER.set(user);
        try {
            return action.get();
        } finally {
            if (previous != null) {
                CURRENT_USER.set(previous);
            } else {
                CURRENT_USER.remove();
            }
        }
    }

    private User resolveCurrentUser() {
        User user = resolveCurrentUserOptional();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Debe iniciar sesión con un token JWT válido");
        }
        return user;
    }
}
