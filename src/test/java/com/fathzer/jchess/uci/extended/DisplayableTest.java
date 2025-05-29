package com.fathzer.jchess.uci.extended;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisplayableTest {

    @Test
    void getBoardAsString_returnsSameAsGetFEN() {
        // Arrange
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Displayable mockDisplayable = mock(Displayable.class);

        // Only stub getFEN, let getBoardAsString use the default implementation
        when(mockDisplayable.getFEN()).thenReturn(fen);
        // Use CALLS_REAL_METHODS to use default methods
        when(mockDisplayable.getBoardAsString()).thenCallRealMethod();

        // Act & Assert
        assertEquals(fen, mockDisplayable.getBoardAsString());
    }
}