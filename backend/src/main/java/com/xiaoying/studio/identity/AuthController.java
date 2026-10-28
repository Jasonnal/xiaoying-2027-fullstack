package com.xiaoying.studio.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(@NotBlank(message = "不能为空") String handle, String displayName) {
    }

    public record UserResponse(String id, String handle, String displayName, String accentColor) {
    }

    public record DemoAccountResponse(String handle, String displayName, String accentColor) {
    }

    @PostMapping("/auth/session")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AppUser user = authService.login(request.handle(), request.displayName());
        String token = authService.issueToken(user.id());

        ResponseCookie cookie = ResponseCookie.from(authService.cookieName(), token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(authService.sessionTtlSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/auth/session")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = readToken(request);
        authService.logout(token);
        ResponseCookie cookie = ResponseCookie.from(authService.cookieName(), "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/auth/accounts")
    public List<DemoAccountResponse> accounts() {
        return authService.demoAccounts().stream()
                .map(a -> new DemoAccountResponse(a.handle(), a.displayName(), a.accentColor()))
                .toList();
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUser AppUser user) {
        return toResponse(user);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.id(), user.handle(), user.displayName(), user.accentColor());
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authService.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
