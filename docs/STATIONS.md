# Station catalog and source verification

Checked 15 September 2026. Coverage is the 17 Singapore FM services (including
BBC World Service), Mediacorp's indiego and the eight music channels currently
listed on Kakee. This is a broadcaster catalog, not an exhaustive directory of
independent internet stations or neighbouring countries' FM signals.

## Sources

- [Mediacorp's audio brands](https://www.mediacorp.sg/corporate/brands/audio).
  Stream mounts come from the [official meLISTEN station catalog](https://www.melisten.sg/api/mcapi/stations-176261?station_type=live), `streamURLHigh`.
- [SPH Media radio brands](https://www.sph.com.sg/our-brands/radio/) and
  [SPH Audio](https://audio.sph.com.sg/home/). Its public player supplies
  `triton_mount_name`; AAC variants are confirmed by the StreamTheWorld player
  provisioning endpoint, for example
  `https://playerservices.streamtheworld.com/api/livestream?version=1.9&station=MONEY_893&lang=en`.
- [Kakee's channels and regional FAQ](https://www.kakee.sg/). Its public player
  supplies `data-mount-name`; AAC variants are confirmed by the same provisioning
  API. All Kakee music streams are limited to Singapore according to that FAQ.
  POWER 98 and 88.3JIA are labelled Online, not with former FM frequencies.
- [BBC's listening guide](https://downloads.bbc.co.uk/worldservice/schedules/eastasia_audienceguidetolistening.pdf)
  identifies the Singapore 88.9 FM relay. The app uses the broadcaster-hosted
  `stream.live.vc.bbcmedia.co.uk/bbc_world_service` global English stream. Its
  schedule may differ from the East Asia FM relay; the UI labels it World feed.

## Network checks

GET requests followed broadcaster HTTPS redirects, used `SingaporeRadio/1.3`,
and read at most 8 KiB per stream. **22/26 returned HTTP 200 with AAC or MP3 audio;
4/26 returned HTTP 403.** The four 403 responses have no body, and their exact
cause is not established. Both their AAC and MP3 variants were refused. The
broadcaster's regional restriction is documented; do not treat those four feeds
as verified playable. No geoblock workaround, proxy or stream relay is used.

These checks establish connectivity and response type, not decoded audio,
station identity, continuous playback, public-distribution permission or future
availability. Test the four refused feeds on a Singapore Android device before
publishing. Requests use the stable redirect URLs below, never a pinned edge
server or temporary token.

| Station | Display details | Stream | Check |
| --- | --- | --- | --- |
| YES 933 | 93.3 FM • Mandarin | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/YES933_PREM.aac) | 200 — audio/aacp |
| LOVE 972 | 97.2 FM • Mandarin | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/LOVE972FM_PREM.aac) | 200 — audio/aacp |
| CAPITAL 958 | 95.8 FM • Mandarin | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/CAPITAL958FM_PREM.aac) | 200 — audio/aacp |
| 96.3好FM | 96.3 FM • Mandarin | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/HAO_963AAC.aac) | 200 — audio/aacp |
| UFM100.3 | 100.3 FM • Mandarin | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/UFM_1003AAC.aac) | 200 — audio/aacp |
| MONEY FM 89.3 | 89.3 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/MONEY_893AAC.aac) | 200 — audio/aacp |
| GOLD 905 | 90.5 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/GOLD905_PREM.aac) | 200 — audio/aacp |
| ONE FM 91.3 | 91.3 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/ONE_FM_913AAC.aac) | 200 — audio/aacp |
| Kiss92 FM | 92.0 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/KISS_92AAC.aac) | 200 — audio/aacp |
| Symphony 924 | 92.4 FM • Classical / English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/SYMPHONY924_PREM.aac) | 200 — audio/aacp |
| CNA938 | 93.8 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/938NOW_PREM.aac) | 200 — audio/aacp |
| CLASS 95 | 95.0 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/CLASS95_PREM.aac) | 200 — audio/aacp |
| 987 | 98.7 FM • English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/987FM_PREM.aac) | 200 — audio/aacp |
| RIA 897 | 89.7 FM • Malay | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/RIA897FM_PREM.aac) | 200 — audio/aacp |
| WARNA 942 | 94.2 FM • Malay | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/WARNA942FM_PREM.aac) | 200 — audio/aacp |
| OLI 968 | 96.8 FM • Tamil | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/OLI968FM_PREM.aac) | 200 — audio/aacp |
| BBC World Service | 88.9 FM • English / World feed | [Stream](https://stream.live.vc.bbcmedia.co.uk/bbc_world_service) | 200 — audio/mpeg |
| indiego | Online • Indie / English | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/INDIEGO_S01_PREM.aac) | 200 — audio/aacp |
| 88.3JIA | Online • Mandarin / English • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/883JIAAAC.aac) | 200 — audio/aacp |
| POWER 98 | Online • English • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/POWER98_LOVESONGSAAC.aac) | 200 — audio/aacp |
| 88.3JIA Trending Hits | Online • Mandarin / English • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/JIA_WEBHITS_S01AAC.aac) | 403 — playback unverified |
| 88.3JIA Cantopop | Online • Cantonese • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/HARRYS_S02AAC.aac) | 200 — audio/aacp |
| 88.3JIA K-Pop | Online • Korean • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/JIA_KPOP_S01AAC.aac) | 403 — playback unverified |
| POWER 98 Mixtape | Online • English • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/POWER_98_HITS_S01AAC.aac) | 403 — playback unverified |
| POWER 98 EDM Club Hits | Online • Dance / English • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/POWER_98_RAW_S01AAC.aac) | 403 — playback unverified |
| POWER 98 EMERGENC-E | Online • Indie / English • Singapore only | [Stream](https://playerservices.streamtheworld.com/api/livestream-redirect/SMOOTHJAZZ_S01AAC.aac) | 200 — audio/aacp |

## Maintenance

Edit `StationData.ALL` to add or update a station. The UI renders and filters
that catalog automatically. Preserve the first three stations for existing
listeners. Supply all relevant language tags for bilingual channels, use a full
display name and label online-only channels honestly. The catalog and language
sets are immutable. Run unit tests and lint, then probe changed feeds and test
them on a device; endpoints can change independently of an app release.
