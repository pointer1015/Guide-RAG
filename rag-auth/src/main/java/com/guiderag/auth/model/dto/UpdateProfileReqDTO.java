package com.guiderag.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "修改个人资料请求数据结")
@Data
public class UpdateProfileReqDTO {
    @Schema(description = "显示名称昵称", example = "王小牛")
    @NotBlank(message = "昵称不能为空")
    private String displayName;
}
