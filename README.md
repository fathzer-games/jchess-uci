[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jchess-uci)](https://central.sonatype.com/artifact/com.fathzer/jchess-uci)
[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/fathzer-games/jchess-uci/blob/master/LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fathzer-games_jchess-uci&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer-games_jchess-uci)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jchess-uci/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jchess-uci)

# jchess-uci
A partial (but yet usable) java implementation of the [chess UCI protocol](https://www.shredderchess.com/chess-features/uci-universal-chess-interface.html) with pluggable chess engines.

## Get rid of System.out and System.in
UCI protocol uses standard input and output console to communicate which is not really modern...
If you want another way to exchange messages, you can subclass the UCI class and override the *getNextCommand* and/or the *out* methods.


## TODO
* Verify the engine is protected against strange client behavior (like changing the position during a go request).
* Implement a plugin loader? Ask myself if uci.bat/uci.sh are in their right places (UCI has no main anymore).
