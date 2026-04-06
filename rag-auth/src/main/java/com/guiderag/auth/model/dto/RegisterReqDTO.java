package com.guiderag.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户注册请求参数实体")
@Data
public class RegisterReqDTO {

    @Schema(description = "用户展示昵称", example = "极客阿三")
    @NotBlank(message = "用户名不能为空")
    @Length(min = 2, max = 20, message = "用户名长度在 2-20 个字符之间")
    private String username;

    @Schema(description = "用户邮箱(将作为登录账号)", example = "user@demo.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "强密码明文", example = "SecurePass123!")
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 32, message = "密码长度在 6-32 个字符之间")
    private String password;

    @Schema(description = "生成验证码接口返回的UUID流水凭证", example = "uuid-xxx-yyy")
    @NotBlank(message = "验证码凭证不能为空")
    private String captchaUuid;

    @Schema(description = "前端用户肉眼识别输入的字串", example = "6x29")
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
