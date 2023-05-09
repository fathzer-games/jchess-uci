[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jchess-uci)](https://central.sonatype.com/artifact/com.fathzer/jchess-uci)
[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/fathzer-games/jchess-uci/blob/master/LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fathzer-games_jchess-uci&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer-games_jchess-uci)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jchess-uci/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jchess-uci)

# jchess-uci
A java implementation of the chess UCI protocol with pluggable chess engines.

## TODO
* Implement uci.doPerfStat with no dependency to JChess
* Think about where Perft data set should be stored (An extra artifact?)
* Allow UCI to get its input/output elsewhere from System.in/out
* Implement a plugin loader? Ask myself if uci.bat/uci.sh are in their right places (UCI has no main anymore).
