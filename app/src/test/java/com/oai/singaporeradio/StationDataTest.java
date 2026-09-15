package com.oai.singaporeradio;

import org.junit.Test;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;
import static com.oai.singaporeradio.Station.Language.*;

public class StationDataTest {
    @Test public void catalogHasUniqueSecurePlayableEntriesAndKeepsFamilyStationsFirst() {
        assertEquals(26, StationData.ALL.size());
        assertEquals("YES 933", StationData.ALL.get(0).name);
        assertEquals("LOVE 972", StationData.ALL.get(1).name);
        assertEquals("CAPITAL 958", StationData.ALL.get(2).name);
        Set<String> names = new HashSet<>();
        Set<String> urls = new HashSet<>();
        for (Station station : StationData.ALL) {
            assertTrue(station.name, names.add(station.name));
            assertTrue(station.name, urls.add(station.url));
            assertEquals("https", URI.create(station.url).getScheme());
            assertNotNull(URI.create(station.url).getHost());
            assertFalse(station.subtitle.isEmpty());
            assertFalse(station.languages.isEmpty());
        }
    }

    @Test public void bilingualChannelsAppearInBothLanguageFilters() {
        Station jia = StationData.ALL.stream().filter(s -> s.name.equals("88.3JIA")).findFirst().get();
        assertTrue(StationData.forLanguage(MANDARIN).contains(jia));
        assertTrue(StationData.forLanguage(ENGLISH).contains(jia));
        assertFalse(StationData.forLanguage(MALAY).contains(jia));
        assertEquals(2, StationData.forLanguage(MALAY).size());
        assertEquals("OLI 968", StationData.forLanguage(TAMIL).get(0).name);
        assertEquals("88.3JIA Cantopop", StationData.forLanguage(CANTONESE).get(0).name);
        assertEquals("88.3JIA K-Pop", StationData.forLanguage(KOREAN).get(0).name);
        assertSame(StationData.ALL, StationData.forLanguage(null));
    }

    @Test public void digitalChannelsDoNotClaimFormerFmFrequencies() {
        long fmCount = StationData.ALL.stream().filter(s -> s.subtitle.contains(" FM • ")).count();
        assertEquals(17, fmCount);
        for (Station station : StationData.ALL) {
            if (station.name.startsWith("POWER 98") || station.name.startsWith("88.3JIA") || station.name.equals("indiego")) {
                assertTrue(station.name, station.subtitle.startsWith("Online • "));
            }
        }
    }

    @Test public void sharedCatalogCannotBeModifiedByCallers() {
        assertThrows(UnsupportedOperationException.class, () -> StationData.ALL.set(0, StationData.ALL.get(1)));
        assertThrows(UnsupportedOperationException.class, () -> StationData.forLanguage(MANDARIN).clear());
        assertThrows(UnsupportedOperationException.class, () -> StationData.ALL.get(0).languages.clear());
    }
}
