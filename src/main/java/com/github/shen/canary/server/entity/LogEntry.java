package com.github.shen.canary.server.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LogEntry {

    private String timestamp;
    private String content;
    private String level;   // INFO/WARN/ERROR

    private String step; // git build deploy

    private Boolean highlight;
//    private ErrorSuggestion suggestion;

}
