package com.fathzer.jchess.uci.extended;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fathzer.games.MoveGenerator;
import com.fathzer.games.perft.PerfT;
import com.fathzer.games.perft.PerfTBuilder;
import com.fathzer.games.perft.PerfTResult;
import com.fathzer.jchess.uci.Engine;
import com.fathzer.jchess.uci.parameters.PerfTParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

class PerftTaskTest {

	private PerfTParameters params;
	private PerfTBuilder<String> perfTbuilder;
    private MoveGenerator<String> moveGenerator;
    private PerfT<String> perft;
    private PerfTResult<String> perftResult;
    private Supplier<MoveGenerator<String>> engineSupplier;

    private class MyTask extends PerftTask<String> {

		private MyTask() {
			super(engineSupplier, params);
		}

		@Override
		PerfTBuilder<String> createBuilder() {
		    return perfTbuilder;
		}
	}

    @SuppressWarnings("unchecked")
	@BeforeEach
    void setUp() {
        params = mock(PerfTParameters.class);
        when(params.getParallelism()).thenReturn(1);
        moveGenerator = mock(MoveGenerator.class, withSettings().extraInterfaces(Engine.class));
        when(moveGenerator.fork()).thenReturn(moveGenerator);
        perft = mock(PerfT.class);
        perftResult = mock(PerfTResult.class);
        engineSupplier = () -> moveGenerator;
        when(perft.get()).thenReturn(perftResult);
        perfTbuilder = mock(PerfTBuilder.class);
        doReturn(perft).when(perfTbuilder).build(any(), anyInt());
    }

    @Test
    void testCall_LegalMovesPlayLeaves() {
        // Arrange
        when(params.isLegal()).thenReturn(true);
        when(params.isPlayLeaves()).thenReturn(true);
        when(params.getDepth()).thenReturn(2);

        // Act
        PerfTResult<String> result = new MyTask().call();

        // Assert
        assertSame(perftResult, result);
        verify(perfTbuilder).setLegalMoves(true);
        verify(perfTbuilder, never()).setPlayLeaves(false); // since isPlayLeaves is true
        verify(perfTbuilder).setExecutor(any(ForkJoinPool.class));
        verify(perfTbuilder).build(any(), eq(2));
        verify(perft).get();
    }

    @Test
    void testCall_LegalMovesNoPlayLeaves() {
        when(params.isLegal()).thenReturn(true);
        when(params.isPlayLeaves()).thenReturn(false);
        when(params.getDepth()).thenReturn(3);

        PerfTResult<String> result = new MyTask().call();

        assertSame(perftResult, result);
        verify(perfTbuilder).setLegalMoves(true);
        verify(perfTbuilder).setPlayLeaves(false); // since isPlayLeaves is false
        verify(perfTbuilder).setExecutor(any(ForkJoinPool.class));
        verify(perfTbuilder).build(any(), eq(3));
        verify(perft).get();
    }

    @Test
    void testCall_NotLegalMoves() {
        when(params.isLegal()).thenReturn(false);
        when(params.getDepth()).thenReturn(1);

        PerfTResult<String> result = new MyTask().call();

        assertSame(perftResult, result);
        verify(perfTbuilder, never()).setLegalMoves(true);
        verify(perfTbuilder, never()).setPlayLeaves(false);
        verify(perfTbuilder).setExecutor(any(ForkJoinPool.class));
        verify(perfTbuilder).build(any(), eq(1));
        verify(perft).get();
    }

    @Test
    void testStop2() {
        when(params.getDepth()).thenReturn(1);
        final MyTask task = new MyTask();
        
        assertThrows(IllegalStateException.class, () -> task.stop());
        
		task.call();
        task.stop();
        verify(perft).interrupt();
    }
}