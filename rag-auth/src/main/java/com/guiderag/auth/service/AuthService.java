package com.guiderag.auth.service;

import com.guiderag.auth.model.dto.CaptchaResDTO;
import com.guiderag.auth.model.dto.LoginReqDTO;
import com.guiderag.auth.model.dto.RegisterReqDTO;
import com.guiderag.auth.model.dto.UserResDTO;
import com.guiderag.auth.model.dto.UpdateProfileReqDTO;
import com.guiderag.auth.model.dto.ChangePasswordReqDTO;

public interface AuthService {
    // 生成验证码，返回验证码图片的Base64编码和对应的UUID
    CaptchaResDTO generateCaptcha(String ip);

    // 登录成功返回 JWT Token
    String login(LoginReqDTO reqDTO);

    // 注册新用户，注册成功后直接签发 JWT Token 返回（避免二次验证码校验）
    String register(RegisterReqDTO reqDTO);

    // 注销用户，通常是将 JWT Token 加入黑名单或删除相关会话信息
    void logout(String token);

    // 获取用户信息，通常通过 JWT Token 中的用户ID来查询
    UserResDTO getUserInfo(Long userId);

    // 刷新Token，返回新的JWT Token
    String refreshToken(String oldToken);

    // 修改个人资料
    void updateProfile(Long userId, UpdateProfileReqDTO reqDTO);

    // 更新头像
    void updateAvatar(Long userId, String avatarUrl);

    // 修改密码
    void changePassword(Long userId, ChangePasswordReqDTO reqDTO);
}
