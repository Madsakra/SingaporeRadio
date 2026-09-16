package com.oai.singaporeradio;

import android.content.Context;
import java.util.Locale;

/** UI-only labels and assets. Station identities and stream URLs stay in the existing catalog. */
final class StationPresentation {
    static final int[] LANGUAGE_LABELS = {R.string.language_mandarin, R.string.language_english,
        R.string.language_malay, R.string.language_tamil, R.string.language_cantonese, R.string.language_korean};

    static boolean matches(Station station, String query) {
        String needle = normalize(query);
        // Whitespace-insensitive names/frequencies also accept "yes933" and "93.3".
        return needle.isEmpty() || normalize(station.name).contains(needle)
            || normalize(station.subtitle.split(" • ")[0]).contains(needle);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    static String subtitle(Context context, Station station) {
        String value = station.subtitle.replace(" • ", " · ");
        String[] words = {"Mandarin", "English", "Malay", "Tamil", "Cantonese", "Korean",
            "Online", "Singapore only", "Classical", "World feed", "Indie", "Dance"};
        int[] resources = {R.string.language_mandarin, R.string.language_english,
            R.string.language_malay, R.string.language_tamil, R.string.language_cantonese,
            R.string.language_korean, R.string.online, R.string.singapore_only,
            R.string.classical, R.string.world_feed, R.string.indie, R.string.dance};
        for (int i = 0; i < words.length; i++) value = value.replace(words[i], context.getString(resources[i]));
        return value;
    }

    static Station named(String name) {
        for (Station station : StationData.ALL) if (station.name.equals(name)) return station;
        return null;
    }

    static int logo(Station station) {
        if (station == null) return R.drawable.ic_radio;
        switch (station.name) {
            case "YES 933": return R.drawable.station_yes_933;
            case "LOVE 972": return R.drawable.station_love_972;
            case "CAPITAL 958": return R.drawable.station_capital_958;
            case "96.3好FM": return R.drawable.station_hao_963;
            case "UFM100.3": return R.drawable.station_ufm_1003;
            case "MONEY FM 89.3": return R.drawable.station_money_893;
            case "GOLD 905": return R.drawable.station_gold_905;
            case "ONE FM 91.3": return R.drawable.station_one_913;
            case "Kiss92 FM": return R.drawable.station_kiss_92;
            case "Symphony 924": return R.drawable.station_symphony_924;
            case "CNA938": return R.drawable.station_cna_938;
            case "CLASS 95": return R.drawable.station_class_95;
            case "987": return R.drawable.station_987;
            case "RIA 897": return R.drawable.station_ria_897;
            case "WARNA 942": return R.drawable.station_warna_942;
            case "OLI 968": return R.drawable.station_oli_968;
            case "BBC World Service": return R.drawable.station_bbc_world_service;
            case "indiego": return R.drawable.station_indiego;
            case "88.3JIA": return R.drawable.station_jia_883;
            case "POWER 98": return R.drawable.station_power_98;
            case "88.3JIA Trending Hits": return R.drawable.station_jia_trending;
            case "88.3JIA Cantopop": return R.drawable.station_jia_cantopop;
            case "88.3JIA K-Pop": return R.drawable.station_jia_kpop;
            case "POWER 98 Mixtape": return R.drawable.station_power_98_mixtape;
            case "POWER 98 EDM Club Hits": return R.drawable.station_power_98_edm;
            case "POWER 98 EMERGENC-E": return R.drawable.station_power_98_emergenc_e;
            default: return R.drawable.ic_radio;
        }
    }
}
