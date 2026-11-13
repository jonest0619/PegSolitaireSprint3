package com.example;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Optional;
import java.util.Random;

public class PegSolitaire extends Application {
    static final int AUTOMATED_MOVE_DELAY_MILLIS = 600;

    private final PegSolitaireGame.GameState gameState = new PegSolitaireGame.GameState();
    private GridPane boardContainer;
    private ComboBox<PegSolitaireGame.BoardType> boardSelector;
    private ComboBox<PegSolitaireGame.GameMode> gameModeSelector;
    private final PauseTransition autoMovePause =
        new PauseTransition(Duration.millis(AUTOMATED_MOVE_DELAY_MILLIS));


    // Game Metrics (AC 3.2)
    private Label scoreLabel;
    private Label timerLabel;
    private Timeline gameTimer;
    private Button startButton;
    private Button randomizeButton;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        gameModeSelector = new ComboBox<>();
        gameModeSelector.getItems().setAll(PegSolitaireGame.GameMode.values());
        gameModeSelector.setValue(PegSolitaireGame.GameMode.MANUAL);
        gameModeSelector.setOnAction(e -> handleGameModeChange());



        boardSelector = new ComboBox<>();
        boardSelector.getItems().setAll(PegSolitaireGame.BoardType.values());
        boardSelector.setValue(PegSolitaireGame.BoardType.ENGLISH_33);

        boardSelector.setOnAction(e -> handleBoardSelectionChange());

        startButton = new Button("Start");
        startButton.setOnAction(e -> startGameTimer());

        randomizeButton = new Button("Randomize");
        randomizeButton.setDisable(true);
        randomizeButton.setOnAction(e -> handleRandomizeRequest());

        Button restartButton = new Button("Restart");
        restartButton.setOnAction(e -> handleRestartRequest());

        scoreLabel = new Label("Score: 0");
        timerLabel = new Label("Time: 00:00");

        String metricStyle = "-fx-font-weight: bold; -fx-font-size: 13px;";
        scoreLabel.setStyle(metricStyle);
        timerLabel.setStyle(metricStyle);

        HBox topBar = new HBox(12, 
            new Label("Board Size and Type:"), 
            boardSelector, 
            new Label("Mode:"),
            gameModeSelector,

            startButton,
            randomizeButton,
            restartButton,
            scoreLabel,
            timerLabel
        );
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-padding: 15px; -fx-background-color: #e4baba;");
        root.setTop(topBar);

        boardContainer = new GridPane();
        boardContainer.setAlignment(Pos.CENTER);
        boardContainer.setHgap(8);
        boardContainer.setVgap(8);
        boardContainer.setStyle("-fx-padding: 20px;");
        root.setCenter(boardContainer);

        setupTimer();

        initiateNewGame();

        Scene scene = new Scene(root, 950, 600);
        primaryStage.setTitle("Peg Solitaire");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void handleGameModeChange() {
        gameState.setGameMode(gameModeSelector.getValue());
        if (gameState.isAutomatedMoveAllowed()) {
            scheduleAutomatedMove();
        } else {
            autoMovePause.stop();
            selectedRow = -1;
            selectedCol = -1;
            renderBoard();
        }
        updateRandomizeButtonState();
    }

    private void scheduleAutomatedMove() {
        autoMovePause.stop();

        if (!gameState.isAutomatedMoveAllowed()) {
            return;
        }

        autoMovePause.setOnFinished(event -> {
            PegSolitaireGame.AutomatedTurn turn = gameState.performAutomatedTurn();
            if (turn.moved()) {
                selectedRow = -1;
                selectedCol = -1;
                scoreLabel.setText("Score: " + gameState.getScore());
                renderBoard();
            }

            if (turn.gameOver().isPresent()) {
                handleGameOver(turn.gameOver().get());
            } else if (gameState.isAutomatedMoveAllowed()) {
                scheduleAutomatedMove();
            }
        });

        autoMovePause.playFromStart();
    }

    private void handleRandomizeRequest() {
        if (!gameState.isInProgress() || !gameState.isStarted() ||
            gameModeSelector.getValue() != PegSolitaireGame.GameMode.MANUAL) {
            return;
        }

        if (!gameState.randomizeBoard(new Random())) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(boardContainer.getScene().getWindow());
            alert.setTitle("Cannot Randomize Board");
            alert.setContentText("The board cannot be randomized into a playable position.");
            alert.show();
            return;
        }

        selectedRow = -1;
        selectedCol = -1;
        renderBoard();
    }

    private void updateRandomizeButtonState() {
        if (randomizeButton != null) {
            randomizeButton.setDisable(
                !gameState.isInProgress() || !gameState.isStarted() ||
                gameModeSelector.getValue() != PegSolitaireGame.GameMode.MANUAL
            );
        }
    }


    private void handleBoardSelectionChange() {
        if (gameState.isInProgress()) {
            if (confirmRestart("Change Board Type", "Are you sure you want to change the board?")) {
                initiateNewGame();
            } else {
                // Revert dropdown back to currently loaded board
                boardSelector.setValue(getCurrentBoardType());
            }
        } else {
            initiateNewGame();
        }
    }

    private void handleRestartRequest() {
        if (gameState.isInProgress()) {
            if (confirmRestart("Restart Game", "Are you sure you want to restart?")) {
                gameState.startNewGame(boardSelector.getValue());
                selectedRow = -1;
                selectedCol = -1;
                resetMetrics();
                renderBoard();
            }
        } else {
            initiateNewGame();
        }
    }

    private boolean confirmRestart(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText("Your current progress, score, and timer will be reset.");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * AC 3.1 & 3.2: Resets metrics, loads fresh board array, and renders UI.
     */
    private void initiateNewGame() {
        PegSolitaireGame.BoardType selectedType = boardSelector.getValue();
        if (selectedType == null) return;

        selectedRow = -1;
        selectedCol = -1;

        // AC 3.1: Reset matrix to starting positions
        gameState.startNewGame(selectedType);

        // AC 3.2: Reset metrics
        resetMetrics();

        // Render starting board
        renderBoard();

        updateRandomizeButtonState();
        scheduleAutomatedMove();
    }

    /**
     * AC 3.2: Sets timer and score to 0.
     */
    private void resetMetrics() {
        scoreLabel.setText("Score: " + gameState.getScore());
        timerLabel.setText(String.format("Time: %02d:%02d",
            gameState.getSecondsElapsed() / 60, gameState.getSecondsElapsed() % 60));

        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (startButton != null) {
            startButton.setDisable(false);
        }
        updateRandomizeButtonState();
    }

    private void startGameTimer() {
        if (!gameState.start()) {
            return;
        }

        gameTimer.playFromStart();
        startButton.setDisable(true);
        updateRandomizeButtonState();
        scheduleAutomatedMove();
    }

    private void setupTimer() {
        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            gameState.incrementSecondsElapsed();
            int mins = gameState.getSecondsElapsed() / 60;
            int secs = gameState.getSecondsElapsed() % 60;
            timerLabel.setText(String.format("Time: %02d:%02d", mins, secs));
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
    }

    private PegSolitaireGame.BoardType getCurrentBoardType() {
        return gameState.getBoardType();
    }

    private void renderBoard() {
        boardContainer.getChildren().clear();

        int[][] currentGrid = gameState.getGridSnapshot();
        int rows = currentGrid.length;
        int cols = currentGrid[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cellState = currentGrid[r][c];

                if (cellState == 0) {
                    continue;
                }

                StackPane cellNode = createCellNode(r, c, cellState);
                boardContainer.add(cellNode, c, r);
            }
        }
    }
        private int selectedRow = -1;
        private int selectedCol = -1;

    private StackPane createCellNode(int row, int col, int cellState) {
        StackPane cell = new StackPane();

        Circle hole = new Circle(18);
        hole.setFill(Color.rgb(60, 40, 20));
        hole.setStroke(Color.BLACK);
        cell.getChildren().add(hole);

        if (cellState == 1) {
            Circle peg = new Circle(13);

            if (row == selectedRow && col == selectedCol){
                peg.setFill(Color.GOLD);
                peg.setStroke(Color.ORANGE);
            } else {
            peg.setFill(Color.CRIMSON);
            peg.setStroke(Color.MAROON);
            }

            cell.getChildren().add(peg);
        }
        cell.setOnMouseClicked(e -> handleCellClick(row, col));
        return cell;
    }

    private void handleCellClick(int row, int col) {
        if (!gameState.isStarted()) {
            return;
        }

        int cellState = gameState.getCellState(row, col);

        if (gameModeSelector.getValue() == PegSolitaireGame.GameMode.AUTOMATED) {
            return;
        }

        if (cellState == 1) {
            if (selectedRow == row && selectedCol == col){
                selectedRow = -1;
                selectedCol = -1;
            } else {
                selectedRow = row;
                selectedCol = col;
            }
            renderBoard();
            return;
        }

        if (cellState == 2 && selectedRow != -1 && selectedCol != -1){
            if (gameState.isValidMove(selectedRow, selectedCol, row, col)) {
                executeMove(selectedRow, selectedCol, row, col);
            } else{
                System.out.println("Invalid move");
            }
        }
    }

    private void executeMove(int fromR, int fromC, int toR, int toC) {
        if (!gameState.applyMove(fromR, fromC, toR, toC)) {
            return;
        }

        // Clear Selection State
        selectedRow = -1;
        selectedCol = -1;

        scoreLabel.setText("Score: " + gameState.getScore());

        // Re-render Board UI
        renderBoard();

        checkGameOver();
    }

    private void checkGameOver() {
        if (!gameState.isInProgress()) {
            return;
        }

        Optional<PegSolitaireGame.GameOverResult> result = gameState.getGameOverResult();
        if (result.isEmpty()) {
            return;
        }

        gameState.finish();
        handleGameOver(result.get());
    }

    private void handleGameOver(PegSolitaireGame.GameOverResult gameOver) {
        autoMovePause.stop();
        if (gameTimer != null) {
            gameTimer.stop();
        }
        updateRandomizeButtonState();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(boardContainer.getScene().getWindow());
            ButtonType restartBtn = new ButtonType(
                gameOver.actions().get(0),
                ButtonBar.ButtonData.OK_DONE
            );
            ButtonType exitBtn = new ButtonType(
                gameOver.actions().get(1),
                ButtonBar.ButtonData.CANCEL_CLOSE
            );
            alert.getButtonTypes().setAll(restartBtn, exitBtn);
            alert.setTitle(gameOver.title());
            alert.setContentText(gameOver.message());

            alert.showAndWait().ifPresent(response -> {
                if (response == restartBtn) {
                    initiateNewGame();
                } else if (response == exitBtn) {
                    Platform.exit();
                }
            });
        });
    }

public static void main(String[] args) {
        launch(args);
    }
}