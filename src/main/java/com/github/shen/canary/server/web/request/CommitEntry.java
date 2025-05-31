package com.github.shen.canary.server.web.request;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CommitEntry {

    private String commitId;
    private String message;
}
