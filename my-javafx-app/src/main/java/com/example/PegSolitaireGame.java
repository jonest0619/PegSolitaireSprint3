package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

final class PegSolitaireGame {
    enum GameMode {
        MANUAL("Manual"),
        AUTOMATED("Automated");

        private final String label;

        GameMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    record Move(int fromRow, int fromCol, int toRow, int toCol) {}

    record AutomatedTurn(boolean moved, Optional<GameOverResult> gameOver) {}

    enum GameOutcome {
        WON,
        LOST
    }

    record GameOverResult(
        GameOutcome outcome,
        int remainingPegs,
        String title,
        String message,
        List<String> actions
    ) {}

    enum BoardType {
        ENGLISH_33("English (33 Holes)", new int[][]{
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 0, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 2, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 0, 0}
        }),
        DIAMOND_41("Diamond (41 Holes)", new int[][]{
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 0, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 0},
            {1, 1, 1, 1, 2, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0}
        }),
        HEXAGON_21("Hexagon (21 Holes)", new int[][]{
            {0, 0, 1, 1, 1, 0, 0},
            {0, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 2, 1, 1, 0},
            {0, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 0, 0}
        });

        private final String displayName;
        private final int[][] defaultGrid;

        BoardType(String displayName, int[][] defaultGrid) {
            this.displayName = displayName;
            this.defaultGrid = defaultGrid;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int[][] createInitialGrid() {
            int[][] copy = new int[defaultGrid.length][];
            for (int row = 0; row < defaultGrid.length; row++) {
                copy[row] = defaultGrid[row].clone();
            }
            return copy;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static final class GameState {
        private int[][] grid;
        private int score;
        private int secondsElapsed;
        private GameMode gameMode = GameMode.MANUAL;
        private BoardType boardType;
        private boolean started;
        private boolean inProgress;

        void startNewGame(BoardType boardType) {
            this.boardType = boardType;
            grid = boardType.createInitialGrid();
            score = 0;
            secondsElapsed = 0;
            started = false;
            inProgress = true;
        }

        boolean start() {
            if (!inProgress || started) {
                return false;
            }
            started = true;
            return true;
        }

        void setGameMode(GameMode gameMode) {
            this.gameMode = gameMode;
        }

        boolean isAutomatedMoveAllowed() {
            return inProgress && started && gameMode == GameMode.AUTOMATED;
        }

        boolean isStarted() {
            return started;
        }

        boolean isInProgress() {
            return inProgress;
        }

        BoardType getBoardType() {
            return boardType;
        }

        int[][] getGridSnapshot() {
            int[][] copy = new int[grid.length][];
            for (int row = 0; row < grid.length; row++) {
                copy[row] = grid[row].clone();
            }
            return copy;
        }

        int getCellState(int row, int col) {
            return grid[row][col];
        }

        boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
            return PegSolitaireGame.isValidMove(grid, fromRow, fromCol, toRow, toCol);
        }

        int getScore() {
            return score;
        }

        int getSecondsElapsed() {
            return secondsElapsed;
        }

        void incrementSecondsElapsed() {
            secondsElapsed++;
        }

        boolean applyMove(int fromRow, int fromCol, int toRow, int toCol) {
            if (!PegSolitaireGame.applyMove(grid, fromRow, fromCol, toRow, toCol)) {
                return false;
            }
            score += 10;
            return true;
        }

        boolean randomizeBoard(Random random) {
            return PegSolitaireGame.randomizeBoard(grid, random);
        }

        Optional<GameOverResult> getGameOverResult() {
            return PegSolitaireGame.getGameOverResult(grid, score, formatTime());
        }

        AutomatedTurn performAutomatedTurn() {
            if (!isAutomatedMoveAllowed()) {
                return new AutomatedTurn(false, Optional.empty());
            }

            Optional<Move> move = findFirstLegalMove(grid);
            if (move.isEmpty()) {
                return finishAutomatedGame(false);
            }

            Move next = move.get();
            if (!applyMove(next.fromRow(), next.fromCol(), next.toRow(), next.toCol())) {
                return new AutomatedTurn(false, Optional.empty());
            }

            Optional<GameOverResult> result = getGameOverResult();
            if (result.isPresent()) {
                inProgress = false;
            }
            return new AutomatedTurn(true, result);
        }

        void finish() {
            inProgress = false;
        }

        private AutomatedTurn finishAutomatedGame(boolean moved) {
            Optional<GameOverResult> result = getGameOverResult();
            if (result.isPresent()) {
                inProgress = false;
            }
            return new AutomatedTurn(moved, result);
        }

        private String formatTime() {
            return String.format("%02d:%02d", secondsElapsed / 60, secondsElapsed % 60);
        }
    }

    static Optional<Move> findFirstLegalMove(int[][] grid) {
        int[][] directions = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] != 1) {
                    continue;
                }

                for (int[] direction : directions) {
                    int toRow = row + direction[0];
                    int toCol = col + direction[1];
                    if (isValidMove(grid, row, col, toRow, toCol)) {
                        return Optional.of(new Move(row, col, toRow, toCol));
                    }
                }
            }
        }

        return Optional.empty();
    }

    static boolean isValidMove(int[][] grid, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow < 0 || fromRow >= grid.length ||
            fromCol < 0 || fromCol >= grid[fromRow].length ||
            toRow < 0 || toRow >= grid.length ||
            toCol < 0 || toCol >= grid[toRow].length) {
            return false;
        }

        if (grid[fromRow][fromCol] != 1 || grid[toRow][toCol] != 2) {
            return false;
        }

        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        boolean isOrthogonalJump =
            (rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2);
        if (!isOrthogonalJump) {
            return false;
        }

        return grid[(fromRow + toRow) / 2][(fromCol + toCol) / 2] == 1;
    }

    static boolean applyMove(int[][] grid, int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidMove(grid, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        int middleRow = (fromRow + toRow) / 2;
        int middleCol = (fromCol + toCol) / 2;
        grid[fromRow][fromCol] = 2;
        grid[middleRow][middleCol] = 2;
        grid[toRow][toCol] = 1;
        return true;
    }

    static boolean randomizeBoard(int[][] grid, Random random) {
        List<int[]> playablePositions = new ArrayList<>();
        List<Move> possibleMoves = new ArrayList<>();
        int pegCount = 0;

        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] != 0) {
                    playablePositions.add(new int[]{row, col});
                    if (grid[row][col] == 1) {
                        pegCount++;
                    }
                }
            }
        }

        if (pegCount < 2 || pegCount >= playablePositions.size()) {
            return false;
        }

        int[][] directions = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for (int[] position : playablePositions) {
            int fromRow = position[0];
            int fromCol = position[1];
            for (int[] direction : directions) {
                int toRow = fromRow + direction[0];
                int toCol = fromCol + direction[1];
                int middleRow = fromRow + direction[0] / 2;
                int middleCol = fromCol + direction[1] / 2;

                if (toRow >= 0 && toRow < grid.length &&
                    toCol >= 0 && toCol < grid[toRow].length &&
                    grid[middleRow][middleCol] != 0 &&
                    grid[toRow][toCol] != 0) {
                    possibleMoves.add(new Move(fromRow, fromCol, toRow, toCol));
                }
            }
        }

        if (possibleMoves.isEmpty()) {
            return false;
        }

        int[][] randomizedGrid = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            randomizedGrid[row] = grid[row].clone();
        }
        for (int[] position : playablePositions) {
            randomizedGrid[position[0]][position[1]] = 2;
        }

        Move guaranteedMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
        int middleRow = (guaranteedMove.fromRow() + guaranteedMove.toRow()) / 2;
        int middleCol = (guaranteedMove.fromCol() + guaranteedMove.toCol()) / 2;
        randomizedGrid[guaranteedMove.fromRow()][guaranteedMove.fromCol()] = 1;
        randomizedGrid[middleRow][middleCol] = 1;

        List<int[]> remainingPositions = new ArrayList<>(playablePositions);
        remainingPositions.removeIf(position ->
            (position[0] == guaranteedMove.fromRow() &&
                position[1] == guaranteedMove.fromCol()) ||
            (position[0] == middleRow && position[1] == middleCol) ||
            (position[0] == guaranteedMove.toRow() &&
                position[1] == guaranteedMove.toCol())
        );
        Collections.shuffle(remainingPositions, random);
        for (int index = 0; index < pegCount - 2; index++) {
            int[] position = remainingPositions.get(index);
            randomizedGrid[position[0]][position[1]] = 1;
        }

        for (int row = 0; row < grid.length; row++) {
            System.arraycopy(randomizedGrid[row], 0, grid[row], 0, grid[row].length);
        }
        return true;
    }

    static Optional<GameOverResult> getGameOverResult(int[][] grid, int score, String time) {
        if (hasAnyValidMoves(grid)) {
            return Optional.empty();
        }

        int remainingPegs = countRemainingPegs(grid);
        if (remainingPegs == 1) {
            return Optional.of(new GameOverResult(
                GameOutcome.WON,
                remainingPegs,
                "YOU WINNNNN!",
                String.format(
                    "1 peg remaining!\nFinal Score: %d | Time: %s",
                    score,
                    time
                ),
                List.of("Restart", "Exit")
            ));
        }

        return Optional.of(new GameOverResult(
            GameOutcome.LOST,
            remainingPegs,
            "Game Over",
            String.format(
                "Game Over :O You have %d pegs remaining. \nFinal Score: %d | Time: %s",
                remainingPegs,
                score,
                time
            ),
            List.of("Restart", "Exit")
        ));
    }

    static int countRemainingPegs(int[][] grid) {
        int pegs = 0;
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell == 1) {
                    pegs++;
                }
            }
        }
        return pegs;
    }

    static boolean hasAnyValidMoves(int[][] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] == 1 &&
                    (isValidMove(grid, row, col, row - 2, col) ||
                        isValidMove(grid, row, col, row + 2, col) ||
                        isValidMove(grid, row, col, row, col - 2) ||
                        isValidMove(grid, row, col, row, col + 2))) {
                    return true;
                }
            }
        }
        return false;
    }
}
