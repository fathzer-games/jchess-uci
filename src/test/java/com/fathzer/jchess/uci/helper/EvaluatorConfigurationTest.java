package com.fathzer.jchess.uci.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.ai.evaluation.Evaluator;

class EvaluatorConfigurationTest {
    @Test
    void testValidConfiguration() {
        // Given
        String name = "testEvaluator";
        @SuppressWarnings("unchecked")
        Evaluator<String, MoveGenerator<String>> evaluator = mock(Evaluator.class);
        Supplier<Evaluator<String, MoveGenerator<String>>> supplier = () -> evaluator;
        
        // When
        EvaluatorConfiguration<String, MoveGenerator<String>> config = new EvaluatorConfiguration<>(name, supplier);
        
        // Then
        assertEquals(name, config.name());
        assertSame(supplier, config.evaluatorBuilder());
        assertSame(evaluator, config.evaluatorBuilder().get());
    }
    
    @Test
    void testNullName() {
        @SuppressWarnings("unchecked")
        Supplier<Evaluator<String, MoveGenerator<String>>> supplier = () -> mock(Evaluator.class);
        assertThrows(IllegalArgumentException.class, () -> new EvaluatorConfiguration<>(null, supplier));
        assertThrows(IllegalArgumentException.class, () -> new EvaluatorConfiguration<>("test", null));
    }
}
