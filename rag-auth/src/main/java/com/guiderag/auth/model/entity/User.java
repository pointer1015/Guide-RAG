package com.guiderag.auth.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String displayName;
    private String email;
    private String avatar;
    private String passwordHash;
    private String status;
    private Integer isDeleted;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
