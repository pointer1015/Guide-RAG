package com.guiderag.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "图形验证码响应数据实体")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResDTO {
    @Schema(description = "全局唯一流水号，注册或登录时需携带回传")
    private String uuid;
    @Schema(description = "带前缀的Base64图片编码：data:image/png;base64,...")
    private String base64Image;
}
