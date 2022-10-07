package com.samifying.hideylink.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class DataModel {
    private String id;
    private String name;
    private String nickname;
    private String avatar;
    private boolean moderator;
    private boolean supporter;
}
