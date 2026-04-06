package com.guiderag.auth.controller;

import com.guiderag.auth.model.dto.ChangePasswordReqDTO;
import com.guiderag.auth.model.dto.UpdateProfileReqDTO;
import com.guiderag.auth.service.AuthService;
import com.guiderag.common.exception.AuthException;
import com.guiderag.common.result.Result;
import com.guiderag.common.utils.JwtUtils;
import com.guiderag.common.utils.MinioUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "个人资料接口", description = "提供用户头像上传、资料修改、密码修改等功能")
@RestController
@RequestMapping("/rag/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final MinioUtils minioUtils;

    @Operation(summary = "修改个人资料", description = "修改当前登录用户的显示名称")
    @PutMapping("/profile")
    public Result<Void> updateProfile(
            HttpServletRequest request,
            @Validated @RequestBody UpdateProfileReqDTO reqDTO
    ) {
        Long userId = getUserId(request);
        authService.updateProfile(userId, reqDTO);
        return Result.success(null);
    }

    @Operation(summary = "上传头像", description = "上传图片到 MinIO 并更新用户头像字段")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = getUserId(request);
        // 1. 上传文件到 MinIO
        String objectName = minioUtils.uploadFile(file);
        // 2. 获取预览链接 (或者直接存储 objectName，由前端根据获取用户信息接口返回的链接展示)
        // 这里我们存储 objectName，但在查询用户信息时转换为预览链接，或者在这里直接返回预览链接
        String previewUrl = minioUtils.getPreviewUrl(objectName);
        
        // 3. 更新数据库
        authService.updateAvatar(userId, objectName);
        
        return Result.success(previewUrl);
    }

    @Operation(summary = "修改密码", description = "验证旧密码后更新为新密码")
    @PutMapping("/password")
    public Result<Void> changePassword(
            HttpServletRequest request,
            @Validated @RequestBody ChangePasswordReqDTO reqDTO
    ) {
        Long userId = getUserId(request);
        authService.changePassword(userId, reqDTO);
        return Result.success(null);
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = JwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new AuthException("未授权的访问或Token已过期");
        }
        return userId;
    }
}
