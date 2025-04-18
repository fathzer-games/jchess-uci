package com.fathzer.jchess.uci;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.fathzer.jchess.uci.parameters.ParameterDefinition;
import com.fathzer.jchess.uci.parameters.Parser;

import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class GoReplyTest {
	
    private static class ParsedInfoReply {
        int depth;
        List<String> score;
        int hashfull;
        int multipv;
        List<String> pv;

        private static class InfoReplyParser extends Parser<ParsedInfoReply> {
            InfoReplyParser() {
                super(Arrays.asList(
                    new ParameterDefinition<>(
                        (info, args) -> info.depth = positiveInt(args), "depth"
                    ),
                    new ParameterDefinition<>(
                        (info, args) -> {
                        	info.score = new ArrayList<>();
                            while (!args.isEmpty()) {
                            	info.score.add(args.pop());
                            }
                        }, "score"
                    ),
                    new ParameterDefinition<>(
                        (info, args) -> info.hashfull = positiveInt(args), "hashfull"
                    ),
                    new ParameterDefinition<>(
                        (info, args) -> info.multipv = positiveInt(args), "multipv"
                    ),
                    new ParameterDefinition<>(
                        (info, args) -> {
                        	info.pv = new ArrayList<>();
                            while (!args.isEmpty()) {
                            	info.pv.add(args.pop());
                            }
                        }, "pv"
                    )
                ));
            }
        }
        
        private static ParsedInfoReply from(String str) {
        	final Deque<String> tokens = new LinkedList<>(Arrays.asList(str.split(" ")));
        	tokens.removeFirst();
        	final ParsedInfoReply result = new ParsedInfoReply();
        	final List<String> ignored = new InfoReplyParser().parse(result, tokens);
        	assertTrue(ignored.isEmpty(),"There was some unexpected tokens ("+ignored+") in the following move info string "+str);
			return result;
        }

		@Override
		public int hashCode() {
			return Objects.hash(depth, hashfull, multipv, pv, score);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ParsedInfoReply other = (ParsedInfoReply) obj;
			return depth == other.depth && hashfull == other.hashfull && multipv == other.multipv
					&& Objects.equals(pv, other.pv) && Objects.equals(score, other.score);
		}
    }
    
    void testInfoString(String expected, String current) {
    	assertNotNull(current);
    	assertEquals(ParsedInfoReply.from(expected), ParsedInfoReply.from(current));
    }

    private static UCIMove move1;
    private static UCIMove move2;
    private static UCIMove movePromotion;

    @BeforeAll
    static void setUp() {
        move1 = new UCIMove("e2", "e4");
        move2 = new UCIMove("e7", "e5");
        movePromotion = new UCIMove("e7", "e8", "q");
    }

    @Test
    void testCpScore() {
        GoReply.CpScore score = new GoReply.CpScore(42);
        assertEquals(42, score.cp());
        assertEquals("cp 42", score.toUCI());
    }

    @Test
    void testLowerScore() {
        GoReply.LowerScore score = new GoReply.LowerScore(10);
        assertEquals(10, score.cp());
        assertEquals("lowerbound 10", score.toUCI());
    }

    @Test
    void testUpperScore() {
        GoReply.UpperScore score = new GoReply.UpperScore(-5);
        assertEquals(-5, score.cp());
        assertEquals("upperbound -5", score.toUCI());
    }

    @Test
    void testMateScore() {
        GoReply.MateScore score = new GoReply.MateScore(3);
        assertEquals(3, score.moveNumber());
        assertEquals("mate 3", score.toUCI());
    }

    @Test
    void testGoReplyConstructorsAndGetters() {
        GoReply reply = new GoReply(move1, move2);
        assertEquals(Optional.of(move1), reply.getMove());
        assertEquals(Optional.of(move2), reply.getPonderMove());
        assertTrue(reply.getInfo().isEmpty());

        GoReply reply2 = new GoReply((UCIMove) null);
        assertEquals(Optional.empty(), reply2.getMove());
        assertEquals(Optional.empty(), reply2.getPonderMove());
        assertTrue(reply2.getInfo().isEmpty());
    }

    @Test
    void testSetInfoAndGetInfo() {
        GoReply reply = new GoReply(move1);
        GoReply.Info info = new GoReply.Info(12);
        reply.setInfo(info);
        assertTrue(reply.getInfo().isPresent());
        assertEquals(info, reply.getInfo().get());
    }

    @Test
    void testInfoFieldsAndMethods() {
        GoReply.Info info = new GoReply.Info(5);
        assertEquals(5, info.getDepth());
        assertEquals(-1, info.getHashFull());
        assertEquals(List.of(), info.getExtraMoves());

        info.setHashFull(999);
        assertEquals(999, info.getHashFull());

        List<UCIMove> extra = List.of(move1, move2);
        info.setExtraMoves(extra);
        assertEquals(extra, info.getExtraMoves());

        // pvBuilder and scoreBuilder
        info.setPvBuilder(m -> Optional.of(List.of(move2, move1)));

        info.setScoreBuilder(m -> Optional.of(new GoReply.CpScore(77)));
        
        GoReply reply = new GoReply(movePromotion);
        reply.setInfo(info);
        testInfoString("info depth 5 score cp 77 hashfull 999 multipv 2 pv e7e5 e2e4", reply.getInfoString(1).orElse(null));
        testInfoString("info depth 5 score cp 77 hashfull 999 multipv 3 pv e7e5 e2e4", reply.getInfoString(2).orElse(null));
        assertThrows(IllegalArgumentException.class, () -> reply.getInfoString(3));
    }
    
    @Test
    void testToString() {
        GoReply reply = new GoReply(move1, move2);
        assertEquals("bestmove e2e4 e7e5", reply.toString());

        GoReply reply2 = new GoReply(move1);
        assertEquals("bestmove e2e4", reply2.toString());

        GoReply reply3 = new GoReply((UCIMove) null, null);
        assertEquals("bestmove (none)", reply3.toString());
    }

    @Test
    void testGetMainInfoStringAndGetInfoString() {
        GoReply reply = new GoReply(move1);
        GoReply.Info info = new GoReply.Info(10);
        info.setHashFull(500);
        info.setScoreBuilder(m -> Optional.of(new GoReply.CpScore(30)));
        info.setPvBuilder(m -> Optional.of(List.of(move1, move2)));
        reply.setInfo(info);

        // Main info string
        Optional<String> mainInfo = reply.getMainInfoString();
        assertTrue(mainInfo.isPresent());
        String expected = "info depth 10 score cp 30 hashfull 500 multipv 1 pv e2e4 e7e5";
        testInfoString(expected, mainInfo.get());

        // Info string for index 0 (best move)
        Optional<String> infoString0 = reply.getInfoString(0);
        assertTrue(infoString0.isPresent());
        testInfoString(expected, infoString0.get());

        // Info string for index 1 (extra move)
        info.setExtraMoves(List.of(move2));
        Optional<String> infoString1 = reply.getInfoString(1);
        assertTrue(infoString1.isPresent());
        String expected1 = "info depth 10 score cp 30 hashfull 500 multipv 2 pv e2e4 e7e5";
        testInfoString(expected1, infoString1.get());
    }

    @Test
    void testGetMainInfoStringEmptyWhenNoMove() {
        GoReply reply = new GoReply((UCIMove) null);
        assertTrue(reply.getMainInfoString().isEmpty());
    }
}