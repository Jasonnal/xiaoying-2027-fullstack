package com.xiaoying.studio.identity;

import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.config.AppProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号密码身份服务。
 *
 * <p>设计说明：
 * <ul>
 *   <li>口令使用 BCrypt 哈希落库（只引 {@code spring-security-crypto}，不启用整套 Security 过滤器链）；</li>
 *   <li>登录成功后签发不透明 UUID 会话令牌，存 {@code app_session} 表，通过 HttpOnly + SameSite=Lax Cookie 下发；</li>
 *   <li>对外统一返回「账号或密码不正确」，不区分"账号不存在"与"密码错误"，避免账号枚举；</li>
 *   <li>预置 3 个演示账号（同一演示口令），方便验收者不注册也能体验多用户隔离。</li>
 * </ul>
 */
@Service
public class AuthService {

    /** 演示账号口令：写在 README 与登录页提示里，仅用于本地演示。 */
    public static final String DEMO_PASSWORD = "demo1234";

    private static final List<DemoAccount> DEMO_ACCOUNTS = List.of(
            new DemoAccount("demo", "演示账号", "#22D3A8", DEMO_PASSWORD),
            new DemoAccount("lin", "林小满", "#7C6CFF", DEMO_PASSWORD),
            new DemoAccount("chen", "陈声", "#F5A524", DEMO_PASSWORD));

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,32}$");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 64;

    private final AppUserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserMapper userMapper, SessionMapper sessionMapper, AppProperties properties) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.properties = properties;
    }

    @Transactional
    public AppUser register(String rawHandle, String displayName, String rawPassword) {
        String handle = normalizeHandle(rawHandle);
        String password = validatePassword(rawPassword);

        if (userMapper.findByHandle(handle) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "HANDLE_TAKEN", "账号「" + handle + "」已被占用，换一个或直接登录");
        }

        String name = (displayName == null || displayName.isBlank()) ? handle : displayName.trim();
        AppUser created = new AppUser(
                UUID.randomUUID().toString(),
                handle,
                name.length() > 32 ? name.substring(0, 32) : name,
                passwordEncoder.encode(password),
                pickAccent(handle),
                LocalDateTime.now());
        userMapper.insert(created);
        return created;
    }

    /** 账号密码校验；失败统一 401。 */
    public AppUser authenticate(String rawHandle, String rawPassword) {
        String handle = rawHandle == null ? "" : rawHandle.trim().toLowerCase();
        String password = rawPassword == null ? "" : rawPassword;
        if (handle.isEmpty() || password.isEmpty()) {
            throw invalidCredentials();
        }

        AppUser user = userMapper.findByHandle(handle);
        if (user == null || user.passwordHash() == null
                || !passwordEncoder.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
        return user;
    }

    public String issueToken(String userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        sessionMapper.insert(token, userId, now, now.plusHours(properties.auth().sessionTtlHours()));
        return token;
    }

    /** 令牌无效/过期都返回 null，由拦截器决定是否放行。 */
    public AppUser resolve(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String userId = sessionMapper.findValidUserId(token, LocalDateTime.now());
        return userId == null ? null : userMapper.findById(userId);
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessionMapper.delete(token);
        }
    }

    public List<DemoAccount> demoAccounts() {
        return DEMO_ACCOUNTS;
    }

    public String cookieName() {
        return properties.auth().cookieName();
    }

    public int sessionTtlSeconds() {
        return properties.auth().sessionTtlHours() * 3600;
    }

    /** 启动时补齐演示账号；已存在但缺口令的旧数据顺手补上哈希。 */
    public void ensureDemoAccounts() {
        for (DemoAccount account : DEMO_ACCOUNTS) {
            AppUser existing = userMapper.findByHandle(account.handle());
            if (existing == null) {
                userMapper.insert(new AppUser(
                        UUID.randomUUID().toString(),
                        account.handle(),
                        account.displayName(),
                        passwordEncoder.encode(account.password()),
                        account.accentColor(),
                        LocalDateTime.now()));
            } else if (existing.passwordHash() == null || existing.passwordHash().isBlank()) {
                userMapper.updatePassword(existing.id(), passwordEncoder.encode(account.password()));
            }
        }
    }

    private String normalizeHandle(String rawHandle) {
        String handle = rawHandle == null ? "" : rawHandle.trim().toLowerCase();
        if (!HANDLE_PATTERN.matcher(handle).matches()) {
            throw ApiException.badRequest("INVALID_HANDLE",
                    "账号只能包含字母、数字、下划线和短横线（2-32 位）");
        }
        return handle;
    }

    private String validatePassword(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw ApiException.badRequest("WEAK_PASSWORD",
                    "密码长度需在 " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " 位之间");
        }
        return password;
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码不正确");
    }

    private String pickAccent(String handle) {
        String[] palette = {"#22D3A8", "#7C6CFF", "#F5A524", "#3B82F6", "#EC4899"};
        return palette[Math.floorMod(handle.hashCode(), palette.length)];
    }

    public record DemoAccount(String handle, String displayName, String accentColor, String password) {
    }
}
