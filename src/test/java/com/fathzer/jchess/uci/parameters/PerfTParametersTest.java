package com.fathzer.jchess.uci.parameters;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerfTParametersTest {

    @Test
    void testParseAllOptions() {
        // Example: depth 7 threads 4 legal playleaves
        Deque<String> tokens = new LinkedList<>(Arrays.asList("7 threads 4 legal playleaves".split(" ")));
        PerfTParameters params = new PerfTParameters();
        List<String> ignored = PerfTParameters.PARSER.parse(params, tokens);

        assertEquals(7, params.getDepth());
        assertEquals(4, params.getParallelism());
        assertTrue(params.isLegal());
        assertTrue(params.isPlayLeaves());
        assertTrue(ignored.isEmpty());
    }

    @Test
    void testParseWithAliases() {
        // Example: depth 5 t 2 l pl
        Deque<String> tokens = new LinkedList<>(Arrays.asList("5 t 2 l pl".split(" ")));
        PerfTParameters params = new PerfTParameters();
        List<String> ignored = PerfTParameters.PARSER.parse(params, tokens);

        assertEquals(5, params.getDepth());
        assertEquals(2, params.getParallelism());
        assertTrue(params.isLegal());
        assertTrue(params.isPlayLeaves());
        assertTrue(ignored.isEmpty());
    }

    @Test
    void testDefaultValues() {
        // Only depth is provided
        Deque<String> tokens = new LinkedList<>(Arrays.asList("3"));
        PerfTParameters params = new PerfTParameters();
        List<String> ignored = PerfTParameters.PARSER.parse(params, tokens);

        assertEquals(3, params.getDepth());
        assertEquals(1, params.getParallelism());
        assertFalse(params.isLegal());
        assertFalse(params.isPlayLeaves());
        assertTrue(ignored.isEmpty());
    }

    @Test
    void testIgnoredTokens() {
        // Unknown option "foo" should be ignored
        Deque<String> tokens = new LinkedList<>(Arrays.asList("2", "foo", "bar", "threads", "3"));
        PerfTParameters params = new PerfTParameters();
        List<String> ignored = PerfTParameters.PARSER.parse(params, tokens);

        assertEquals(2, params.getDepth());
        assertEquals(3, params.getParallelism());
        assertTrue(ignored.contains("foo"));
        assertTrue(ignored.contains("bar"));
    }

    @Test
    void testIllegalDepth() {
        // Depth is missing
        Deque<String> tokens = new LinkedList<>(Arrays.asList());
        PerfTParameters params = new PerfTParameters();
        assertThrows(IllegalArgumentException.class, () -> PerfTParameters.PARSER.parse(params, tokens));
    }

    @Test
    void testIllegalDepthNegative() {
        // Negative depth should throw
        Deque<String> tokens = new LinkedList<>(Arrays.asList("-1"));
        PerfTParameters params = new PerfTParameters();
        assertThrows(IllegalArgumentException.class, () -> PerfTParameters.PARSER.parse(params, tokens));
    }
}