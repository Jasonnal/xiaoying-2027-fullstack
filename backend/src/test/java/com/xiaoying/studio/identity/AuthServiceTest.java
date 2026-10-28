package com.xiaoying.studio.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xiaoying.studio.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 账号密码身份的边界：注册校验、密码错误、口令不落明文、账号不可枚举。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private AppUserMapper userMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM app_session");
        jdbcTemplate.update("DELETE FROM app_user WHERE handle LIKE 'reg-%'");
    }

    @Test
    @DisplayName("注册后可用同一口令登录，且库中不存明文")
    void registerThenAuthenticate() {
        AppUser created = authService.register("reg-alice", "Alice", "secret123");

        AppUser loaded = userMapper.findByHandle("reg-alice");
        assertThat(loaded.passwordHash()).isNotEqualTo("secret123");
        assertThat(loaded.passwordHash()).startsWith("$2");

        AppUser authenticated = authService.authenticate("reg-alice", "secret123");
        assertThat(authenticated.id()).isEqualTo(created.id());
        // 账号大小写不敏感：统一转小写
        assertThat(authService.authenticate("REG-ALICE", "secret123").id()).isEqualTo(created.id());
    }

    @Test
    @DisplayName("密码错误与账号不存在返回同一个错误，避免账号枚举")
    void wrongPasswordAndUnknownHandleLookTheSame() {
        authService.register("reg-bob", "Bob", "secret123");

        assertThatThrownBy(() -> authService.authenticate("reg-bob", "wrong-pass"))
                .isInstanceOf(ApiException.class)
                .hasMessage("账号或密码不正确");
        assertThatThrownBy(() -> authService.authenticate("reg-nobody", "secret123"))
                .isInstanceOf(ApiException.class)
                .hasMessage("账号或密码不正确");
    }

    @Test
    @DisplayName("重复注册与弱口令被拒绝")
    void rejectDuplicateHandleAndWeakPassword() {
        authService.register("reg-carol", "Carol", "secret123");

        assertThatThrownBy(() -> authService.register("reg-carol", "Carol2", "secret123"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已被占用");
        assertThatThrownBy(() -> authService.register("reg-dave", "Dave", "123"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("密码长度");
        assertThatThrownBy(() -> authService.register("bad handle!", "X", "secret123"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("账号只能包含");
    }

    @Test
    @DisplayName("演示账号可用统一演示口令登录")
    void demoAccountsSharePassword() {
        authService.ensureDemoAccounts();
        AppUser demo = authService.authenticate("demo", AuthService.DEMO_PASSWORD);
        assertThat(demo.handle()).isEqualTo("demo");
    }
}
