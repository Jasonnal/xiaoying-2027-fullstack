package com.xiaoying.studio.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(
            @NotBlank(message = "不能为空") String handle,
            @NotBlank(message = "不能为空") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "不能为空") String handle,
            @NotBlank(message = "不能为空") @Size(min = 6, max = 64, message = "长度需在 6-64 位之间") String password,
            String displayName) {
    }

    /** 出参不含 passwordHash。 */
    public record UserResponse(String id, String handle, String displayName, String accentColor) {
    }

    /** 演示账号直接带上演示口令，登录页可一键填入（仅演示用途）。 */
    public record DemoAccountResponse(String handle, String displayName, String accentColor, String password) {
    }

    /** 登录：账号 + 密码。 */
    @PostMapping("/auth/session")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AppUser user = authService.authenticate(request.handle(), request.password());
        return ResponseEntity.ok(toResponse(user, response));
    }

    /** 注册：注册成功直接登录。 */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletResponse response) {
        AppUser user = authService.register(request.handle(), request.displayName(), request.password());
        return toResponse(user, response);
    }

    @DeleteMapping("/auth/session")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(readToken(request));
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
                .map(a -> new DemoAccountResponse(a.handle(), a.displayName(), a.accentColor(), a.password()))
                .toList();
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUser AppUser user) {
        return new UserResponse(user.id(), user.handle(), user.displayName(), user.accentColor());
    }

    private UserResponse toResponse(AppUser user, HttpServletResponse response) {
        String token = authService.issueToken(user.id());
        ResponseCookie cookie = ResponseCookie.from(authService.cookieName(), token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(authService.sessionTtlSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
