package com.oai.singaporeradio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static com.oai.singaporeradio.Station.Language.*;

/** Broadcaster sources and verification notes are recorded in docs/STATIONS.md. */
public final class StationData {
    private StationData() {}

    // Preserve the original three stations and their ordering for existing listeners.
    public static final List<Station> ALL = Collections.unmodifiableList(Arrays.asList(
        triton("YES 933", "93.3 FM • Mandarin", "YES933_PREM", MANDARIN),
        triton("LOVE 972", "97.2 FM • Mandarin", "LOVE972FM_PREM", MANDARIN),
        triton("CAPITAL 958", "95.8 FM • Mandarin", "CAPITAL958FM_PREM", MANDARIN),
        triton("96.3好FM", "96.3 FM • Mandarin", "HAO_963AAC", MANDARIN),
        triton("UFM100.3", "100.3 FM • Mandarin", "UFM_1003AAC", MANDARIN),
        triton("MONEY FM 89.3", "89.3 FM • English", "MONEY_893AAC", ENGLISH),
        triton("GOLD 905", "90.5 FM • English", "GOLD905_PREM", ENGLISH),
        triton("ONE FM 91.3", "91.3 FM • English", "ONE_FM_913AAC", ENGLISH),
        triton("Kiss92 FM", "92.0 FM • English", "KISS_92AAC", ENGLISH),
        triton("Symphony 924", "92.4 FM • Classical / English", "SYMPHONY924_PREM", ENGLISH),
        triton("CNA938", "93.8 FM • English", "938NOW_PREM", ENGLISH),
        triton("CLASS 95", "95.0 FM • English", "CLASS95_PREM", ENGLISH),
        triton("987", "98.7 FM • English", "987FM_PREM", ENGLISH),
        triton("RIA 897", "89.7 FM • Malay", "RIA897FM_PREM", MALAY),
        triton("WARNA 942", "94.2 FM • Malay", "WARNA942FM_PREM", MALAY),
        triton("OLI 968", "96.8 FM • Tamil", "OLI968FM_PREM", TAMIL),
        new Station("BBC World Service", "88.9 FM • English / World feed",
            "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", ENGLISH),
        triton("indiego", "Online • Indie / English", "INDIEGO_S01_PREM", ENGLISH),
        // Kakee stations are online streams; the former FM frequencies are not current.
        triton("88.3JIA", "Online • Mandarin / English • Singapore only", "883JIAAAC", MANDARIN, ENGLISH),
        triton("POWER 98", "Online • English • Singapore only", "POWER98_LOVESONGSAAC", ENGLISH),
        triton("88.3JIA Trending Hits", "Online • Mandarin / English • Singapore only", "JIA_WEBHITS_S01AAC", MANDARIN, ENGLISH),
        triton("88.3JIA Cantopop", "Online • Cantonese • Singapore only", "HARRYS_S02AAC", CANTONESE),
        triton("88.3JIA K-Pop", "Online • Korean • Singapore only", "JIA_KPOP_S01AAC", KOREAN),
        triton("POWER 98 Mixtape", "Online • English • Singapore only", "POWER_98_HITS_S01AAC", ENGLISH),
        triton("POWER 98 EDM Club Hits", "Online • Dance / English • Singapore only", "POWER_98_RAW_S01AAC", ENGLISH),
        triton("POWER 98 EMERGENC-E", "Online • Indie / English • Singapore only", "SMOOTHJAZZ_S01AAC", ENGLISH)
    ));

    /** A null language selects the entire catalog, in its original order. */
    public static List<Station> forLanguage(Station.Language language) {
        if (language == null) return ALL;
        List<Station> matches = new ArrayList<>();
        for (Station station : ALL) {
            if (station.languages.contains(language)) matches.add(station);
        }
        return Collections.unmodifiableList(matches);
    }

    private static Station triton(String name, String subtitle, String mount,
                                  Station.Language firstLanguage, Station.Language... otherLanguages) {
        return new Station(name, subtitle,
            "https://playerservices.streamtheworld.com/api/livestream-redirect/" + mount + ".aac",
            firstLanguage, otherLanguages);
    }
}
