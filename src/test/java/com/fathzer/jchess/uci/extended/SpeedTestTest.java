package com.fathzer.jchess.uci.extended;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.EvaluatedMove;
import com.fathzer.games.ai.evaluation.Evaluation;
import com.fathzer.games.ai.iterativedeepening.DeepeningPolicy;
import com.fathzer.games.ai.iterativedeepening.IterativeDeepeningEngine;
import com.fathzer.games.ai.iterativedeepening.SearchHistory;
import com.fathzer.games.ai.transposition.TranspositionTable;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.helper.AbstractEngine;

class SpeedTestTest {
	@Test
	void test() {
        final List<String> out = new ArrayList<>();
        final AtomicReference<String> fenRef = new AtomicReference<>();
        @SuppressWarnings("unchecked")
        IterativeDeepeningEngine<String, MoveGenerator<String>> itEngine = mock(IterativeDeepeningEngine.class);
        final DeepeningPolicy policy = new DeepeningPolicy(20);
        when(itEngine.getDeepeningPolicy()).thenReturn(policy);
        AbstractEngine<String, MoveGenerator<String>> engine = new AbstractEngine<>(itEngine, null) {
            @Override
            public void setStartPosition(String fen) {
                fenRef.set(fen);
            }
            @Override
            public UCIMove toUCI(String move) {
                return UCIMove.from(move);
            }
            @Override
            public String toMove(UCIMove move) {
                return move.toString();
            }
            @Override
            public TranspositionTable<String, MoveGenerator<String>> buildTranspositionTable(int sizeInMB) {
                throw new UnsupportedOperationException();
            }
        };
		SpeedTest<String, MoveGenerator<String>> test = new SpeedTest<>(engine, s->out.add(s.toString()));

        when(itEngine.getBestMoves(any())).thenAnswer(invocationOnMock -> getFailedBestMoves());
        assertThrows(IllegalArgumentException.class, test::run);
        assertFalse(out.isEmpty());

        when(itEngine.getBestMoves(any())).thenAnswer(invocationOnMock -> getBestMoves(policy, fenRef.get()));
        out.clear();
        test.run();
        System.out.println(out);
        assertTrue(out.isEmpty());
	}

    private SearchHistory<String> getBestMoves(DeepeningPolicy policy, String fen) {
        SearchHistory<String> history = new SearchHistory<>(policy);
        if (fen.startsWith("7k/5p2/5PQN/5PPK/6PP/8/8/8 w")) {
            history.add(List.of(
                new EvaluatedMove<>("h6f7", Evaluation.win(1, 1000)),
                new EvaluatedMove<>("g6g7", Evaluation.win(1, 1000)),
                new EvaluatedMove<>("g6g8", Evaluation.win(1, 1000)),
                new EvaluatedMove<>("g6h7", Evaluation.win(5, 996)),
                new EvaluatedMove<>("h6g8", Evaluation.win(11, 990)),
                new EvaluatedMove<>("g6f7", Evaluation.score(0))
                ), 1);
        } else if (fen.startsWith("1R6/8/8/7R/k7/ppp1p3/r2bP3/1K6 b")) {
            history.add(List.of(
                new EvaluatedMove<>("c3c2", Evaluation.win(1, 1000)),
                new EvaluatedMove<>("a2b2", Evaluation.win(3, 998)),
                new EvaluatedMove<>("d2e1", Evaluation.win(16, 985)),
                new EvaluatedMove<>("d2c1", Evaluation.score(0)),
                new EvaluatedMove<>("a2c2", Evaluation.score(0)),
                new EvaluatedMove<>("a2a1", Evaluation.score(0)),
                new EvaluatedMove<>("b3b2", Evaluation.score(-714))
                ), 1);
        } else if (fen.startsWith("8/8/8/8/1B6/NN6/pk1K4/8 w")) {
            history.add(List.of(
                new EvaluatedMove<>("b3a1", Evaluation.win(2, 999)),
                new EvaluatedMove<>("b3c1", Evaluation.score(-714))
                ), 1);
        } else if (fen.startsWith("8/4k1KP/6nn/6b1/8/8/8/8 b")) {
            history.add(List.of(
                new EvaluatedMove<>("g6h8", Evaluation.win(2, 999)),
                new EvaluatedMove<>("g6f8", Evaluation.score(-84))
                ), 1);
        } else if (fen.startsWith("r2k1r2/pp1b2pp/1b2Pn2/2p5/Q1B2Bq1/2P5/P5PP/3R1RK1 w")) {
            history.add(List.of(
                new EvaluatedMove<>("d1d7", Evaluation.win(2, 1000)),
                new EvaluatedMove<>("f4e5", Evaluation.win(10, 990))
                ), 1);
        } else if (fen.startsWith("8/4k3/8/R7/8/8/8/4K2R w K")) {
            history.add(List.of(
                new EvaluatedMove<>("h1h6", Evaluation.win(4, 997)),
                new EvaluatedMove<>("a5a6", Evaluation.win(4, 997)),
                new EvaluatedMove<>("e1e2", Evaluation.score(0))
                ), 1);
        } else {
            System.out.println(fen);
        }
        return history;
    }

    private SearchHistory<String> getFailedBestMoves() {
        return new SearchHistory<>(new DeepeningPolicy(5));
    }
}
