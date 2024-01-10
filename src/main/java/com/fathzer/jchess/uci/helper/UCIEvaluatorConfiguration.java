package com.fathzer.jchess.uci.helper;

import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.Evaluator;

public class UCIEvaluatorConfiguration<M, B extends MoveGenerator<M>> {
	private final String name;
	private final Supplier<Evaluator<M, B>> evaluatorBuilder;
	
	public UCIEvaluatorConfiguration(String name, Supplier<Evaluator<M, B>> evaluatorBuilder) {
		if (name==null || evaluatorBuilder==null) {
			throw new IllegalArgumentException();
		}
		this.name = name;
		this.evaluatorBuilder = evaluatorBuilder;
	}

	public String getName() {
		return name;
	}

	public Supplier<Evaluator<M, B>> getEvaluatorBuilder() {
		return evaluatorBuilder;
	}
}
