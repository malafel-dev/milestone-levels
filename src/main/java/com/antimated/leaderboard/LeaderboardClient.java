package com.antimated.leaderboard;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import okhttp3.*;

import javax.inject.Inject;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Facilitates making requests to the OSRS hiscores website, specifically for "leaderboard" pages under specific skills.
 * The interface returns that data as `LeaderboardResult` objects, which currently must be assembled by parsing HTML
 * pages with `LeaderboardParser`. This parsing is done because there is no known public-facing API for the leaderboard
 * data.
 */
@Slf4j
public class LeaderboardClient {
    private final OkHttpClient client;
    private final Gson gson;

    @Inject
    private LeaderboardClient(OkHttpClient client, Gson gson)
    {
        this.client = client;
        this.gson = gson;
    }

    public CompletableFuture<LeaderboardResult> lookupAsync(Skill skill, int page, LeaderboardEndpoint endpoint) {
        HttpUrl url = endpoint.getLeaderboardURL().newBuilder()
            .addQueryParameter("table", String.valueOf(SkillTable.valueOf(skill.name()).tableNumber))
            .addQueryParameter("page", String.valueOf(page))
            .build();

        Request request = new Request.Builder()
            .url(url)
            .build();

        CompletableFuture<LeaderboardResult> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    LeaderboardResult result;
                    try {
                        String documentContents = response.body().string();
                        result = LeaderboardParser.parseDocument(documentContents);
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
            }
        });

        return future;
    }


}
