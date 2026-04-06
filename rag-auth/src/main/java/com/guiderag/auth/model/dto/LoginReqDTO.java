package com.guiderag.auth.model.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户登录请求参数实体")
@Data
public class LoginReqDTO {
    @Schema(description = "用户邮箱地址", example = "zhangsan@gmail.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "强密码明文", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码图片包含的字符", example = "a2bc")
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @Schema(description = "生成由于验证码时返回的全局唯一凭证", example = "uuid-xxx-yyy")
    @NotBlank(message = "验证码流水号不能为空")
    private String captchaUuid;
}
