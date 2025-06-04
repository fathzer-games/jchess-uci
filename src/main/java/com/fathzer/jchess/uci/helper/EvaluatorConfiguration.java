package com.fathzer.jchess.uci.helper;

import java.util.function.Supplier;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.Evaluator;

/**
 * The configuration of an evaluator.
 * @param <M> The type of moves the evaluator can handle
 * @param <B> The type of board the evaluator can handle
 * @see AbstractEngine#setEvaluators(java.util.List)
 * @param name The name of the evaluator
 * @param evaluatorBuilder A supplier that can create an evaluator
 */
public record EvaluatorConfiguration<M, B extends MoveGenerator<M>> (String name, Supplier<Evaluator<M, B>> evaluatorBuilder) {
    /**
     * @throws IllegalArgumentException If the name or the evaluator builder is null
     */
    public EvaluatorConfiguration {
        if (name == null || evaluatorBuilder == null) {
            throw new IllegalArgumentException();
        }
    }
}
