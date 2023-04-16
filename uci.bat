SET PATH=C:\Program files\Java\jdk-11.0.5\bin
echo %PATH%
rem java -DdebugUCI=false -cp target/classes com.fathzer.jchess.uci.UCI
java -DdebugUCI=false -cp target/JChess-0.0.1-SNAPSHOT.jar com.fathzer.jchess.uci.UCI

pause