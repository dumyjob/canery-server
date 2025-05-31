package com.github.shen.canary.server.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class CanaryTimer {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    public static String formate(final LocalDateTime dateTime) {
        return formatter.format(dateTime);
    }

}
