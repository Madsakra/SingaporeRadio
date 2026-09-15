# Third-party notices

## Radio stations and logos

The 26 station and channel logos in `app/src/main/res/drawable-nodpi/` come from the broadcasters' public assets. The original files are bundled locally and scaled to fit each station tile without cropping or network requests. Sources were checked on 15 September 2026.

| Station | Bundled file | Official page | Logo source |
| --- | --- | --- | --- |
| YES 933 | `station_yes_933.png` | [Station page](https://data.melisten.sg/radio/yes-933) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/yes-933.png) |
| LOVE 972 | `station_love_972.png` | [Station page](https://data.melisten.sg/radio/love-972) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/love-972.png) |
| CAPITAL 958 | `station_capital_958.png` | [Station page](https://data.melisten.sg/radio/capital-958) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/capital-958.png) |
| 96.3好FM | `station_hao_963.png` | [Station page](https://audio.sph.com.sg/home/) | [Original artwork](https://static.awedio.sg/default/17283793075d2b55.png) |
| UFM100.3 | `station_ufm_1003.png` | [Station page](https://audio.sph.com.sg/home/) | [Original artwork](https://static.awedio.sg/default/c2c49348420d162f40a43b754acc6cd1) |
| MONEY FM 89.3 | `station_money_893.png` | [Station page](https://audio.sph.com.sg/home/) | [Original artwork](https://static.awedio.sg/default/f7f3a2c39c6d1062dbb8d203fb86267b) |
| GOLD 905 | `station_gold_905.png` | [Station page](https://www.melisten.sg/radio/gold-905) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/gold-905.png) |
| ONE FM 91.3 | `station_one_913.png` | [Station page](https://audio.sph.com.sg/home/) | [Original artwork](https://static.awedio.sg/default/4ad8b255ee1faeae74904239b23925a5) |
| Kiss92 FM | `station_kiss_92.png` | [Station page](https://audio.sph.com.sg/home/) | [Original artwork](https://static.awedio.sg/default/1729004029af2c8f.png) |
| Symphony 924 | `station_symphony_924.png` | [Station page](https://www.melisten.sg/radio/symphony-924) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/symphony-924.png) |
| CNA938 | `station_cna_938.png` | [Station page](https://www.melisten.sg/radio/cna938) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/cna-938.png) |
| CLASS 95 | `station_class_95.png` | [Station page](https://www.melisten.sg/radio/class-95) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/class-95.png) |
| 987 | `station_987.png` | [Station page](https://www.melisten.sg/radio/987) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/987.png) |
| RIA 897 | `station_ria_897.png` | [Station page](https://www.melisten.sg/radio/ria-897) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/ria-897.png) |
| WARNA 942 | `station_warna_942.png` | [Station page](https://www.melisten.sg/radio/warna-942) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/warna-942.png) |
| OLI 968 | `station_oli_968.png` | [Station page](https://www.melisten.sg/radio/oli-968) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/oli-968.png) |
| BBC World Service | `station_bbc_world_service.png` | [Station page](https://www.bbc.co.uk/sounds/play/live/bbc_world_service) | [Original artwork](https://sounds.files.bbci.co.uk/3.12.0/networks/bbc_world_service/blocks-colour_600x600.png) |
| indiego | `station_indiego.png` | [Station page](https://www.melisten.sg/radio/indiego) | [Original artwork](https://www16.mediacorp.sg/melisten/radiostation-logos/indiego.png) |
| 88.3JIA | `station_jia_883.png` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile_883jia.png) |
| POWER 98 | `station_power_98.png` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile_power98.png) |
| 88.3JIA Trending Hits | `station_jia_trending.jpg` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile-883th.jpg) |
| 88.3JIA Cantopop | `station_jia_cantopop.jpg` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile-883cantopop.jpg) |
| 88.3JIA K-Pop | `station_jia_kpop.png` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile_883jia_kpop.png) |
| POWER 98 Mixtape | `station_power_98_mixtape.jpg` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile_power98_mixtape.jpg) |
| POWER 98 EDM Club Hits | `station_power_98_edm.jpg` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile_power98_edm.jpg) |
| POWER 98 EMERGENC-E | `station_power_98_emergenc_e.jpg` | [Station page](https://www.kakee.sg/) | [Original artwork](https://www.kakee.sg/images/camokakislibraries/kakee-reskin/tile_power98_emergenc-e.jpg) |

The original vector radio icon (`ic_radio.xml`) remains as a fallback for future catalog entries without artwork. Every current station has its own logo.

The app connects directly to the StreamTheWorld and BBC HTTPS endpoints listed in `StationData.java`. The catalog includes Mediacorp, SPH Media, BBC World Service and So Drama! Entertainment/Kakee. Source pages and verification results are listed in [docs/STATIONS.md](docs/STATIONS.md). No broadcast audio is included in this repository. Names, trademarks, logos, and broadcast content remain the property of their respective owners. Attribution is not a grant of permission; public redistribution rights have not been verified. This project is not affiliated with or endorsed by the broadcasters.

## Build and playback dependencies

- AndroidX Media3 ExoPlayer: https://github.com/androidx/media (Apache License 2.0)
- AndroidX Core: https://android.googlesource.com/platform/frameworks/support/ (Apache License 2.0)
- Robolectric and JUnit (test-only): https://github.com/robolectric/robolectric (MIT) and https://github.com/junit-team/junit4 (EPL 1.0)
- Gradle wrapper: https://github.com/gradle/gradle (Apache License 2.0; license notice in wrapper scripts)

Each dependency and its transitive dependencies remain subject to their own license terms and notices.
