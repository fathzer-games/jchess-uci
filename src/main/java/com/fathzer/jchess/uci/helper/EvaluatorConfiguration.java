package com.fathzer.jchess.uci.helper;

import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.Evaluator;

/**
 * The configuration of an evaluator.
 * @param <M> The type of moves the evaluator can handle
 * @param <B> The type of board the evaluator can handle
 * @see AbstractEngine#setEvaluators(java.util.List)
 */
public class EvaluatorConfiguration<M, B extends MoveGenerator<M>> {
	private final String name;
	private final Supplier<Evaluator<M, B>> evaluatorBuilder;
	
	/** Constructor
	 * @param name The name of the evaluator
	 * @param evaluatorBuilder A supplier that can create an evaluator
	 * @throws IllegalArgumentException If the name or the evaluator builder is null
	 */
	public EvaluatorConfiguration(String name, Supplier<Evaluator<M, B>> evaluatorBuilder) {
		if (name==null || evaluatorBuilder==null) {
			throw new IllegalArgumentException();
		}
		this.name = name;
		this.evaluatorBuilder = evaluatorBuilder;
	}
	
	/** Gets the name of the evaluator.
	 * @return A string
	 */
	public String getName() {
		return name;
	}
	
	/** Gets the supplier of the evaluator.
	 * @return A supplier that can create an evaluator
	 */
	public Supplier<Evaluator<M, B>> getBuilder() {
		return evaluatorBuilder;
	}
}
