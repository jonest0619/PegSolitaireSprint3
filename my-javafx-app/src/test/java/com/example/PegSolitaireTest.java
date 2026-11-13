package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PegSolitaireTest {

    @Test
    void eachBoardTypeCreatesExpectedInitialPosition() {
        assertInitialBoard(PegSolitaireGame.BoardType.ENGLISH_33, 7, 7, 32, 3, 3);
        assertInitialBoard(PegSolitaireGame.BoardType.DIAMOND_41, 9, 9, 40, 4, 4);
        assertInitialBoard(PegSolitaireGame.BoardType.HEXAGON_21, 5, 7, 20, 2, 3);
    }

    @Test
    void creatingBoardAgainDoesNotReuseMutableGrid() {
        int[][] first = PegSolitaireGame.BoardType.ENGLISH_33.createInitialGrid();
        int[][] second = PegSolitaireGame.BoardType.ENGLISH_33.createInitialGrid();

        assertNotSame(first, second);
        assertNotSame(first[0], second[0]);
        first[2][2] = 0;

        assertEquals(1, second[2][2]);
        assertEquals(1, PegSolitaireGame.BoardType.ENGLISH_33.createInitialGrid()[2][2]);
    }

    @Test
    void acceptsValidHorizontalAndVerticalJumps() {
        int[][] horizontal = {
            {1, 1, 2}
        };
        int[][] vertical = {
            {1},
            {1},
            {2}
        };

        assertTrue(PegSolitaireGame.isValidMove(horizontal, 0, 0, 0, 2));
        assertTrue(PegSolitaireGame.isValidMove(vertical, 0, 0, 2, 0));
    }

    @Test
    void rejectsMovesThatDoNotFollowPegSolitaireRules() {
        int[][] board = {
            {1, 1, 2},
            {1, 1, 2},
            {2, 2, 2}
        };
        int[][] occupiedDestination = {
            {1, 1, 1}
        };

        assertFalse(PegSolitaireGame.isValidMove(board, 0, 0, 1, 1), "diagonal jump");
        assertFalse(PegSolitaireGame.isValidMove(board, 0, 0, 0, 1), "one-space move");
        assertFalse(PegSolitaireGame.isValidMove(occupiedDestination, 0, 0, 0, 2), "destination is occupied");
        assertFalse(PegSolitaireGame.isValidMove(board, 0, 2, 0, 0), "source has no peg");
        assertFalse(PegSolitaireGame.isValidMove(board, 0, 0, 0, 3), "destination is outside the board");
        assertFalse(PegSolitaireGame.isValidMove(board, 0, 0, -2, 0), "destination is outside the board");
    }

    @Test
    void applyingValidMoveUpdatesThreeHolesAndReducesPegCount() {
        int[][] board = {
            {1, 1, 2}
        };

        assertTrue(PegSolitaireGame.applyMove(board, 0, 0, 0, 2));

        assertArrayEquals(new int[]{2, 2, 1}, board[0]);
        assertEquals(1, PegSolitaireGame.countRemainingPegs(board));
    }

    @Test
    void applyingInvalidMoveLeavesBoardUnchanged() {
        int[][] board = {
            {1, 2, 2}
        };
        int[][] original = {
            {1, 2, 2}
        };

        assertFalse(PegSolitaireGame.applyMove(board, 0, 0, 0, 2));

        assertArrayEquals(original[0], board[0]);
    }

    @Test
    void reportsWhetherAnyLegalMovesRemain() {
        int[][] movable = {
            {1, 1, 2}
        };
        int[][] blocked = {
            {1, 2, 1}
        };

        assertTrue(PegSolitaireGame.hasAnyValidMoves(movable));
        assertFalse(PegSolitaireGame.hasAnyValidMoves(blocked));
    }

    @Test
    void choosingAutomatedModeStartsAutomatedPlayOnSelectedBoardAfterStart() {
        for (PegSolitaireGame.BoardType boardType : PegSolitaireGame.BoardType.values()) {
            PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
            int initialPegCount =
                PegSolitaireGame.countRemainingPegs(boardType.createInitialGrid());
            gameState.startNewGame(boardType);
            gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);

            assertGridEquals(boardType.createInitialGrid(), gameState.getGridSnapshot());
            assertFalse(gameState.performAutomatedTurn().moved(),
                "automated mode must still wait for Start");

            assertTrue(gameState.start());
            PegSolitaireGame.AutomatedTurn turn = gameState.performAutomatedTurn();

            assertTrue(turn.moved(), boardType + " should make an automated move");
            assertEquals(
                initialPegCount - 1,
                PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot())
            );
            assertEquals(10, gameState.getScore());
        }
    }

    @Test
    void choosingManualModeAllowsManualPlayOnSelectedBoardWithoutAutomatedMoves() {
        for (PegSolitaireGame.BoardType boardType : PegSolitaireGame.BoardType.values()) {
            PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
            int initialPegCount =
                PegSolitaireGame.countRemainingPegs(boardType.createInitialGrid());
            gameState.startNewGame(boardType);
            gameState.setGameMode(PegSolitaireGame.GameMode.MANUAL);

            assertGridEquals(boardType.createInitialGrid(), gameState.getGridSnapshot());
            assertTrue(gameState.start());
            assertFalse(gameState.performAutomatedTurn().moved());
            assertEquals(
                initialPegCount,
                PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot())
            );

            PegSolitaireGame.Move manualMove =
                PegSolitaireGame.findFirstLegalMove(gameState.getGridSnapshot()).orElseThrow();
            assertTrue(gameState.applyMove(
                manualMove.fromRow(),
                manualMove.fromCol(),
                manualMove.toRow(),
                manualMove.toCol()
            ));
            assertEquals(10, gameState.getScore());
            assertEquals(
                initialPegCount - 1,
                PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot())
            );
        }
    }

    @Test
    void automatedGameWaitsForStartAndAutomatedMode() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);
        int[][] original = copyGrid(gameState.getGridSnapshot());

        PegSolitaireGame.AutomatedTurn beforeStart = gameState.performAutomatedTurn();

        assertFalse(beforeStart.moved());
        assertTrue(beforeStart.gameOver().isEmpty());
        assertGridEquals(original, gameState.getGridSnapshot());
        assertEquals(0, gameState.getScore());

        assertTrue(gameState.start());
        gameState.setGameMode(PegSolitaireGame.GameMode.MANUAL);
        PegSolitaireGame.AutomatedTurn inManualMode = gameState.performAutomatedTurn();

        assertFalse(inManualMode.moved());
        assertGridEquals(original, gameState.getGridSnapshot());
        assertEquals(0, gameState.getScore());
    }

    @Test
    void ac6_1_startingAutomatedGameMakesLegalMoveOnlyAfterStart() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);

        int pegsBefore = PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot());
        int[][] initialBoard = copyGrid(gameState.getGridSnapshot());
        PegSolitaireGame.AutomatedTurn beforeStart = gameState.performAutomatedTurn();

        assertFalse(beforeStart.moved(), "automated game must wait for Start");
        assertGridEquals(initialBoard, gameState.getGridSnapshot());
        assertEquals(0, gameState.getScore());

        assertTrue(gameState.start());
        PegSolitaireGame.Move expectedMove =
            PegSolitaireGame.findFirstLegalMove(gameState.getGridSnapshot()).orElseThrow();

        PegSolitaireGame.AutomatedTurn turn = gameState.performAutomatedTurn();

        assertTrue(turn.moved());
        int[][] expectedBoard = PegSolitaireGame.BoardType.ENGLISH_33.createInitialGrid();
        assertTrue(PegSolitaireGame.applyMove(
            expectedBoard,
            expectedMove.fromRow(),
            expectedMove.fromCol(),
            expectedMove.toRow(),
            expectedMove.toCol()
        ));
        assertGridEquals(expectedBoard, gameState.getGridSnapshot());
        assertEquals(pegsBefore - 1, PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot()));
        assertEquals(10, gameState.getScore());
        assertTrue(turn.gameOver().isEmpty());
    }

    @Test
    void blockedBoardProducesGameOverMessage() {
        int[][] grid = {
            {1, 2, 1, 0, 2}
        };
        PegSolitaireGame.GameOverResult result =
            PegSolitaireGame.getGameOverResult(grid, 0, "00:00").orElseThrow();

        assertEquals(PegSolitaireGame.GameOutcome.LOST, result.outcome());
        assertTrue(result.message().contains("Game Over"));
        assertTrue(result.message().contains("2 pegs remaining"));
        assertEquals(List.of("Restart", "Exit"), result.actions());
    }

    @Test
    void automatedGameContinuesUntilGameOverThenStops() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);
        assertTrue(gameState.start());

        PegSolitaireGame.AutomatedTurn turn = null;
        int turns = 0;
        while (turns <= 32 && gameState.isInProgress()) {
            turn = gameState.performAutomatedTurn();
            if (turn.moved()) {
                assertEquals(32 - turns - 1, PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot()));
                assertEquals((turns + 1) * 10, gameState.getScore());
            }
            turns++;
        }

        assertTrue(turns <= 32, "automated game should reach a terminal board");
        assertTrue(turn.gameOver().isPresent());
        assertFalse(gameState.isInProgress());
        assertEquals(List.of("Restart", "Exit"), turn.gameOver().orElseThrow().actions());

        int scoreAtGameOver = gameState.getScore();
        int[][] boardAtGameOver = copyGrid(gameState.getGridSnapshot());
        PegSolitaireGame.AutomatedTurn afterGameOver = gameState.performAutomatedTurn();
        assertFalse(afterGameOver.moved());
        assertGridEquals(boardAtGameOver, gameState.getGridSnapshot());
        assertEquals(scoreAtGameOver, gameState.getScore());
    }

    @Test
    void switchingToManualCancelsAutomatedTurnsUntilAutomatedModeReturns() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);
        assertTrue(gameState.start());
        assertTrue(gameState.performAutomatedTurn().moved());

        gameState.setGameMode(PegSolitaireGame.GameMode.MANUAL);
        int[][] boardInManualMode = copyGrid(gameState.getGridSnapshot());
        int scoreInManualMode = gameState.getScore();
        assertFalse(gameState.performAutomatedTurn().moved());
        assertGridEquals(boardInManualMode, gameState.getGridSnapshot());
        assertEquals(scoreInManualMode, gameState.getScore());

        gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);
        assertTrue(gameState.performAutomatedTurn().moved());
        assertEquals(scoreInManualMode + 10, gameState.getScore());
    }

    @Test
    void restartingOrChangingBoardRequiresStartAgainInAutomatedMode() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.setGameMode(PegSolitaireGame.GameMode.AUTOMATED);
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        assertTrue(gameState.start());
        assertTrue(gameState.performAutomatedTurn().moved());

        gameState.startNewGame(PegSolitaireGame.BoardType.DIAMOND_41);

        assertFalse(gameState.isStarted());
        assertEquals(0, gameState.getScore());
        assertEquals(0, gameState.getSecondsElapsed());
        assertEquals(40, PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot()));
        assertFalse(gameState.performAutomatedTurn().moved());
        assertTrue(gameState.start());
        assertTrue(gameState.performAutomatedTurn().moved());
    }

    @Test
    void automatedMovesUseConfiguredDelay() {
        assertEquals(600, PegSolitaire.AUTOMATED_MOVE_DELAY_MILLIS);
    }

    @Test
    void randomizingBoardPreservesPlayableShapeAndPegCountAndLeavesLegalMove() {
        for (PegSolitaireGame.BoardType boardType : PegSolitaireGame.BoardType.values()) {
            int[][] board = boardType.createInitialGrid();
            int[][] original = copyGrid(board);
            int pegCount = PegSolitaireGame.countRemainingPegs(board);

            assertTrue(PegSolitaireGame.randomizeBoard(board, new Random(42)));

            assertEquals(pegCount, PegSolitaireGame.countRemainingPegs(board));
            assertTrue(PegSolitaireGame.hasAnyValidMoves(board));
            for (int row = 0; row < original.length; row++) {
                assertEquals(original[row].length, board[row].length);
                for (int col = 0; col < original[row].length; col++) {
                    if (original[row][col] == 0) {
                        assertEquals(0, board[row][col]);
                    } else {
                        assertTrue(board[row][col] == 1 || board[row][col] == 2);
                    }
                }
            }
        }
    }

    @Test
    void randomizingBoardWithoutEnoughPegsLeavesBoardUnchanged() {
        int[][] board = {
            {1, 2, 2}
        };
        int[][] original = copyGrid(board);

        assertFalse(PegSolitaireGame.randomizeBoard(board, new Random(42)));

        assertGridEquals(original, board);
    }

    @Test
    void randomizingMidgamePreservesCurrentPegCount() {
        int[][] board = PegSolitaireGame.BoardType.ENGLISH_33.createInitialGrid();
        assertTrue(PegSolitaireGame.applyMove(board, 3, 1, 3, 3));
        int pegCount = PegSolitaireGame.countRemainingPegs(board);

        assertTrue(PegSolitaireGame.randomizeBoard(board, new Random(7)));

        assertEquals(pegCount, PegSolitaireGame.countRemainingPegs(board));
        assertTrue(PegSolitaireGame.hasAnyValidMoves(board));
    }

    @Test
    void ac8_1_randomizingBoardDuringManualGameRearrangesPegsAndEmptySpots() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        gameState.setGameMode(PegSolitaireGame.GameMode.MANUAL);
        assertTrue(gameState.start());
        assertTrue(gameState.applyMove(3, 1, 3, 3));
        gameState.incrementSecondsElapsed();

        int[][] original = copyGrid(gameState.getGridSnapshot());
        int pegCount = PegSolitaireGame.countRemainingPegs(original);
        int score = gameState.getScore();
        int elapsedSeconds = gameState.getSecondsElapsed();

        assertTrue(gameState.randomizeBoard(new Random(8)));

        assertFalse(gridsEqual(original, gameState.getGridSnapshot()),
            "randomization should produce a different peg arrangement");
        assertEquals(pegCount, PegSolitaireGame.countRemainingPegs(gameState.getGridSnapshot()));
        assertTrue(PegSolitaireGame.hasAnyValidMoves(gameState.getGridSnapshot()));
        assertEquals(score, gameState.getScore());
        assertEquals(elapsedSeconds, gameState.getSecondsElapsed());
        for (int row = 0; row < original.length; row++) {
            for (int col = 0; col < original[row].length; col++) {
                if (original[row][col] == 0) {
                    assertEquals(0, gameState.getGridSnapshot()[row][col]);
                }
            }
        }
    }

    @Test
    void startNewGameResetScoreAndTime() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);

        assertTrue(gameState.applyMove(3, 1, 3, 3));
        gameState.incrementSecondsElapsed();

        gameState.startNewGame(PegSolitaireGame.BoardType.DIAMOND_41);

        assertEquals(0, gameState.getScore());
        assertEquals(0, gameState.getSecondsElapsed());

        int[][] expectedGrid = PegSolitaireGame.BoardType.DIAMOND_41.createInitialGrid();
        int[][] actualGrid = gameState.getGridSnapshot();
        assertEquals(expectedGrid.length, actualGrid.length);
        for (int row = 0; row < expectedGrid.length; row++) {
            assertArrayEquals(expectedGrid[row], actualGrid[row]);
        }
    }

    @Test
    void changingSnapshotDoesNotMutateGameState() {
        PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
        gameState.startNewGame(PegSolitaireGame.BoardType.ENGLISH_33);
        int[][] snapshot = gameState.getGridSnapshot();
        snapshot[3][1] = 2;
        snapshot[3][3] = 1;

        assertGridEquals(
            PegSolitaireGame.BoardType.ENGLISH_33.createInitialGrid(),
            gameState.getGridSnapshot()
        );
    }

    @Test
    void validHorizontalMoveMovesPegAndRemovesJumpedPeg() {
        // Arrange: peg, peg, empty hole
        int[][] board = {
            {1, 1, 2}
    };

        // Act: jump from column 0 to column 2
        boolean moved = PegSolitaireGame.applyMove(board, 0, 0, 0, 2);

        // Assert: starting hole and jumped hole are empty;
        // destination contains the moved peg.
        assertTrue(moved);
        assertArrayEquals(new int[]{2, 2, 1}, board[0]);
        assertEquals(1, PegSolitaireGame.countRemainingPegs(board));
    }

    @Test
    void validVerticalMoveMovesPegAndRemovesJumpedPeg() {
        // Arrange: peg, peg, empty hole vertically
        int[][] board = {
            {1},
            {1},
            {2}
        };

        // Act: jump from row 0 to row 2
        boolean moved = PegSolitaireGame.applyMove(board, 0, 0, 2, 0);

        // Assert
        assertTrue(moved);
        assertEquals(2, board[0][0], "Starting hole should be empty");
        assertEquals(2, board[1][0], "Jumped peg should be removed");
        assertEquals(1, board[2][0], "Destination should contain the moved peg");
        assertEquals(1, PegSolitaireGame.countRemainingPegs(board));
    }

    @Test
    void finalMoveWithOnePegReportsVictory() {
        int[][] board = {
            {1, 1, 2}
        };
        assertTrue(PegSolitaireGame.applyMove(board, 0, 0, 0, 2));

        Optional<PegSolitaireGame.GameOverResult> result =
            PegSolitaireGame.getGameOverResult(board, 10, "00:01");

        assertTrue(result.isPresent());
        assertEquals(PegSolitaireGame.GameOutcome.WON, result.get().outcome());
        assertEquals(1, result.get().remainingPegs());
        assertTrue(result.get().title().toLowerCase().contains("win"));
        assertTrue(result.get().message().contains("1 peg remaining"));
        assertTrue(result.get().message().contains("Final Score: 10"));
        assertTrue(result.get().message().contains("Time: 00:01"));
        assertEquals(List.of("Restart", "Exit"), result.get().actions());
    }

    @Test
    void finalMoveWithMultiplePegsReportsDefeatAndPegCount() {
        int[][] board = {
            {1, 1, 2, 0, 1}
        };
        assertTrue(PegSolitaireGame.applyMove(board, 0, 0, 0, 2));

        Optional<PegSolitaireGame.GameOverResult> result =
            PegSolitaireGame.getGameOverResult(board, 10, "00:01");

        assertTrue(result.isPresent());
        assertEquals(PegSolitaireGame.GameOutcome.LOST, result.get().outcome());
        assertEquals(2, result.get().remainingPegs());
        assertEquals("Game Over", result.get().title());
        assertTrue(result.get().message().contains("2 pegs remaining"));
        assertTrue(result.get().message().contains("Final Score: 10"));
        assertTrue(result.get().message().contains("Time: 00:01"));
        assertEquals(List.of("Restart", "Exit"), result.get().actions());
    }

    @Test
    void gameOverIsNotReportedWhileValidMovesRemain() {
        int[][] board = {
            {1, 1, 2}
        };

        assertEquals(
            Optional.empty(),
            PegSolitaireGame.getGameOverResult(board, 0, "00:00")
        );
    }

    private static int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }

    private static void assertGridEquals(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], actual[row]);
        }
    }

    private static boolean gridsEqual(int[][] first, int[][] second) {
        if (first.length != second.length) {
            return false;
        }
        for (int row = 0; row < first.length; row++) {
            if (!java.util.Arrays.equals(first[row], second[row])) {
                return false;
            }
        }
        return true;
    }

    private static void assertInitialBoard(
        PegSolitaireGame.BoardType boardType,
        int expectedRows,
        int expectedColumns,
        int expectedPegs,
        int emptyRow,
        int emptyColumn
    ) {
        int[][] grid = boardType.createInitialGrid();

        assertEquals(expectedRows, grid.length);
        for (int[] row : grid) {
            assertEquals(expectedColumns, row.length);
        }
        assertEquals(2, grid[emptyRow][emptyColumn]);
        assertEquals(expectedPegs, PegSolitaireGame.countRemainingPegs(grid));
    }
}
