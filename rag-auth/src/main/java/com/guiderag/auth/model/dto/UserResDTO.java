package com.guiderag.auth.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户信息响应数据结构")
@Data
public class UserResDTO {
    @Schema(description = "用户自增主键Id")
    private Long id;
    @Schema(description = "显示名称昵称", example = "王小牛")
    private String displayName;
    @Schema(description = "绑定邮箱", example = "cow@163.com")
    private String email;
    @Schema(description = "头像路径或URL")
    private String avatar;
    @Schema(description = "状态：active/disabled", example = "active")
    private String status;
    @Schema(description = "开户注册时间")
    private LocalDateTime gmtCreate;
}
