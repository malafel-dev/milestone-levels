package com.antimated.leaderboard;

import lombok.Getter;
import okhttp3.HttpUrl;

/**
 * Maps the various game modes to leaderboard domains on the OSRS hiscores website. This parallels `HiscoreEndpoint` in
 * the RuneLite code base.
 */
@Getter
public enum LeaderboardEndpoint {
    NORMAL("Normal", "https://services.runescape.com/m=hiscore_oldschool/overall"),
    IRONMAN("Ironman", "https://services.runescape.com/m=hiscore_oldschool_ironman/overall"),
    HARDCORE_IRONMAN("Hardcore Ironman", "https://services.runescape.com/m=hiscore_oldschool_hardcore_ironman/overall"),
    ULTIMATE_IRONMAN("Ultimate Ironman", "https://services.runescape.com/m=hiscore_oldschool_ultimate/overall"),
    DEADMAN("Deadman", "https://services.runescape.com/m=hiscore_oldschool_deadman/overall"),
    SEASONAL("Leagues", "https://services.runescape.com/m=hiscore_oldschool_seasonal/overall"),
    TOURNAMENT("Tournament", "https://services.runescape.com/m=hiscore_oldschool_tournament/overall"),
    FRESH_START_WORLD("Fresh Start", "https://secure.runescape.com/m=hiscore_oldschool_fresh_start/overall"),
    PURE("1 Defence Pure", "https://secure.runescape.com/m=hiscore_oldschool_skiller_defence/overall"),
    LEVEL_3_SKILLER("Level 3 Skiller", "https://secure.runescape.com/m=hiscore_oldschool_skiller/overall");

    private final String name;
    private final HttpUrl leaderboardURL;

    LeaderboardEndpoint(String name, String leaderboardURL) {
        this.name = name;
        this.leaderboardURL = HttpUrl.get(leaderboardURL);
    }
}
