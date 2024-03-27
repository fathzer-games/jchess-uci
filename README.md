[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jchess-uci)](https://central.sonatype.com/artifact/com.fathzer/jchess-uci)
[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/fathzer-games/jchess-uci/blob/master/LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fathzer-games_jchess-uci&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer-games_jchess-uci)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jchess-uci/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jchess-uci)

# jchess-uci
A partial (but yet usable) java implementation of the [chess UCI protocol](https://www.shredderchess.com/chess-features/uci-universal-chess-interface.html) with pluggable chess engines.

## How to use it
This library requires Java 17+ and is available in [Maven central](https://central.sonatype.com/artifact/com.fathzer/jchess-uci).

- First create a class that implements the **com.fathzer.jchess.uci.Engine** interface.  
Let say this implementation class is **MyEngine**.
- Launch the **com.fathzer.jchess.uci.UCI** class:  
```java
// Create your engine
final Engine = new MyEngine();
// Launch UCI interface
try (UCI uci = new UCI(engine)) {
  uci.run();
}
```

If you plan to use the *perft* and *test* commands described below, launch the **ExtendedUCI** class instead of **UCI**.

That's all!

## Partial implementation ... and extra functionalities
It does not directly support the following commands (but you can add them in an *com.fathzer.jchess.uci.UCI* subclass):
- **Dont miss the ShredderChess Annual Barbeque**: This command was in the original specification ... But was a joke.
- **register**: As a promoter of open source free sofware, I will not encourage you to develop software that requires registration.
- **ponderhit** is not yet implemented.
- Only depth, score and pv are implemented in info lines preceeding go reply.

It also does not recognize commands starting with unknown token (to be honest, it's not very hard to implement but seemed a very bad, error prone, idea to me).

The following extensions are implemented in **UCI** class:
- It can accept different engines, that can be selected using the **engine** command. You can view these engines as plugins.  
**engine** [*engineId*] Lists the available engines id or change the engine if *engineId* is provided.
- **q** is a shortcut for standard **quit** command

The **ExtendedUCI** class implements the following extensions in addition of the standard commands:
- **block** Waits until the current background task (for instance a go command) is ended before reading next command on input.  
**Warning:** Be cautious with this command, once the input reading is blocked there's no way to stop the current background task. Typically, the *stop* command will no work.
- **d** [*fen*] displays a textual representation of the game. If the command is followed by *fen*, the command displays the representation of a game in the [Forsyth–Edwards Notation](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation).  
**Please note this command is optional**, only engines that implement *com.fathzer.jchess.uci.extended.Displayable* interface support it.
- **perft** *depth* [threads *nb*] [legal] [playleaves] runs [perft](https://www.chessprogramming.org/Perft) test and displays the divide result.  
*depth* is mandatory and is the search depth of the perft algorithm. It should be strictly positive.  
*threads* is followed by the number of threads used to process the queries. This number should be strictly positive. Default is 1. *threads* can be replaced by the shortcut *t*.  
*legal* uses legal moves from the move generator instead of the default pseudo-legal moves. *legal* can be replaced by the shortcut *l*.  
*playleaves* plays, when used with *legal*, the leave moves. These moves are always played when using pseudo-legal moves. *playleaves* can be replaced by the shortcut *pl*.     
**Please note this command is optional**, only engines that implement *com.fathzer.jchess.uci.extended.MoveGeneratorSupplier* interface support it.  
If the engine implements the *com.fathzer.jchess.uci.extended.MoveToUCIConverter* interface, the divide displays moves in UCI format, otherwise the move's toString method is used.
- **test** *depth* [threads *nb*] [legal] [playleaves] [cut *s*] runs a move generator test based on [perft](https://www.chessprogramming.org/Perft).  
It also can be used to test move generator's performance as it outputs the number of moves generated per second.  
*depth* is mandatory and is the search depth of perft algorithm. It should be strictly positive.  
*threads* is followed by the number of threads used to process the test. This number should be strictly positive. Default is 1. *threads* can be replaced by the shortcut *t*.  
*legal* uses legal moves from the move generator instead of the default pseudo-legal moves. *legal* can be replaced by the shortcut *l*.  
*playleaves* plays, when used with *legal*, the leave moves. These moves are always played when using pseudo-legal moves. *playleaves* can be replaced by the shortcut *pl*.  
*cut* is followed by the number of seconds allowed to process the test. This number should be strictly positive. Default is Integer.MAX_VALUE.  
**Please note:**
  - **This command is optional**, only engines that implement *com.fathzer.games.perft.TestableMoveGeneratorBuilder* interface support it.
  - **This command requires the *com.fathzer.jchess.uci.extended.ExtendedUCI.readTestData()* method to be overridden** in order to return a non empty test data set.  
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
 
## Initializing the engine with the command line.
At program startup (when ```uci.run()``` is called), if the *uciInitCommands* system property is set, it is used as the path of a file that contains an uci command per line.  
Every commands are executed once, before starting to listen to the standard input for commands issued by the GUI. 

## Adding custom commands
Override the **com.fathzer.jchess.uci.UCI** or **com.fathzer.jchess.uci.extended.ExtendedUCI** classes and use its *addCommand* method to add your own custom commands.  
Then instantiate your UCI subclass and launch its **run** method.

## Get rid of System.out and System.in
UCI protocol uses standard input and output console to communicate which is effective ... but not really modern.  
If you want another way to exchange messages, you can subclass the UCI class and override the *getNextCommand* and/or the *out* (and *debug* if you send debug messages) methods.

## Shrinking (a little) your artifacts
If you do not use the *com.fathzer.jchess.uci.extended* and *com.fathzer.jchess.uci.helper* packages, you can exclude the *com.fathzer:games-core* dependency.

## Known bugs

## TODO
* Verify the engine is protected against strange client behavior (like changing the position during a go request).
* Implement support for multi-PV search.
* Implement support for pondering.
