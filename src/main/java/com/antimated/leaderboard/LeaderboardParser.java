package com.antimated.leaderboard;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Provides parsing logic for leaderboard HTML documents. `LeaderboardParser` depends on the structure of the OSRS
 * hiscores pages keeping the same format, meaning this is one of the classes that's most likely to break as Jagex
 * introduces changes to their website.
 */
public class LeaderboardParser {
    /**
     * Parses the unstructured `documentContents` into a LeaderboardResult.
     *
     * @param documentContents String
     * @return LeaderboardResult
     * @throws ParseException when parsing of the document fails for any reason.
     */
    public static LeaderboardResult parseDocument(String documentContents) throws ParseException {
        Document document = Jsoup.parse(documentContents);
        Element tableOuterDiv = document.getElementById("contentHiscores");
        Element table = tableOuterDiv.selectFirst("table");
        Element tableBody = table.selectFirst("tbody");
        Elements rows = tableBody.children();

        ArrayList<LeaderboardEntry> entries = new ArrayList<LeaderboardEntry>();
        for (Element row: rows.asList()) {
            Elements tableData = row.children();
            // Element spaceElement = tableData.get(0);
            Element rankElement = tableData.get(1);
            Element nameElement = tableData.get(2);
            Element nameAElement = nameElement.firstElementChild();
            Element levelElement = tableData.get(3);
            Element xpElement = tableData.get(4);

            int rank = NumberFormat.getNumberInstance(java.util.Locale.US).parse(rankElement.text()).intValue();
            String name = nameAElement.text();
            int level = Integer.parseInt(levelElement.text());
            int xp = NumberFormat.getNumberInstance(java.util.Locale.US).parse(xpElement.text()).intValue();
            LeaderboardEntry entry = new LeaderboardEntry(name, rank, level, xp);
            entries.add(entry);
        }
        return new LeaderboardResult(entries);
    }
}
