package com.fathzer.jchess.uci;

import com.fathzer.jchess.uci.option.ButtonOption;
import com.fathzer.jchess.uci.option.CheckOption;
import com.fathzer.jchess.uci.option.SpinOption;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UsualOptionsTest {

    @Test
    void testChess960Option() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        CheckOption option = UsualOptions.chess960(triggered::set);

        assertEquals(UsualOptions.CHESS960_NAME, option.getName());
        assertFalse(option.getValue());

        option.setValue("true");
        assertTrue(triggered.get());
        assertTrue(option.getValue());
    }

    @Test
    void testHashOption() {
        AtomicInteger triggered = new AtomicInteger(-1);
        int defaultSize = 16, minSize = 1, maxSize = 512;
        SpinOption<Integer> option = UsualOptions.hash(triggered::set, defaultSize, minSize, maxSize);

        assertEquals(UsualOptions.HASH_NAME, option.getName());
        assertEquals(defaultSize, option.getValue());

        // Within bounds
        option.setValue("64");
        assertEquals(64, option.getValue());
        assertEquals(64, triggered.get());

        // Lower bound
        option.setValue(Integer.toString(minSize));
        assertEquals(minSize, option.getValue());

        // Upper bound
        option.setValue(Integer.toString(maxSize));
        assertEquals(maxSize, option.getValue());

        // Below min
		final String belowMin = Integer.toString(minSize - 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(belowMin));

        // Above max
		final String aboveMax = Integer.toString(maxSize + 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(aboveMax));
    }

    @Test
    void testClearHashOption() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        ButtonOption option = UsualOptions.clearHash(() -> triggered.set(true));

        assertEquals(UsualOptions.CLEAR_HASH_NAME, option.getName());
        assertFalse(triggered.get());
        option.setValue(null);
        assertTrue(triggered.get());
    }

    @Test
    void testEloOption() {
        AtomicInteger triggered = new AtomicInteger(-1);
        int defaultElo = 2000, minElo = 1350, maxElo = 2850;
        SpinOption<Integer> option = UsualOptions.elo(triggered::set, defaultElo, minElo, maxElo);

        assertEquals(UsualOptions.ELO_NAME, option.getName());
        assertEquals(defaultElo, option.getValue());

        // Within bounds
        option.setValue("2500");
        assertEquals(2500, option.getValue());
        assertEquals(2500, triggered.get());

        // Lower bound
        option.setValue(Integer.toString(minElo));
        assertEquals(minElo, option.getValue());

        // Upper bound
        option.setValue(Integer.toString(maxElo));
        assertEquals(maxElo, option.getValue());

        // Below min
		final String belowMin = Integer.toString(minElo - 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(belowMin));

        // Above max
		final String aboveMax = Integer.toString(maxElo + 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(aboveMax));
    }

    @Test
    void testLimitStrengthOption() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        CheckOption option = UsualOptions.limitStrength(triggered::set);

        assertEquals(UsualOptions.LIMIT_STRENGTH_NAME, option.getName());
        assertFalse(option.getValue());

        option.setValue("true");
        assertTrue(triggered.get());
        assertTrue(option.getValue());
    }

    @Test
    void testMultiPVOption() {
        AtomicInteger triggered = new AtomicInteger(-1);
        SpinOption<Integer> option = UsualOptions.multiPV(triggered::set);

        assertEquals(UsualOptions.MULTI_PV_NAME, option.getName());
        assertEquals(1, option.getValue());

        // Within bounds
        option.setValue("10");
        assertEquals(10, option.getValue());
        assertEquals(10, triggered.get());

        // Lower bound
        option.setValue("1");
        assertEquals(1, option.getValue());

        // Upper bound
        option.setValue("256");
        assertEquals(256, option.getValue());

        // Below min
        assertThrows(IllegalArgumentException.class, () -> option.setValue("0"));

        // Above max
        assertThrows(IllegalArgumentException.class, () -> option.setValue("257"));
    }

    @Test
    void testOwnBookOption() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        CheckOption option = UsualOptions.ownBook(triggered::set, true);

        assertEquals(UsualOptions.OWN_BOOK_NAME, option.getName());
        assertTrue(option.getValue());

        option.setValue("false");
        assertFalse(triggered.get());
        assertFalse(option.getValue());
    }

    @Test
    void testPonderOption() {
        AtomicBoolean triggered = new AtomicBoolean(true);
        CheckOption option = UsualOptions.ponder(triggered::set, false);

        assertEquals(UsualOptions.PONDER_NAME, option.getName());
        assertFalse(option.getValue());

        option.setValue("true");
        assertTrue(triggered.get());
        assertTrue(option.getValue());
    }

    @Test
    void testLevelOption() {
        AtomicInteger triggered = new AtomicInteger(-1);
        int maxValue = 20;
        SpinOption<Integer> option = UsualOptions.level(triggered::set, maxValue);

        assertEquals(UsualOptions.LEVEL_NAME, option.getName());
        assertEquals(maxValue, option.getValue());

        // Within bounds
        option.setValue("10");
        assertEquals(10, option.getValue());
        assertEquals(10, triggered.get());

        // Lower bound
        option.setValue("0");
        assertEquals(0, option.getValue());

        // Upper bound
        option.setValue(Integer.toString(maxValue));
        assertEquals(maxValue, option.getValue());

        // Below min
        assertThrows(IllegalArgumentException.class, () -> option.setValue("-1"));

        // Above max
        final String aboveMax = Integer.toString(maxValue + 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(aboveMax));
    }

    @Test
    void testThreadsOption() {
        AtomicInteger triggered = new AtomicInteger(-1);
        int defaultValue = 2;
        int minThreads = 1;
        int maxThreads = Runtime.getRuntime().availableProcessors();
        SpinOption<Integer> option = UsualOptions.threads(triggered::set, defaultValue);

        assertEquals(UsualOptions.THREADS_NAME, option.getName());
        assertEquals(defaultValue, option.getValue());

        // Within bounds
        option.setValue("3");
        assertEquals(3, option.getValue());
        assertEquals(3, triggered.get());

        // Lower bound
        option.setValue(Integer.toString(minThreads));
        assertEquals(minThreads, option.getValue());

        // Upper bound
        option.setValue(Integer.toString(maxThreads));
        assertEquals(maxThreads, option.getValue());

        // Below min
        final String belowMin = Integer.toString(minThreads - 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(belowMin));

        // Above max
        final String aboveMax = Integer.toString(maxThreads + 1);
        assertThrows(IllegalArgumentException.class, () -> option.setValue(aboveMax));
    }
}
