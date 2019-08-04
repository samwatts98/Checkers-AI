package checkers;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Optional;

public class Main extends Application {

    private Board board; // The Checkers game object.
    private PlayerStats userColour; // The colour checker the user chose.
    private int difficulty; //The numerical value of the difficulty selected.
    private boolean firstAttack; // global variable of whether forced attack possible
    private boolean continuedAttack; //global variable of whether a player can carry on attacking
    private GridPane boardPane; // Where the GUI Board tiles are stored.
    private BorderPane root; // The root Pane for GUI.
    private boolean showHints;
    Button stopTurn;
    private Stage primaryStage;

    private int[] selectedChecker = new int[2]; // Where we store the selected checker to move.

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Checkers");
        root = new BorderPane();
        root.setCenterShape(true);
        showHints = false;

        initialiseDifficultyDialog(); // The difficulty is the maximum depth of minimax AB
        selectColourDialog(); //The players colour choice, red or white.

        board = new Board(difficulty, userColour);
        firstAttack = board.attackableMovePresent(board.getBoard(),board.getCurrentTurn());
        //firstAttack will always initially be false at first due to the nature of starting state. However, this check
        //Was used when testing specific scenarios, and specific plays were the initial state.

        continuedAttack = false; //Always initially false.

        ToolBar toolBar = new ToolBar(); //Where buttons are stored for GUI.
        Button help = new Button("Help"); //Takes the user to the rules of the game website.
        help.setOnMouseClicked(event -> getHostServices().showDocument("http://www.indepthinfo.com/checkers/play.shtml"));

        Button newGame = new Button("New Game"); // Starts a new game.
        newGame.setOnMouseClicked(event -> {
            initialiseDifficultyDialog();
            selectColourDialog();
            board = new Board(difficulty, userColour);
            firstAttack = false;
            continuedAttack = false;
            if (userColour != PlayerStats.b) {
                takeAITurn(board.attackableMovePresent(board.getBoard(),board.getAiPlayer()),false);
                updateBoard();
            } else updateBoard();
        });

        Button toggleHints = new Button("Toggle Hints"); //Toggles possible move locations on and off.
        toggleHints.setOnMouseClicked(event -> {
            showHints = !showHints;
            updateBoard();
        });

        stopTurn = new Button("Give Up Turn"); //Only activated when a user can give up their multi-step move.
        stopTurn.setOnMouseClicked(event -> {
            if (continuedAttack) {
                continuedAttack = false;
                board.changeTurn();
                takeAITurn(board.attackableMovePresent(board.getBoard(),board.getAiPlayer()),false);
                updateBoard();

                if (board.gameOver()) showWinnerDialog();
            }

        });
        stopTurn.setDisable(true); //Initially deactivated, as it's only turned on during continuedAttack.

        toolBar.getItems().addAll(newGame,toggleHints, help, stopTurn);
        root.setTop(toolBar);

        primaryStage.setScene(new Scene(root, Board.TILESIZE * 8, Board.TILESIZE * 8 + 30));
        primaryStage.show();

        if (userColour != PlayerStats.b) { // If the AI is B, they go first.

            takeAITurn(board.attackableMovePresent(board.getBoard(),board.getAiPlayer()),false);
            firstAttack = board.attackableMovePresent(board.getBoard(),board.getCurrentTurn());
            updateBoard();
        } else updateBoard();

    }

    /**
     * Trying different methods to show intermediate moves.
     */
    Task<Void> waitToShowSteps = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            updateBoard();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            return null;
        }
    };

    /**
     * Update the board GUI representationn.
     */
    private void updateBoard() {
        boardPane = getGUIBoard();
        root.setCenter(boardPane);
        initialiseListeners();
        primaryStage.show();
    }

    /**
     * This procedure deals with the AI's turn.
     * @param firstAttack whether or not they have to attack their first turn.
     * @param continuedAttack whether or not they have to continue attacking from a specific piece.
     */
    private void takeAITurn(boolean firstAttack, boolean continuedAttack) {
        //Gets the best possible move from successor function.
        char[][] bestChoiceForAI;
        //mustAttack = board.attackableMovePresent(board.getBoard(), userColour);
        if (firstAttack) {
            bestChoiceForAI = board.getAIMoveMustAttack(false,false,0,0);
            firstAttack = false; }
           // bestChoiceForAI = board.getAIMoveMustAttack(Character.isUpperCase(board.getBoard()[1][0]), selectedChecker[0], selectedChecker[1]);
        else if (continuedAttack) {
            bestChoiceForAI = board.getAIMoveMustAttack(true,Character.isUpperCase(board.getBoard()[1][0]), selectedChecker[0], selectedChecker[1]);
        }
        else bestChoiceForAI = board.getAIMove();

        //If the AI just performed an attack move..
        if (board.countPlayerTokens(bestChoiceForAI, userColour, false, false) <
                board.countPlayerTokens(board.getBoard(), userColour, false, false)) {

            int[] destination = board.findCoordsOfResultOfMove(board.getBoard(), bestChoiceForAI, board.getAiPlayer());
            continuedAttack = board.attackableMovePresent(bestChoiceForAI, board.getAiPlayer(),
                    Character.isUpperCase(bestChoiceForAI[destination[1]][destination[0]]), destination[0], destination[1]);

            if (continuedAttack) selectedChecker = destination;
            else {
                board.changeTurn();
            }

        } else {
            continuedAttack = false;
            this.continuedAttack = false;
            board.changeTurn();
        }
        board.updateCurrentState(bestChoiceForAI);
        if (continuedAttack) {
            //PauseTransition pause = new PauseTransition(Duration.seconds(1));
            //new Thread(waitToShowSteps).start();
            takeAITurn(false,true);
        }
    }

    /**
     * Click listeners perform the work of the user's turn, when a user clicks down on a piece, that coordinate is stored.
     * When they release, the move is determined.
     */
    private void initialiseListeners() {

        //When a user click and holds the tile with the checker on they wish to move..
        boardPane.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {

            int[] temp = calculateSquareCoOrds(event.getX(), event.getY());
            assert temp != null;
            System.out.println("Click On: " + temp[0] + "," + temp[1]);
            //If its the users turn and they don't have to attack.. We store the co-ordinates.
            if (board.getCurrentTurn() == userColour && !continuedAttack)
                selectedChecker = temp;//calculateSquareCoOrds(event.getSceneX(),event.getSceneY());
        });

        //When the user releases their click..
        boardPane.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            //If it's their turn..
            if (board.getCurrentTurn() == userColour) {
                //We store the result co-ordinates.
                int[] destination = calculateSquareCoOrds(event.getX(), event.getY());
                assert destination != null;
                System.out.println("Click Off: " + destination[0] + "," + destination[1]);


                //If their move is valid..
                if (board.playerMoveValid(board.getBoard(), selectedChecker[0], selectedChecker[1])) {
                    //We create a state representation of the board after they move.
                    char[][] moveState = board.returnStateOfMove(selectedChecker[0], selectedChecker[1], destination[0], destination[1]);

                    //If the user can attack on their first go, they must.
                    if (firstAttack) {
                        //We check to see if the state representation of their move is contained within their possible
                        //states gained from the attack only successor function.
                        if (Board.contains(board.getAllAttackableMoves(board.getBoard(), userColour, Character.isUpperCase(board.getBoard()[selectedChecker[1]][selectedChecker[0]]),
                                selectedChecker[0], selectedChecker[1]), moveState)) {
                            //If it is, we update the current state.
                            System.out.println("all good!");
                            board.updateCurrentState(moveState);

                            //And we set must attack to true if the user can attack again from those co-ordinates. False otherwise.
                            continuedAttack = board.attackableMovePresent(board.getBoard(), userColour, Character.isUpperCase(board.getBoard()[destination[1]][destination[0]]),
                                    destination[0], destination[1]);
                            firstAttack = false;
                            //If they can attack again, their origin selection is stored as the attack destination.
                            if (continuedAttack) {
                                selectedChecker = destination;
                                stopTurn.setDisable(false);
                            }

                            //Otherwise, we change turn.
                            else board.changeTurn();
                            //If their move representation wasn't contained in the attack only successor function, invalid move.
                        } else {
                            showInvalidMove("You must perform an attacking move if one is presented to you.\nSee Hints for assistance.");
                        }
                    }
                    //If the user already attacked, and can again, they must, or forfeit their go.
                    else if (continuedAttack) {
                        //If the move state representation is contained within the attack move only successor function,
                        //It's a legal move.
                        if (Board.contains(board.getAllAttackableMoves(board.getBoard(), board.getCurrentTurn(),
                                board.getBoard()[selectedChecker[1]][selectedChecker[0]] == board.getCurrentTurn().getKingChar(),
                                selectedChecker[0], selectedChecker[1]), moveState)) {
                            System.out.println("all good!");
                            //We update the current state
                            board.updateCurrentState(moveState);

                            //If, after the attack, the same piece can attack again, it's still a continued attack.
                            continuedAttack = board.attackableMovePresent(board.getBoard(), userColour, Character.isUpperCase(board.getBoard()[destination[1]][destination[0]]),
                                    destination[0], destination[1]);

                            //If they can attack, store origin of next move as destination of this move.
                            if (continuedAttack) selectedChecker = destination;

                            //Otherwise, change turn.
                            else {
                                board.changeTurn();
                            }
                        } else {

                            //If the successor function didn't contain this move, we show invalid move message.
                            showInvalidMove("You can only attack from the checker you previously attacked with, or forfeit your turn.\nSee Hints for assistance.");
                        }
                    }
                    //If they don't have to make an attacking move..
                    else {
                        //We check to see if the move is a legal valid play from whether or not it's stored in the successor Function.
                        if (Board.contains(board.successorFunction(board.getCurrentTurn(), board.getBoard(), false), moveState)) {
                            System.out.println("all good!");
                            //Update the current state
                            board.updateCurrentState(moveState);
                            //Change turn.
                            board.changeTurn();
                            //If the successor function didn't contain this move, we show invalid move message.
                        } else {
                            showInvalidMove("You cannot move in this way given current the current board.\nSee Hints for assistance.");
                        }
                    }
                }
                else {
                    showInvalidMove("This was either not your piece or you performed an invalid move!");
                }
                //After all this, we update the board display.
                updateBoard();
                //If it's a game over, show the winner dialog.
                if (board.gameOver()) {
                    continuedAttack = false;
                    firstAttack = false;
                    updateBoard();
                    showWinnerDialog();
                }
            }
            //The AI's turn..
            if (board.getCurrentTurn() == board.getAiPlayer() && !board.gameOver()) {
                takeAITurn(board.attackableMovePresent(board.getBoard(),board.getAiPlayer()),false);

                //After the AI's move, if the user can attack, they must.
                firstAttack = board.attackableMovePresent(board.getBoard(),userColour);
                continuedAttack = false;
                stopTurn.setDisable(true);
                updateBoard();

                if (board.gameOver()) {
                    showWinnerDialog();
                    //FreezeGame
                }
            }
        });
    }

    private void showInvalidMove(String message) {
        Alert invalidMove = new Alert(Alert.AlertType.WARNING);
        invalidMove.setHeaderText("Invalid Move!");
        invalidMove.setContentText(message);
        invalidMove.showAndWait();
    }

    private GridPane getGUIBoard() {
        ArrayList<int[]> suggestionLocations;
        if (continuedAttack) {
            suggestionLocations = board.interpretState(board.getAllAttackableMoves(board.getBoard(), userColour,
                    Character.isUpperCase(board.getBoard()[selectedChecker[1]][selectedChecker[0]]), selectedChecker[0], selectedChecker[1])
                    , userColour);
        }
        else if (firstAttack) {
            suggestionLocations = board.interpretState(board.successorFunction(userColour,board.getBoard(),true) , userColour);

        } else{
            suggestionLocations = board.interpretState(board.successorFunction(userColour, board.getBoard(), false), userColour);
        }
        GridPane gridPane = new GridPane();
        board.updateBoardForNewKings();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (board.getBoard()[y][x] == 'X') {
                    gridPane.add(new Rectangle(Board.TILESIZE, Board.TILESIZE, Color.BROWN), x, y);
                } else if (board.getBoard()[y][x] == 'O') {
                    StackPane stackPane = new StackPane();
                    Rectangle rectangle = new Rectangle(Board.TILESIZE, Board.TILESIZE, Color.BLACK);
                    stackPane.getChildren().add(rectangle);
                    if (showHints) {
                        if (deepContains(suggestionLocations, new int[]{x, y})) {
                            Line line1 = new Line(x * Board.TILESIZE + Board.TILESIZE / 3,
                                    y * Board.TILESIZE + Board.TILESIZE / 3,
                                    x * Board.TILESIZE + (Board.TILESIZE / 3 * 2),
                                    y * Board.TILESIZE + (Board.TILESIZE / 3 * 2));
                            line1.setStroke(Color.GREEN);

                            Line line2 = new Line(x * Board.TILESIZE + (Board.TILESIZE / 3 * 2),
                                    y * Board.TILESIZE + Board.TILESIZE / 3,
                                    x * Board.TILESIZE + Board.TILESIZE / 3,
                                    y * Board.TILESIZE + (Board.TILESIZE / 3 * 2));
                            line2.setStroke(Color.GREEN);

                            stackPane.getChildren().addAll(line1, line2);
                        }
                    }

                    gridPane.add(stackPane, x, y);
                } else if (board.getBoard()[y][x] == 'b') {
                    StackPane stackPane = new StackPane();
                    stackPane.getChildren().add(new Rectangle(Board.TILESIZE, Board.TILESIZE, Color.BLACK));
                    stackPane.getChildren().add(new Circle(Board.COUNTERSIZE, PlayerStats.b.getColor()));
                    gridPane.add(stackPane, x, y);
                } else if (board.getBoard()[y][x] == 'w') {
                    StackPane stackPane = new StackPane();
                    stackPane.getChildren().add(new Rectangle(Board.TILESIZE, Board.TILESIZE, Color.BLACK));
                    stackPane.getChildren().add(new Circle(Board.COUNTERSIZE, PlayerStats.w.getColor()));
                    gridPane.add(stackPane, x, y);
                } else if (board.getBoard()[y][x] == 'B') {
                    StackPane stackPane = new StackPane();
                    stackPane.getChildren().add(new Rectangle(Board.TILESIZE, Board.TILESIZE, Color.BLACK));
                    stackPane.getChildren().add(new Circle(Board.COUNTERSIZE, PlayerStats.b.getColor()));
                    stackPane.getChildren().add(new Circle(Board.COUNTERSIZE / 4, Color.BLACK));
                    gridPane.add(stackPane, x, y);
                } else if (board.getBoard()[y][x] == 'W') {
                    StackPane stackPane = new StackPane();
                    stackPane.getChildren().add(new Rectangle(Board.TILESIZE, Board.TILESIZE, Color.BLACK));
                    stackPane.getChildren().add(new Circle(Board.COUNTERSIZE, PlayerStats.w.getColor()));
                    stackPane.getChildren().add(new Circle(Board.COUNTERSIZE / 4, Color.BLACK));
                    gridPane.add(stackPane, x, y);
                }
            }
        }

        return gridPane;
    }

    private int[] calculateSquareCoOrds(double x, double y) {
        int boardX = (int) x / Board.TILESIZE;
        int boardY = (int) y / Board.TILESIZE;
        return (boardX < 8 && boardY < 8 && boardX >= 0 && boardY >= 0) ? new int[]{boardX, boardY} : null;
    }

    private void initialiseDifficultyDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Difficulty");
        alert.setHeaderText("Checkers");
        alert.setContentText("Please select your difficulty.");

        ButtonType easyButton = new ButtonType("Easy");
        ButtonType mediumButton = new ButtonType("Medium");
        ButtonType hardButton = new ButtonType("Hard");

        alert.getButtonTypes().setAll(easyButton, mediumButton, hardButton);

        Optional<ButtonType> option = alert.showAndWait();
        if (option.get() == easyButton) {
            difficulty = 1;
        } else if (option.get() == mediumButton) {
            difficulty = 4;
        } else if (option.get() == hardButton) {
            difficulty = 8;
        } else {
            System.exit(0);
        }
    }

    private void selectColourDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Colour");
        alert.setHeaderText("Checkers");
        alert.setContentText("Please select your Checkers colour.\n" +
                "Red plays first!");

        ButtonType blackButton = new ButtonType("Red");
        ButtonType whiteButton = new ButtonType("White");
        alert.getButtonTypes().setAll(blackButton, whiteButton);
        Optional<ButtonType> option = alert.showAndWait();
        if (option.get() == blackButton) {
            userColour = PlayerStats.b;
        } else if (option.get() == whiteButton) {
            userColour = PlayerStats.w;
        } else {
            System.exit(0);
        }
    }

    private void showWinnerDialog() {
        //board.changeTurn();
        Alert winnerDialog = new Alert(Alert.AlertType.INFORMATION);
        if ((board.hasBlackWon(board.getBoard()) && PlayerStats.b == userColour) ||
                (board.hasWhiteWon(board.getBoard()) && PlayerStats.w == userColour)) {
            winnerDialog.setHeaderText("Congratulations!");
            winnerDialog.setContentText("You Won! Well Done!");
        } else {
            winnerDialog.setContentText("You Lose!");
            winnerDialog.setHeaderText("Loser!");
        }
        winnerDialog.showAndWait();
    }

    private boolean deepContains(ArrayList<int[]> a, int[] b) {
        if (a.isEmpty()) return false;
        if (a == null) return false;
        for (int[] a_ : a) {

            if (a_[0] == b[0] && a_[1] == b[1]) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
