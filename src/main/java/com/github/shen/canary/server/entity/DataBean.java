package com.github.shen.canary.server.entity;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataBean {

    @Column(name = "create_dt")
    protected LocalDateTime createDt;
    @Column(name = "create_by")
    protected String createBy;
    @Column(name = "update_dt")
    protected LocalDateTime updateDt;
    @Column(name = "update_by")
    protected String updateBy;

    @Column(name = "is_delete")
    protected Boolean valid;

}
