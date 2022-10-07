package com.samifying.hideylink.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ErrorModel {
    private String name;
    private String message;
    private String path;
    private long timestamp;
}
