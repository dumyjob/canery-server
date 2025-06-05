package com.github.shen.canary.server.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProjectType {

    SPRING_BOOT("spring-boot"),
    REACT("react"),
    VUE("vue"),
    PYTHON("python"),
    ;

    private final String name;
}
