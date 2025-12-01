package com.dev.lib.security.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenType {
    INTERNAL,
    PUBLIC,
    ACCESS
}
