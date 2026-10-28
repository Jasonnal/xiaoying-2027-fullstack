package com.xiaoying.studio.identity;

import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.config.AppProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示型身份服务。
 *
 * <p>设计取舍：题目不要求账号体系（R-10 只要求"自行设计并说明"），因此这里不做口令校验，
 * 用「handle 即身份」的轻量方案：同一 handle 复用同一账号，会话令牌是不透明 UUID 存在
 * {@code app_session} 表里，通过 HttpOnly Cookie 下发。这样切换用户 = 换一个 handle 登录，
 * 可以直观演示 R-08 要求的"切换用户时清理旧请求/旧订阅"。
 */
@Service
public class AuthService {

    /** 预置演示账号：让验收者不用注册就能体验多用户隔离。 */
    private static final List<DemoAccount> DEMO_ACCOUNTS = List.of(
            new DemoAccount("demo", "演示账号", "#22D3A8"),
            new DemoAccount("lin", "林小满", "#7C6CFF"),
            new DemoAccount("chen", "陈声", "#F5A524"));

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,32}$");

    private final AppUserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final AppProperties properties;

    public AuthService(AppUserMapper userMapper, SessionMapper sessionMapper, AppProperties properties) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.properties = properties;
    }

    @Transactional
    public AppUser login(String rawHandle, String displayName) {
        String handle = rawHandle == null ? "" : rawHandle.trim().toLowerCase();
        if (!HANDLE_PATTERN.matcher(handle).matches()) {
            throw ApiException.badRequest("INVALID_HANDLE", "账号标识只能包含字母、数字、下划线和短横线（2-32 位）");
        }

        AppUser existing = userMapper.findByHandle(handle);
        if (existing != null) {
            return existing;
        }

        String name = (displayName == null || displayName.isBlank()) ? handle : displayName.trim();
        AppUser created = new AppUser(
                UUID.randomUUID().toString(),
                handle,
                name.length() > 32 ? name.substring(0, 32) : name,
                pickAccent(handle),
                LocalDateTime.now());
        userMapper.insert(created);
        return created;
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

    public void ensureDemoAccounts() {
        for (DemoAccount account : DEMO_ACCOUNTS) {
            if (userMapper.findByHandle(account.handle()) == null) {
                userMapper.insert(new AppUser(
                        UUID.randomUUID().toString(),
                        account.handle(),
                        account.displayName(),
                        account.accentColor(),
                        LocalDateTime.now()));
            }
        }
    }

    private String pickAccent(String handle) {
        String[] palette = {"#22D3A8", "#7C6CFF", "#F5A524", "#3B82F6", "#EC4899"};
        return palette[Math.floorMod(handle.hashCode(), palette.length)];
    }

    public record DemoAccount(String handle, String displayName, String accentColor) {
    }
}
