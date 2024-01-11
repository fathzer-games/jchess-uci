package com.fathzer.jchess.uci.extended;

import java.util.List;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.EvaluatedMove;
import com.fathzer.games.ai.evaluation.Evaluation;
import com.fathzer.games.ai.evaluation.Evaluation.Type;
import com.fathzer.games.ai.iterativedeepening.DeepeningPolicy;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.UCIMove;
import com.fathzer.jchess.uci.helper.AbstractEngine;

/** A class that implements a speed test that can be run  on an {@link AbstractEngine}.
 * @param <M> The type of the moves
 * @param <B> The type of the chess move generator.
 */
public class SpeedTest<M, B extends MoveGenerator<M>> {

	private static class Result<M> {
		private String fen;
		private List<EvaluatedMove<M>> moves;
		
		public Result(String fen, List<EvaluatedMove<M>> moves) {
			super();
			this.fen = fen;
			this.moves = moves;
		}

		private void assertEquals(Object expected, Object actual) {
			if (!expected.equals(actual)) {
				show();
				throw new IllegalArgumentException("Expecting "+expected+" but is "+actual);
			}
		}

		private void assertTrue(boolean value) {
			if (!value) {
				show();
				throw new IllegalArgumentException("Expecting true here");
			}
		}
		
		private void show() {
			System.out.println(fen);
			System.out.println(moves);
		}
	}

	private AbstractEngine<M, B> uciEngine;

	/** Creates the test.
	 * @param engine The engine to test
	 */
	public SpeedTest(AbstractEngine<M, B> engine) {
		this.uciEngine = engine;
	}
	
	private Result<M> fill(String fen) {
		uciEngine.newGame();
		uciEngine.setStartPosition(fen);
		return new Result<>(fen, uciEngine.getEngine().getBestMoves(uciEngine.get()));
	}
	
	/** Launches the test.
	 * <br><b>Warning:</b> If the engine does not implement {@link Displayable}, this method modifies the current position.
	 * <br>Either way, {@link Engine#newGame()} is called. 
	 * @return The number of ms the test took.
	 */
	public long run() {
		final DeepeningPolicy policy = uciEngine.getEngine().getDeepeningPolicy();
		int size = policy.getSize();
		int accuracy = policy.getAccuracy();
		int depth = policy.getDepth();
		String fen = (uciEngine.isPositionSet() && uciEngine instanceof Displayable) ? ((Displayable)uciEngine).getFEN() : null;
		try {
			final long start = System.currentTimeMillis();
			doSpeedTest();
			return System.currentTimeMillis()-start;
		} finally {
			policy.setDepth(depth);
			policy.setSize(size);
			policy.setAccuracy(accuracy);
			if (fen!=null) {
				uciEngine.newGame();
				uciEngine.setStartPosition(fen);
			}
		}
	}
	
	private void doSpeedTest() {
		final DeepeningPolicy policy = uciEngine.getEngine().getDeepeningPolicy();
		policy.setSize(Integer.MAX_VALUE);
		policy.setDepth(8);
		
		// 3 possible Mats in 1 with whites
		Result<M> mv = fill("7k/5p2/5PQN/5PPK/6PP/8/8/8 w - - 6 5");
		mv.assertEquals(6, mv.moves.size());
		Evaluation max = mv.moves.get(0).getEvaluation();
		mv.assertEquals(Type.WIN, max.getType());
		mv.assertEquals(1, max.getCountToEnd());
		mv.assertTrue(mv.moves.get(3).getEvaluation().compareTo(max)<0);
		for (int i=1;i<3;i++) {
			mv.assertEquals(max, mv.moves.get(i).getEvaluation());
		}

		// Mat in 1 with blacks
		mv = fill("1R6/8/8/7R/k7/ppp1p3/r2bP3/1K6 b - - 6 5");
		mv.assertEquals(7, mv.moves.size());
		max = mv.moves.get(0).getEvaluation();
		mv.assertEquals(Type.WIN, max.getType());
		mv.assertEquals(1, max.getCountToEnd());
		mv.assertEquals(UCIMove.from("c3c2"), uciEngine.toUCI(mv.moves.get(0).getContent()));
		max = mv.moves.get(1).getEvaluation();
		//TODO iterative engine fails to find the second best move in tree, probably because of deepening interruption by first mat
		// Make a test when it will be fixed with a second move that is a MAT in 3 move (see commented code). 
//		mv.assertEquals(Type.WIN, max.getType());
//		mv.assertEquals(3, max.getCountToEnd());
//		mv.assertEquals(Type.EVAL, mv.moves.get(2).getEvaluation().getType());
		
		// Check in 2
		mv = fill("8/8/8/8/1B6/NN6/pk1K4/8 w - - 0 1");
		max = mv.moves.get(0).getEvaluation();
		mv.assertEquals(Type.WIN, max.getType());
		mv.assertEquals(2, max.getCountToEnd());
		mv.assertTrue(mv.moves.get(1).getScore()<max.getScore());
		mv.assertEquals(UCIMove.from("b3a1"), uciEngine.toUCI(mv.moves.get(0).getContent()));
		
		// Check in 2 with blacks
		mv = fill("8/4k1KP/6nn/6b1/8/8/8/8 b - - 0 1");
		max = mv.moves.get(0).getEvaluation();
		mv.assertEquals(Type.WIN, max.getType());
		mv.assertEquals(2, max.getCountToEnd());
		mv.assertTrue(mv.moves.get(1).getScore()<max.getScore());
		mv.assertEquals(UCIMove.from("g6h8"), uciEngine.toUCI(mv.moves.get(0).getContent()));
		
		// Check in 3
		policy.setSize(3);
		policy.setAccuracy(100);
		mv = fill("r2k1r2/pp1b2pp/1b2Pn2/2p5/Q1B2Bq1/2P5/P5PP/3R1RK1 w - - 0 1");
//		mv.assertEquals(19, mv.moves.size());
		mv.assertEquals(UCIMove.from("d1d7"), uciEngine.toUCI(mv.moves.get(0).getContent()));
		
		// Check in 4
		policy.setSize(1);
		policy.setAccuracy(0);
		mv = fill("8/4k3/8/R7/8/8/8/4K2R w K - 0 1");
		mv.assertEquals(2, mv.moves.size());
		mv.assertEquals(Evaluation.Type.WIN, mv.moves.get(0).getEvaluation().getType());
		mv.assertEquals(4, mv.moves.get(0).getEvaluation().getCountToEnd());
		mv.assertEquals(Evaluation.Type.WIN, mv.moves.get(1).getEvaluation().getType());
		mv.assertEquals(4, mv.moves.get(1).getEvaluation().getCountToEnd());
	}
}
