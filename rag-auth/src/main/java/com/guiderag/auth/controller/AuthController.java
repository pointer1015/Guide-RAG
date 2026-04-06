package com.guiderag.auth.controller;

import com.guiderag.auth.model.dto.CaptchaResDTO;
import com.guiderag.auth.model.dto.LoginReqDTO;
import com.guiderag.auth.model.dto.RegisterReqDTO;
import com.guiderag.auth.model.dto.UserResDTO;
import com.guiderag.auth.service.AuthService;
import com.guiderag.common.exception.AuthException;
import com.guiderag.common.result.Result;
import com.guiderag.common.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "安全认证接口",
    description = "提供用户注册、登录、授权及验证码等相关功能"
)
@RestController
@RequestMapping("/rag/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
        summary = "获取图形验证码",
        description = "获取Base64编码的图形验证码，必须在登录或注册前调用"
    )
    @GetMapping("/captcha")
    public Result<CaptchaResDTO> getCaptcha(HttpServletRequest request) {
        // 获取真实 IP，在微服务架构下往往由网关透传 X-Forwarded-For 等头部，此处简化通过 request 获取
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        CaptchaResDTO captcha = authService.generateCaptcha(ip);
        return Result.success(captcha);
    }

    @Operation(
        summary = "用户登录",
        description = "使用邮箱和密码以及前端获取到的验证码进行登录，成功返回JWT Token"
    )
    @PostMapping("/login")
    public Result<String> login(
        @Validated @RequestBody LoginReqDTO loginReqDTO
    ) {
        String token = authService.login(loginReqDTO);
        return Result.success(token);
    }

    @Operation(
        summary = "账号注册",
        description = "填写邮箱密码并带上图形验证码进行注册，注册成功后直接返回JWT Token完成自动登录"
    )
    @PostMapping("/register")
    public Result<String> register(
        @Validated @RequestBody RegisterReqDTO registerReqDTO
    ) {
        String token = authService.register(registerReqDTO);
        return Result.success(token);
    }

    @Operation(
        summary = "安全登出",
        description = "将当前请求头中合法的Token置入Redis黑名单使之失效"
    )
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        authService.logout(token);
        return Result.success(null);
    }

    @Operation(
        summary = "获取当前用户信息",
        description = "依据请求头中的Token解析获取用户的基本信息（需要携带Token）"
    )
    @GetMapping("/me")
    public Result<UserResDTO> getMe(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = JwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new AuthException("未授权的访问或Token已过期");
        }
        UserResDTO userResDTO = authService.getUserInfo(userId);
        return Result.success(userResDTO);
    }

    @Operation(
        summary = "刷新授权令牌",
        description = "当Token即将过期时，带上老的可用Token换取寿命更长的新Token（旧Token将被拉黑）"
    )
    @PostMapping("/refresh")
    public Result<String> refresh(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String newToken = authService.refreshToken(token);
        return Result.success(newToken);
    }
}
