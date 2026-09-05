package com.oai.singaporeradio;

import java.util.Arrays;
import java.util.List;

public final class StationData {
    private StationData() {}
    public static final List<Station> ALL = Arrays.asList(
        new Station("YES 933", "93.3 FM • Mandarin", "https://playerservices.streamtheworld.com/api/livestream-redirect/YES933_PREM.aac"),
        new Station("LOVE 972", "97.2 FM • Mandarin", "https://playerservices.streamtheworld.com/api/livestream-redirect/LOVE972FM_PREM.aac"),
        new Station("CAPITAL 958", "95.8 FM • Mandarin", "https://playerservices.streamtheworld.com/api/livestream-redirect/CAPITAL958FM_PREM.aac")
    );
}
