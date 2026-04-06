package com.guiderag.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.guiderag.auth.mapper.UserMapper;
import com.guiderag.auth.model.dto.CaptchaResDTO;
import com.guiderag.auth.model.dto.LoginReqDTO;
import com.guiderag.auth.model.dto.RegisterReqDTO;
import com.guiderag.auth.model.dto.UserResDTO;
import com.guiderag.auth.model.dto.UpdateProfileReqDTO;
import com.guiderag.auth.model.dto.ChangePasswordReqDTO;
import com.guiderag.auth.model.entity.User;
import com.guiderag.auth.service.AuthService;
import com.guiderag.common.constant.AuthConstants;
import com.guiderag.common.exception.AuthException;
import com.guiderag.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final com.guiderag.common.utils.MinioUtils minioUtils;
    private final BCryptPasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder();

    // 生成验证码并存储到 Redis，返回验证码图片的 Base64 编码和对应的 UUID
    @Override
    public CaptchaResDTO generateCaptcha(String ip) {
        // 限流防刷校验：同一 IP 1 分钟最多获取 10 次
        String rateLimitKey = "rate_limit:captcha:" + ip;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }
        if (count != null && count > 10) {
            throw new AuthException("请求过于频繁，请稍后再试");
        }

        // 生成验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 30);
        String code = lineCaptcha.getCode();
        String uuid = IdUtil.fastSimpleUUID();

        // 存入 Redis，有效期 5 分钟
        String redisKey = AuthConstants.CAPTCHA_CODE_KEY + uuid;
        redisTemplate
            .opsForValue()
            .set(
                redisKey,
                code,
                Duration.ofMinutes(AuthConstants.CAPTCHA_EXPIRATION)
            );

        return new CaptchaResDTO(uuid, lineCaptcha.getImageBase64Data());
    }

    // 登录逻辑：校验验证码、校验用户邮箱和密码，成功后签发 JWT Token
    @Override
    public String login(LoginReqDTO reqDTO) {
        // 校验验证码
        String redisKey =
            AuthConstants.CAPTCHA_CODE_KEY + reqDTO.getCaptchaUuid();
        String cachedCode = redisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null) {
            throw new AuthException("验证码已过期");
        }
        // 阅后即焚
        redisTemplate.delete(redisKey);
        if (!cachedCode.equalsIgnoreCase(reqDTO.getCaptchaCode())) {
            throw new AuthException("验证码错误");
        }

        // 校验用户邮箱
        User user = userMapper.selectByEmail(reqDTO.getEmail());
        if (user == null) {
            throw new AuthException("用户不存在或已被禁用");
        }

        // 校验密码
        if (
            !passwordEncoder.matches(
                reqDTO.getPassword(),
                user.getPasswordHash()
            )
        ) {
            throw new AuthException("邮箱或密码错误");
        }

        // 签发 JWT
        return JwtUtils.generateToken(
            user.getId(),
            user.getDisplayName(),
            "USER"
        );
    }

    // 注册逻辑：校验验证码、校验邮箱和用户名唯一性，成功后保存用户信息并直接签发 JWT（避免前端二次登录消耗已销毁的验证码）
    @Override
    public String register(RegisterReqDTO reqDTO) {
        // 校验验证码
        String redisKey =
            AuthConstants.CAPTCHA_CODE_KEY + reqDTO.getCaptchaUuid();
        String cachedCode = redisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null) {
            throw new AuthException("验证码已过期");
        }
        redisTemplate.delete(redisKey);
        if (!cachedCode.equalsIgnoreCase(reqDTO.getCaptchaCode())) {
            throw new AuthException("验证码错误");
        }

        // 校验邮箱和用户名唯一性
        if (userMapper.selectByEmail(reqDTO.getEmail()) != null) {
            throw new AuthException("该邮箱已被注册");
        }
        if (userMapper.selectByDisplayName(reqDTO.getUsername()) != null) {
            throw new AuthException("该用户名已被注册");
        }

        // 保存用户
        User user = new User();
        user.setId(IdUtil.getSnowflakeNextId()); // 使用雪花算法生成唯一 bigint 主键
        user.setDisplayName(reqDTO.getUsername());
        user.setEmail(reqDTO.getEmail());
        user.setAvatar(""); // 初始化头像为空字符串
        user.setPasswordHash(passwordEncoder.encode(reqDTO.getPassword()));
        user.setStatus("active"); // 默认启用
        userMapper.insert(user);

        // 注册成功后直接签发 JWT，前端无需再发起一次带验证码的登录请求
        return JwtUtils.generateToken(
            user.getId(),
            user.getDisplayName(),
            "USER"
        );
    }

    // 注销逻辑：将 JWT Token 加入黑名单，黑名单有效期为 Token 的剩余有效期
    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = JwtUtils.parseToken(token);
        if (claims != null) {
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                String blacklistKey = "auth:blacklist:token:" + token;
                redisTemplate
                    .opsForValue()
                    .set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public UserResDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserResDTO resDTO = new UserResDTO();
        resDTO.setId(user.getId());
        resDTO.setDisplayName(user.getDisplayName());
        resDTO.setEmail(user.getEmail());
        
        // 将存储的对象名转换为预览链接
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            resDTO.setAvatar(minioUtils.getPreviewUrl(user.getAvatar()));
        } else {
            resDTO.setAvatar("");
        }
        
        resDTO.setStatus(user.getStatus());
        resDTO.setGmtCreate(user.getGmtCreate());
        return resDTO;
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileReqDTO reqDTO) {
        User user = new User();
        user.setId(userId);
        user.setDisplayName(reqDTO.getDisplayName());
        userMapper.updateById(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordReqDTO reqDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        
        // 校验旧密码
        if (!passwordEncoder.matches(reqDTO.getOldPassword(), user.getPasswordHash())) {
            throw new AuthException("旧密码错误");
        }
        
        // 更新为新密码
        User update = new User();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(reqDTO.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public String refreshToken(String oldToken) {
        if (oldToken != null && oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }
        Claims claims = JwtUtils.parseToken(oldToken);
        if (claims == null) {
            throw new AuthException("无效或已过期的Token，无法刷新");
        }

        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 将旧Token加入黑名单
        long ttl =
            claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            String blacklistKey = "auth:blacklist:token:" + oldToken;
            redisTemplate
                .opsForValue()
                .set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
        }

        return JwtUtils.generateToken(userId, username, role);
    }
}
