package com.github.shen.canary.server.web.request;

import lombok.Data;

@Data
public class StatusEntry {

    private String status;

    private String progress;

    private String logs;
}
