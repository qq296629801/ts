package com.ts.platform.auth;

import com.ts.platform.common.ApiResponse;
import com.ts.platform.common.BusinessException;
import com.ts.platform.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SmsCodeService smsCodeService;
    private final EmailCodeService emailCodeService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            AuthService authService,
            SmsCodeService smsCodeService,
            EmailCodeService emailCodeService,
            JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.smsCodeService = smsCodeService;
        this.emailCodeService = emailCodeService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/send-sms")
    public ApiResponse<Void> sendSms(@RequestBody Map<String, String> body) {
        smsCodeService.send(body.get("phone"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/send-email")
    public ApiResponse<Void> sendEmail(@RequestBody Map<String, String> body) {
        emailCodeService.send(body.get("email"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterBody body) {
        var req = new AuthService.RegisterRequest(
                body.phone(), body.email(), body.password(), body.nickname(),
                body.verifyCode(), body.inviteCode());
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginBody body) {
        var req = new AuthService.LoginRequest(body.account(), body.password(), body.rememberMe());
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(401, "未提供有效 Token");
        }
        try {
            var claims = jwtTokenProvider.parse(authorization.substring(7));
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            String newToken = jwtTokenProvider.createToken(userId, role, false);
            return ApiResponse.ok(Map.of("accessToken", newToken));
        } catch (Exception e) {
            throw new BusinessException(401, "Token 无效或已过期");
        }
    }

    public record RegisterBody(
            String phone,
            String email,
            @NotBlank String password,
            @NotBlank String nickname,
            @NotBlank String verifyCode,
            String inviteCode) {
    }

    public record LoginBody(
            @NotBlank String account,
            @NotBlank String password,
            boolean rememberMe) {
    }
}
