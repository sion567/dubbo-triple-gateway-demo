package com.example.dubbo.user;

import com.example.dubbo.user.api.AccountService;
import com.example.dubbo.user.api.vo.LoginRequest;
import com.example.dubbo.security.JwtUtil;
import com.example.dubbo.user.mapper.AccountMapper;
import com.example.dubbo.user.mapper.UserMapper;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@DubboService
public class AccountServiceImpl implements AccountService {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    public AccountServiceImpl(UserMapper userMapper, AccountMapper accountMapper) {
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        String username = request.getUsername();
        Map<String, Object> account = accountMapper.findByUsername(username);

        if (account == null || !request.getPassword().equals(account.get("password"))) {
            System.out.println("🚫 [UserService] 登录失败: " + username);
            return Map.of("code", 401, "message", "用户名或密码错误");
        }

        String rolesStr = (String) account.get("roles");
        String permsStr = (String) account.getOrDefault("perms", "");
        List<String> roles = Arrays.asList(rolesStr.split(","));
        List<String> perms = permsStr.isBlank() ? List.of() : Arrays.asList(permsStr.split(","));
        String token = JwtUtil.createToken(username, roles, perms);

        Long userId = (Long) account.get("user_id");
        Map<String, Object> user = userMapper.selectById(userId);
        String name = user != null ? (String) user.get("name") : "-";
        System.out.println("🔓 [User-" + userId + "] 登录成功: " + username + ", name=" + name + ", roles=" + roles);

        return Map.of(
                "code", 0,
                "userId", userId,
                "username", username,
                "name", name,
                "roles", roles,
                "perms", perms,
                "token", token,
                "tokenType", "Bearer",
                "expiresAt", Instant.now().plusSeconds(2 * 3600).toString());
    }
}
