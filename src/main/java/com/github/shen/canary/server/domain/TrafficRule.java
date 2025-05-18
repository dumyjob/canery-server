package com.github.shen.canary.server.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TrafficRule {

    auto("全自动模式", 0),
    manual("手动控制模式", 1);

    private final String description;
    private final int code;
}
