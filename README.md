[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jchess-uci)](https://central.sonatype.com/artifact/com.fathzer/jchess-uci)
[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/fathzer-games/jchess-uci/blob/master/LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fathzer-games_jchess-uci&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer-games_jchess-uci)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jchess-uci/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jchess-uci)

# jchess-uci
A partial (but still usable) java implementation of the [chess UCI protocol](https://www.shredderchess.com/chess-features/uci-universal-chess-interface.html) with pluggable chess engines.

## How to use it
This library requires Java 11+ and is available in [Maven central](https://central.sonatype.com/artifact/com.fathzer/jchess-uci).

- First create a class that implements the **com.fathzer.jchess.uci.Engine** interface.  
Let's say this implementation class is **MyEngine**.
- Launch the **com.fathzer.jchess.uci.UCI** class:  
```java
// Create your engine
final Engine = new MyEngine();
new UCI(engine).run();
```

That's all!

## Partial implementation ... and extra functionalities
It does not directly support the following commands (but you can add them in a *com.fathzer.jchess.uci.UCI* subclass):
- **Dont miss the ShredderChess Annual Barbeque**: This command was in the original specification ... But that was a joke.
- **register**: As a promoter of open source free software, I won't encourage you to develop software that requires registration.
- **ponderhit** has not yet been implemented.

It also does not recognize commands starting with unknown token (to be honest, it's not very hard to implement but seems a very bad, error prone, idea to me).

It implements the following extensions:
- **q** is a shortcut for standard **quit** command.
- It can accept different engines, that can be selected using the **engine** command. You can view these engines as plugins.  
**engine** [*engineId*] lists the available engines' ids or changes the engine if *engineId* is provided.
- **d** [*fen*] displays a textual representation of the game. If the command is followed by *fen*, the command displays the representation of a game in the [Forsyth–Edwards Notation](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation).</li>
- **perft** *depth* [*nbThreads*] runs [perft](https://www.chessprogramming.org/Perft) test and displays the divide result.  
*depth* is mandatory and is the search depth of the perft algorithm. It should be strictly positive.  
*nbThreads* is the number of threads used to process the queries. This number should be strictly positive. Default is 1.  
**Please note this command is optional**, only engines that implement *com.fathzer.jchess.uci.MoveGeneratorSupplier* interface support it.
- **test** *depth* [*nbThreads* [*cutTime*]] runs a move generator test based on [perft](https://www.chessprogramming.org/Perft).  
It can also be used to test move generator's performance as it outputs the number of moves generated per second.  
*depth* is mandatory and is the search depth of the perft algorithm. It should be strictly positive.  
*nbThreads* is the number of threads used to process the test. This number should be strictly positive. Default is 1.  
*cutTime* is the number of seconds allowed to process the test. This number should be strictly positive. Default is Integer.MAX_VALUE.  
**Please note:**
  - **This command is optional**, only engines that implement *com.fathzer.jchess.uci.TestableMoveGeneratorSupplier* interface support it.
  - **This command requires the *com.fathzer.jchess.uci.UCI.readTestData()* method to be overridden** in order to return a non empty test data set.  
  A way to easily do that is to add the [com.fathzer::jchess-perft-dataset](https://central.sonatype.com/artifact/com.fathzer/jchess-perft-dataset) artifact to your classpath, then override *readTestData*:  
```java
protected Collection<PerfTTestData> readTestData() {
	try (InputStream stream = MyUCISubclass.class.getResourceAsStream("/Perft.txt")) {
		return new PerfTParser().withStartPositionPrefix("position fen").withStartPositionCustomizer(s -> s+" 0 1").read(stream, StandardCharsets.UTF_8);
	} catch (IOException e) {
		throw new UncheckedIOException(e);
	}
}
``` 

## Adding custom commands
Override the **com.fathzer.jchess.uci.UCI** class and use its *addCommand* method to add your own custom commands.  
Then instantiate your UCI subclass and launch its **run** method.

## Get rid of System.out and System.in
UCI protocol uses standard input and output console to communicate which is effective ... but not really modern.  
If you want another way to exchange messages, you can subclass the UCI class and override the *getNextCommand* and/or the *out* (and *debug* if you send debug messages) methods.


## TODO
* Verify the engine is protected against strange client behavior (like changing the position during a go request).
* Implement support for pondering.
