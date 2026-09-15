package com.oai.singaporeradio;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class Station {
    public enum Language { MANDARIN, ENGLISH, MALAY, TAMIL, CANTONESE, KOREAN }

    public final String name;
    public final String subtitle;
    public final String url;
    public final Set<Language> languages;

    public Station(String name, String subtitle, String url, Language firstLanguage, Language... otherLanguages) {
        this.name = name;
        this.subtitle = subtitle;
        this.url = url;
        this.languages = Collections.unmodifiableSet(EnumSet.of(firstLanguage, otherLanguages));
    }
}
