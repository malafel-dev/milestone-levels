package com.antimated.leaderboard;

import lombok.Getter;
import okhttp3.HttpUrl;

@Getter
public enum ValidLeaderboard {
    NORMAL("Normal"),
    IRONMAN("Ironman"),
    HARDCORE_IRONMAN("Hardcore Ironman"),
    ULTIMATE_IRONMAN("Ultimate Ironman");

    private final String name;

    ValidLeaderboard(String name) {
        this.name = name;
    }
}
