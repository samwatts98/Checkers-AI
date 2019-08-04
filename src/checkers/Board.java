package checkers;

import java.util.ArrayList;
import java.util.Arrays;
class Board {
    private char[][] board;
    private PlayerStats currentTurn;
    private ArrayList<BoardAndScore> successorEvaluations;
    private PlayerStats humanPlayer;
    private PlayerStats aiPlayer;
    static final int TILESIZE = 100;
    static final int COUNTERSIZE = TILESIZE /4;
    private int difficulty;


    Board(int difficulty, PlayerStats humanPlayer) {

        initialiseBoard();
        this.difficulty = difficulty;
        this.humanPlayer = humanPlayer;
        this.aiPlayer = humanPlayer == PlayerStats.b ? PlayerStats.w : PlayerStats.b;
        this.currentTurn = PlayerStats.b;
    }
    private void initialiseBoard() {
        //Initial State
        board = new char[][]{{'X', 'w', 'X', 'w', 'X', 'w', 'X', 'w'},
                             {'w', 'X', 'w', 'X', 'w', 'X', 'w', 'X'},
                             {'X', 'w', 'X', 'w', 'X', 'w', 'X', 'w'},
                             {'O', 'X', 'O', 'X', 'O', 'X', 'O', 'X'},
                             {'X', 'O', 'X', 'O', 'X', 'O', 'X', 'O'},
                             {'b', 'X', 'b', 'X', 'b', 'X', 'b', 'X'},
                             {'X', 'b', 'X', 'b', 'X', 'b', 'X', 'b'},
                             {'b', 'X', 'b', 'X', 'b', 'X', 'b', 'X'}};

    }

    /**
     * This function calls to perform minimax with AB pruning, prints in command line analysis of moves, and returns
     * a new state representation of the best play available for the AI.
     * @return char[][] state representation of best move.
     */
    char[][] getAIMove() {
        //Remove all previous possible moves..
        successorEvaluations = new ArrayList<>();
        //Perform minimax with alpha beta pruning.
        minimaxAB(board, 0, aiPlayer, Integer.MIN_VALUE, Integer.MAX_VALUE);
        //Print out all the possible moves it could of made with statistics.
        commandLineAnalyseStates();
        return returnBestMove();
    }

    /**
     * This function performs the same task as getAIMove, but for a scenario where the AI must perform an attacking move
     * as a result of killing a user pawn/king and another attack is available for the attacking pawn.
     * @param isKing
     * @param x
     * @param y
     * @return char[][] state represntation of best move
     */
    char[][] getAIMoveMustAttack(boolean specificPiece, boolean isKing, int x, int y) {
        successorEvaluations = new ArrayList<>();
        //Perform minimax with alpha beta pruning.
        minimaxAB(board, 0, aiPlayer, Integer.MIN_VALUE, Integer.MAX_VALUE);
        // Successor function specifically for making a secondary attack at (x,y).
        ArrayList<char[][]> validMoves = new ArrayList<>();
        if (specificPiece) {
            validMoves.addAll(getAllAttackableMoves(board,aiPlayer,isKing,x,y));
        }
        else {
            for (int y_ = 0; y_ < 8; y_++) {
                for (int x_ = 0; x_ < 8; x_++) {
                    boolean king = board[y_][x_] == aiPlayer.getKingChar();
                    validMoves.addAll(getAllAttackableMoves(board,aiPlayer,king,x_,y_));
                }
            }
        }

        //A clone of successorEvaluations for all moves to iterate over.
        ArrayList<BoardAndScore> successorEvaluationsClone = new ArrayList<>(successorEvaluations);
        //For each Board and Score in the clone,
        for(BoardAndScore bs : successorEvaluationsClone) {
            //If the board representation is not a valid attack move in this cercumstance, we remove it.
            if (!contains(validMoves,bs.getState())) {
                successorEvaluations.remove(bs);
            }
        }
        commandLineAnalyseStates();
        return returnBestMove();
    }



    private void commandLineAnalyseStates() {
        System.out.println("My Turn!\nHmmm... Let's have a look here then..\n");
        for (BoardAndScore bs : successorEvaluations) {
            System.out.println("Well, I go can for:\n" + Arrays.deepToString(bs.getState()).replaceAll("],", "]\n") + "\nand I'll score: " + bs.getScore());
            System.out.println("Invulnerable Pieces: " + countCheckersInSafeTile(bs.getState(), getAiPlayer()));
            System.out.println("Vulnerable Pieces: " + countTotalVulnerableCheckers(bs.getState(), getAiPlayer()));
            System.out.println("Opponent Vulnerable Pieces: " + countTotalVulnerableCheckers(bs.getState(), getAiPlayer().getOpponent()));
            System.out.println("Kings On Board: " + countNumberOfKings(bs.getState(), getAiPlayer()));
            System.out.println("Enemy Kings: " + countNumberOfKings(bs.getState(), getAiPlayer().getOpponent()));
            System.out.println("Killed Enemies: " + Math.abs(countNumberOfPawns(bs.getState(), getAiPlayer().getOpponent()) +
                    countNumberOfKings(bs.getState(), getAiPlayer().getOpponent()) - 12));
            System.out.println("\n");
        }
    }

    /**
     * Returns the best possible move for the AI depending on if it's the maximising player or minimising player.
     * @return char[][] state representation of best move for AI.
     */
    private char[][] returnBestMove() {
        int max;
        int best = 0;
        if (aiPlayer == PlayerStats.b) {
            max = Integer.MIN_VALUE;
            for (int i = 0; i < successorEvaluations.size(); ++i) {
                if (max < successorEvaluations.get(i).getScore()) {
                    max = successorEvaluations.get(i).getScore();
                    best = i;
                }
            }
        }
        else {
            max = Integer.MAX_VALUE;
            for (int i = 0; i < successorEvaluations.size(); ++i) {
                if (max > successorEvaluations.get(i).getScore()) {
                    max = successorEvaluations.get(i).getScore();
                    best = i;
                }
            }
        }
        return successorEvaluations.isEmpty() ? null : successorEvaluations.get(best).getState();
    }

    /**
     * Returns a score value of the node given. B is the maximising player, so a good score is positive. W is the minimising
     * player, so a good score is negative.
     * @param node
     * @return
     */
    private int evaluateNode(char[][] node) {
        int score = 0;

        //3 points each for the enemy having vulnerable pieces.
        score = score + countTotalVulnerableCheckers(node, PlayerStats.b.getOpponent())*3;
        score = score - countTotalVulnerableCheckers(node, PlayerStats.w.getOpponent())*3;

        //5 points for each king the player owns.
        score = score + countNumberOfKings(node, PlayerStats.b)*5;
        score = score - countNumberOfKings(node, PlayerStats.w)*5;

        //3 points per enemy checker taken off the board.
        score = score + Math.abs(countNumberOfPawns(node, PlayerStats.b.getOpponent()) +
                countNumberOfKings(node, PlayerStats.b.getOpponent()) - 12) * 3;
        score = score - Math.abs(countNumberOfPawns(node, PlayerStats.w.getOpponent()) +
                countNumberOfKings(node, PlayerStats.w.getOpponent()) - 12) * 3;

        //-1 Score for a vulnerable piece on the board.
        score = score - countTotalVulnerableCheckers(node, PlayerStats.b);
        score = score + countTotalVulnerableCheckers(node, PlayerStats.w);

        //1 point for a checker on an edge tile where they cannot be attacked.
        //I removed this scoring method as it didn't bring much to the performance, and sometimes led to too defensive
        //plays by the AI and increasing it's rate of hiding in a corner moving backwards and forth to waste turns when
        //It's a king.
        //score = score + countCheckersInSafeTile(node, PlayerStats.b);
        //score = score - countCheckersInSafeTile(node, PlayerStats.w);
        return score;
    }

    /**
     * Minimax with Alpha Beta pruning.
     * @param node the state.
     * @param depth initially 0, goes up to difficulty level.
     * @param player the current player
     * @param a alpha value
     * @param b beta value
     * @return
     */
    private int minimaxAB(char[][] node, int depth, PlayerStats player, int a, int b) {

        //If W wins, return a large score
        if (hasWhiteWon(node)) return  -1000;
        //Same for B
        if (hasBlackWon(node)) return 1000;
        //If we reach the maximum depth / difficulty level, we evaluate the node and return the value.
        if (depth == difficulty) return evaluateNode(node);

        //If maximising player..
        if (player == PlayerStats.b) {
            int bestScore = Integer.MIN_VALUE;
            //For each child in successor function..
            for (char[][] child: successorFunction(player, node, false)){
                //Get score of the branch
                int currentScore = minimaxAB(child, depth+1, PlayerStats.w,a,b);
                //If current score is better than previous best score, replace it.
                bestScore = Math.max(bestScore, currentScore);
                //If current score is bigger than current alpha, set alpha to current score
                a = Integer.max(a, currentScore);
                //AB Pruning condition.
                if (a >= b)  break;
                //If we're at depth 0, append to successorEvaluations the state with its score.
                if (depth == 0 ) successorEvaluations.add(new BoardAndScore(child,currentScore));
            }
            return bestScore;
        }
        //If minimising player..
        else {
            int bestScore = Integer.MAX_VALUE;
            for (char[][] child: successorFunction(player, node, false)){
                int currentScore = minimaxAB(child, depth +1, PlayerStats.b,a,b);
                bestScore = Integer.min(bestScore, currentScore);
                b = Integer.min(b, currentScore);
                if (a >= b) break;
                if (depth == 0) successorEvaluations.add(new BoardAndScore(child,currentScore));
            }
            return bestScore;
        }
    }

    /**
     * Successor Function.
     * This returns an ArrayList of state representations for every possible passive and aggressive move available in the
     * state for the given player.
     * @param currentPlayer
     * @param currentState
     * @param attackOnly
     * @return ArrayList of char[][] representing available states.
     */
    ArrayList<char[][]> successorFunction(PlayerStats currentPlayer, char[][] currentState, boolean attackOnly) {

        ArrayList<char[][]> possibleStates = new ArrayList<>();
        //If the user isn't forced to attack..
        if (!attackOnly) {
            //For each row,
            for (int y=0; y < 8; y++) {
                //For each tile in the row..
                for (int x=0; x < 8; x++) {
                    //If the currentPlayer has a piece on this tile..
                    if (currentState[y][x] == currentPlayer.getKingChar() || currentState[y][x] == currentPlayer.getPawnChar()) {
                        boolean isKing = (currentState[y][x] == 'B' || currentState[y][x] == 'W');
                        char[][] temp;
                        //We check if they can move forward right at (x,y)
                        if (canMoveForwardRight(currentState, currentPlayer, x, y)) {
                            //Then we create a state where we move this piece forward and right...
                            temp = deepClone(currentState);
                            temp[y][x] = 'O';
                            /*If the current counter makes it to the oppositions's kings row, it's a king! This method will
                             * update a king if it wasn't a king before, or if it was already a king, will stay a king!
                             */
                            temp[y + currentPlayer.getForwardDirection()][x + 1] = (y + currentPlayer.getForwardDirection() == currentPlayer.getOpponent().getKingsRow())
                                    ? currentPlayer.getKingChar() : currentState[y][x];
                            //Append this state to the arrayList of possible states.
                            possibleStates.add(temp);
                        }
                        if (canMoveBackwardRight(currentState, currentPlayer, isKing, x, y)) {
                            temp = deepClone(currentState);
                            temp[y][x] = 'O';

                            temp[y - currentPlayer.getForwardDirection()][x + 1] = (y - currentPlayer.getForwardDirection() == currentPlayer.getOpponent().getKingsRow())
                                    ? currentPlayer.getKingChar() : currentState[y][x];
                            possibleStates.add(temp);
                        }
                        if (canMoveForwardLeft(currentState, currentPlayer, x, y)) {
                            temp = deepClone(currentState);
                            temp[y][x] = 'O';
                            temp[y + currentPlayer.getForwardDirection()][x - 1] = (y + currentPlayer.getForwardDirection() == currentPlayer.getOpponent().getKingsRow())
                                    ? currentPlayer.getKingChar() : currentState[y][x];

                            possibleStates.add(temp);
                        }
                        if (canMoveBackwardLeft(currentState, currentPlayer, isKing, x, y)) {
                            temp = deepClone(currentState);
                            temp[y][x] = 'O';
                            temp[y - currentPlayer.getForwardDirection()][x - 1] = (y - currentPlayer.getForwardDirection() == currentPlayer.getOpponent().getKingsRow())
                                    ? currentPlayer.getKingChar() : currentState[y][x];

                            possibleStates.add(temp);
                        }

                        //If there is an attack move present here, get all the possible attackmoves.
                        if (attackableMovePresent(currentState, currentPlayer, isKing, x, y)) {

                            possibleStates.addAll(getAllAttackableMoves(currentState, currentPlayer, isKing, x, y));
                        }
                    }


                }
            }
        }
        //If they have to attack, it implies there is an attack move possible, so we do not need to check for one.
        else {
            for (int y=0; y < 8; y++) {
                for (int x=0; x < 8; x++) {
                    boolean isKing = currentState[y][x] == currentPlayer.getKingChar();
                    if (currentState[y][x] == currentPlayer.getKingChar() || currentState[y][x] == currentPlayer.getPawnChar()) {
                        possibleStates.addAll(getAllAttackableMoves(currentState, currentPlayer, isKing, x, y));
                    }

                }
            }
        }
        return possibleStates;
    }

    /**
     * Returns true is at x,y the currentPlayer can attack in any legal direction.
     * @param state
     * @param currentPlayer
     * @param isKing
     * @param x
     * @param y
     * @return  true for can attack, false for cant.
     */
    boolean attackableMovePresent(char[][] state, PlayerStats currentPlayer, boolean isKing, int x, int y) {
        return canAttackForwardLeft(state,currentPlayer,x,y) ||canAttackForwardRight(state,currentPlayer,x,y) ||
                canAttackBackwardLeft(state,currentPlayer,isKing,x,y)|| canAttackBackwardRight(state,currentPlayer,isKing,x,y);
    }

    /**
     * Returns true is currentPlayer can make an attack anywhere on the board.
     * @param state
     * @param currentPlayer
     * @return true if they can attack, false if not.
     */
    boolean attackableMovePresent(char[][] state, PlayerStats currentPlayer) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                boolean isKing = false;
                if (board[y][x] == currentPlayer.getKingChar()) isKing = true;
                if (board[y][x] == currentPlayer.getKingChar() || board[y][x] == currentPlayer.getPawnChar())
                if (canAttackForwardLeft(state,currentPlayer,x,y) ||canAttackForwardRight(state,currentPlayer,x,y) ||
                        canAttackBackwardLeft(state,currentPlayer,isKing,x,y)|| canAttackBackwardRight(state,currentPlayer,isKing,x,y)) return true;
            }
        }
        return false;
    }

    /**
     * Can the current Player move forwards and left.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canMoveForwardLeft(char[][] state, PlayerStats currentPlayer, int x, int y) {
        if (y + currentPlayer.getForwardDirection() <= 7 && y + currentPlayer.getForwardDirection() >= 0 &&
                x - 1 >= 0)
        return state[y + currentPlayer.getForwardDirection()][x - 1] == 'O';
        return false;
    }
    /**
     * Can the current Player move forwards and right.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canMoveForwardRight(char[][] state, PlayerStats currentPlayer, int x, int y){
        try {
        if (y + currentPlayer.getForwardDirection() <= 7 && y + currentPlayer.getForwardDirection() >= 0 &&
                x +1 <=7)
        return state[y + currentPlayer.getForwardDirection()][x + 1] == 'O';

        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Can the current Player move backwards and left.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canMoveBackwardLeft(char[][] state, PlayerStats currentPlayer,boolean isKing, int x, int y){
        try {
            if ((y - currentPlayer.getForwardDirection() <= 7 && y - currentPlayer.getForwardDirection() >= 0 &&
                    x -1 >= 0) && isKing)
            return state[y - currentPlayer.getForwardDirection()][x - 1] == 'O';
        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Can the current Player move backwards and right.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canMoveBackwardRight(char[][] state, PlayerStats currentPlayer,boolean isKing, int x, int y){
        try {
            if ((y - currentPlayer.getForwardDirection() <= 7 && y - currentPlayer.getForwardDirection() >= 0 &&
                    x +1 <=7) && isKing)
            return state[y - currentPlayer.getForwardDirection()][x + 1] == 'O';
        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Can the current Player attack forwards and left.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canAttackForwardLeft(char[][] state, PlayerStats currentPlayer, int x, int y){
        try {
            if (y + currentPlayer.getForwardDirection() * 2 <= 7 && y + currentPlayer.getForwardDirection() * 2 >= 0 &&
                    x - 2 >= 0)
            return ((state[y + currentPlayer.getForwardDirection()][x - 1] == currentPlayer.getOpponent().getPawnChar() ||
                    state[y + currentPlayer.getForwardDirection()][x - 1] == currentPlayer.getOpponent().getKingChar()) &&
                    state[y + currentPlayer.getForwardDirection() * 2][x - 2] == 'O');
        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Can the current Player attack forwards and right.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canAttackForwardRight(char[][] state, PlayerStats currentPlayer, int x, int y) {
        try {
            if (y + currentPlayer.getForwardDirection() * 2 <= 7 && y + currentPlayer.getForwardDirection() * 2 >= 0 &&
                    x + 2 <= 7)
            return ((state[y + currentPlayer.getForwardDirection()][x + 1] == currentPlayer.getOpponent().getPawnChar() ||
                    state[y + currentPlayer.getForwardDirection()][x + 1] == currentPlayer.getOpponent().getKingChar()) &&
                    state[y + currentPlayer.getForwardDirection() * 2][x + 2] == 'O');
        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Can the current Player attack backwards and left.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canAttackBackwardLeft(char[][] state, PlayerStats currentPlayer, boolean isKing, int x, int y) {
        try {
            if ((y - currentPlayer.getForwardDirection() * 2 <= 7 && y - currentPlayer.getForwardDirection() * 2 >= 0 &&
                    x - 2 >= 0) && isKing)
            return ((state[y - currentPlayer.getForwardDirection()][x - 1] == currentPlayer.getOpponent().getPawnChar() ||
                    state[y - currentPlayer.getForwardDirection()][x - 1] == currentPlayer.getOpponent().getKingChar()) &&
                    state[y - currentPlayer.getForwardDirection() * 2][x - 2] == 'O');
        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Can the current Player attack backwards and right.
     * @param state
     * @param currentPlayer
     * @param x
     * @param y
     * @return
     */
    private boolean canAttackBackwardRight(char[][] state, PlayerStats currentPlayer, boolean isKing, int x, int y) {
        try {
            if ((y - currentPlayer.getForwardDirection() * 2 <= 7 && y - currentPlayer.getForwardDirection() * 2 >= 0 &&
                    x + 2 <= 7) && isKing)
            return ((state[y - currentPlayer.getForwardDirection()][x + 1] == currentPlayer.getOpponent().getPawnChar() ||
                    state[y - currentPlayer.getForwardDirection()][x + 1] == currentPlayer.getOpponent().getKingChar()) &&
                    state[y - currentPlayer.getForwardDirection() * 2][x + 2] == 'O');
        } catch (IndexOutOfBoundsException ignored) {}
        return false;
    }

    /**
     * Secondary successor Function for just returning attack moves available at a given tile on the board. Follows the
     * same principles as the original successor functions, just for attack moves.
     * @param node
     * @param currentPlayer
     * @param isKing
     * @param x
     * @param y
     * @return
     */
    ArrayList<char[][]> getAllAttackableMoves(char[][] node, PlayerStats currentPlayer, boolean isKing, int x, int y) {
        ArrayList<char[][]> attackMoves = new ArrayList<>();
        if (!attackableMovePresent(node,currentPlayer,isKing,x,y)){
            return new ArrayList<>();
        }
        else {
            char[][] temp;
            try {
                if (canAttackForwardRight(node,currentPlayer,x,y)) {
                    temp = deepClone(node);
                    temp[y][x] = 'O';
                    temp[y + currentPlayer.getForwardDirection()][x + 1] = 'O';
                    temp[y + currentPlayer.getForwardDirection() * 2][x + 2] = (y + currentPlayer.getForwardDirection() * 2 == currentPlayer.getOpponent().getKingsRow())
                            ? currentPlayer.getKingChar() : node[y][x];
                    attackMoves.add(temp);
                }
            } catch (IndexOutOfBoundsException ignored) {}
            try {
                if (isKing) {
                    if ( canAttackBackwardRight(node,currentPlayer,true,x,y)) {
                        temp = deepClone(node);
                        temp[y][x] = 'O';
                        temp[y - currentPlayer.getForwardDirection()][x + 1] = 'O';
                        temp[y - currentPlayer.getForwardDirection() * 2][x + 2] = (y - currentPlayer.getForwardDirection() * 2 == currentPlayer.getOpponent().getKingsRow())
                                ? currentPlayer.getKingChar() : node[y][x];
                        attackMoves.add(temp);
                    }
                }
            } catch (IndexOutOfBoundsException ignored) {}
            try {
                if (canAttackForwardLeft(node,currentPlayer,x,y)) {
                    temp = deepClone(node);
                    temp[y][x] = 'O';
                    temp[y + currentPlayer.getForwardDirection()][x - 1] = 'O';
                    temp[y + currentPlayer.getForwardDirection() * 2][x - 2] = (y + currentPlayer.getForwardDirection() * 2 == currentPlayer.getOpponent().getKingsRow())
                            ? currentPlayer.getKingChar() : node[y][x];
                    attackMoves.add(temp);
                }
            }catch (IndexOutOfBoundsException ignored) {}
            try {
                if (isKing) {
                    if ( canAttackBackwardLeft(node,currentPlayer,true,x,y)) {
                        temp = deepClone(node);
                        temp[y][x] = 'O';
                        temp[y - currentPlayer.getForwardDirection()][x - 1] = 'O';
                        temp[y - currentPlayer.getForwardDirection() * 2][x - 2] = (y - currentPlayer.getForwardDirection() * 2 == currentPlayer.getOpponent().getKingsRow())
                                ? currentPlayer.getKingChar() : node[y][x];
                        attackMoves.add(temp);
                    }
                }
            } catch (IndexOutOfBoundsException ignored) {}
        }
        return attackMoves;
    }

    /**
     * Terminal state test for B winning.
     * @param node
     * @return
     */
    boolean hasBlackWon(char[][] node) {
        int whiteOnBoard = 0;
        for (int x=0; x<8; x++){
            for (int y=0; y<8;y++) {
                if (node[y][x] == PlayerStats.w.getPawnChar() || node[y][x] == PlayerStats.w.getKingChar()) {
                    whiteOnBoard ++;
                }
            }
        }
        return (whiteOnBoard ==0 || successorFunction(PlayerStats.w,node,false).isEmpty());
    }

    /**
     * Terminal state test for W winning.
     * @param node
     * @return
     */
    boolean hasWhiteWon(char[][] node) {
        int blackOnBoard = 0;
        for (int x=0; x<8; x++){
            for (int y=0; y<8;y++) {
                if (node[y][x] == PlayerStats.b.getPawnChar() || node[y][x] == PlayerStats.b.getKingChar()) {
                    blackOnBoard ++;
                }
            }
        }
        return (blackOnBoard ==0 || successorFunction(PlayerStats.b,node,false).isEmpty());
    }

    /**
     * If the player posseses any checker piece that currently can be attacked the next turn, the function returns true.
     * False otherwise.
     * @param node
     * @param player
     * @param x
     * @param y
     * @return true if can be attacked, false if not.
     */
    private boolean isCheckerVulnerable(char[][] node, PlayerStats player, int x, int y) {
        //First Segment: Is there a space behind and to the left of player, and can an enemy checker move into it
        //Next move by attacking this current Checker.
        if (node[y][x] != player.getKingChar() || node[y][x] != player.getPawnChar()) return false;
        try {
            if (node[y - player.getForwardDirection()][x-1] == 'O' &&
                    (node[y+player.getForwardDirection()][x+1] == player.getOpponent().getKingChar() ||
                            node[y+player.getForwardDirection()][x+1] == player.getOpponent().getPawnChar())) {
                return true;
            }
        } catch (Exception ignored) {}

        try {
            if (node[y - player.getForwardDirection()][x+1] == 'O' &&
                    (node[y+player.getForwardDirection()][x-1] == player.getOpponent().getKingChar() ||
                            node[y+player.getForwardDirection()][x-1] == player.getOpponent().getPawnChar())) {
                return true;
            }
        } catch (Exception ignored) {}

        try {
            if (node[y + player.getForwardDirection()][x-1] == 'O' &&
                    node[y - player.getForwardDirection()][x+1] == player.getOpponent().getKingChar()) {
                return true;
            }
        } catch (Exception ignored) {}

        try {
            if (node[y + player.getForwardDirection()][x + 1] == 'O' &&
                    node[y - player.getForwardDirection()][x - 1] == player.getOpponent().getKingChar()) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * The counts how many vulnerable checkers the player owns in the given node.
     * @param node
     * @param player
     * @return
     */
    private int countTotalVulnerableCheckers(char[][] node, PlayerStats player) {
        int count = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (isCheckerVulnerable(node,player,x,y)) count++;
            }
        }
        return count;
    }

    /**
     * Test to see if this piece is on a border tile, if it is, it can't be attacked and is safe.
     * @param x
     * @param y
     * @return boolean
     */
    private boolean canThisPositionBeAttacked(int x, int y) {
        return (x == 7 || x == 0 || y == 0 || y == 7);
    }

    /**
     * A counter for how many unnattackable pieces the player owns.
     * @param node
     * @param player
     * @return int
     */
    private int countCheckersInSafeTile(char[][] node, PlayerStats player) {
        int count = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (node[y][x] == player.getKingChar() || node[y][x] == player.getPawnChar()){
                    if(canThisPositionBeAttacked(x,y)) count++;
                }
            }
        }
        return count;
    }

    /**
     * A counter for how many kings the player owns in the given node.
     * @param node
     * @param player
     * @return int
     */
    private int countNumberOfKings(char[][] node, PlayerStats player) {
        int count = 0;
        for (int y = 0; y < 8 ; y++) {
            for (int x = 0; x < 8; x++) {
                if (node[y][x] == player.getKingChar()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * A counter for how many pawns the player owns in the given node.
     * @param node
     * @param player
     * @return int
     */
    private int countNumberOfPawns(char[][] node, PlayerStats player) {
        int count = 0;
        for (int y = 0; y < 8 ; y++) {
            for (int x = 0; x < 8; x++) {
                if (node[y][x] == player.getPawnChar()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Updates the current state by replacing pawns on their enemies kings row for kings.
     */
    void updateBoardForNewKings() {
        for (int x = 0; x < 8; x++) {
            if (board[PlayerStats.w.getKingsRow()][x] == 'b') board[PlayerStats.w.getKingsRow()][x] = 'B';
            if (board[PlayerStats.b.getKingsRow()][x] == 'w') board[PlayerStats.w.getKingsRow()][x] = 'W';
        }
    }

    /**
     * Helper function that returns a state representation of moving a piece from its origin to its destination.
     * It caters for attack moves also.
     * @param originX
     * @param originY
     * @param destinationX
     * @param destinationY
     * @return char[][] state representation.
     */
    char[][] returnStateOfMove(int originX, int originY, int destinationX, int destinationY) {
        char[][] newState = deepClone(board);
        char temp = newState[originY][originX];
        //Tile the player moved out from is empty..
        newState[originY][originX] = 'O';
        // Destination co-ordinates set to the player's piece, and updates to king if need be.
        newState[destinationY][destinationX] =
                destinationY == PlayerStats.valueOf(Character.toString(Character.toLowerCase(temp))).getOpponent().getKingsRow()
                        ? Character.toUpperCase(temp) : temp;
        //IF attack move..
        if (Math.abs(originX-destinationX) == 2 && Math.abs(originY-destinationY)==2) {
            if (destinationX - originX > 0 && destinationY - originY > 0) {
                //Upwards and right
                newState[originY+1][originX+1] = 'O';
            }
            else if (destinationX - originX < 0 && destinationY - originY > 0) {
                //Upwards and left
                newState[originY+1][originX-1] = 'O';
            }
            else if (destinationX - originX < 0 && destinationY - originY < 0) {
                //backwards and left
                newState[originY-1][originX-1] = 'O';
            }
            else if (destinationX - originX > 0 && destinationY - originY < 0) {
                //Backwards and right
                newState[originY-1][originX+1] = 'O';
            }
        }
        return newState;
    }

    /**
     * Terminal state test.
     * @return
     */
    boolean gameOver() {
        return hasBlackWon(board) || hasWhiteWon(board);
    }

    /**
     * This is a helper function that returns the co-ordinates of the moved checkers' destination co-ordinates as an
     * integer array of size 2 (x,y). Mainly used to show suggested moves.
     * @param possibleStates
     * @param currentPlayer
     * @return Arraylist of int[2]. (x,y)
     */
    ArrayList<int[]> interpretState(ArrayList<char[][]> possibleStates, PlayerStats currentPlayer) {
        ArrayList<int[]> landingStates = new ArrayList<>();
        for (char[][] state : possibleStates) {
            landingStates.add(findCoordsOfResultOfMove(board,state, currentPlayer));
        }
        return landingStates;
    }

    /**
     * Helper function that returns the result of the moved piece between the start and finish board representations.
     * @param start
     * @param finish
     * @param currentPlayer
     * @return int[2] represententing co-ordinates x,y
     */
    int[] findCoordsOfResultOfMove(char[][] start, char[][] finish, PlayerStats currentPlayer) {
        int[] landingCoordinates = new int[2];

        for (int y = 0; y < 8; y++) {
            for (int x=0; x<8; x++) {
                if (start[y][x] != finish[y][x] && (finish[y][x] == currentPlayer.getPawnChar() || finish[y][x] == currentPlayer.getKingChar())) {
                    landingCoordinates[0] = x;
                    landingCoordinates[1] = y;
                    return landingCoordinates;
                }
            }
        }
        return new int[2];
    }

    /**
     * Helper function for a deep comparision of 2D char arrays. If b matches a state in a, return true.
     * @param a
     * @param b
     * @return boolean
     */
    static boolean contains(ArrayList<char[][]> a, char[][] b) {
        boolean contains = false;
        for (char[][] states: a) {
            boolean match = true;
            for (int i = 0; i < a.get(0).length; i++) {
                match = Arrays.equals(states[i], b[i]);
                if (!match) break;
            }
            contains = match;
            if (contains) break;
        }
        return contains;
    }

    /**
     * Helper function to see if the user's move is valid. Returns a boolean value of the answer.
     * @param state
     * @param originX
     * @param originY
     * @param destX
     * @param destY
     * @param mustAttack
     * @return boolean
     */
    boolean playerMoveValid(char[][] state, int originX, int originY) {

        //If original click is not the player's piece return false
        return state[originY][originX] == humanPlayer.getPawnChar() || state[originY][originX] == humanPlayer.getKingChar();
    }

    /**
     * Counts the number of pawns and kings on the board owned by the player.
     * @param state
     * @param player
     * @param justPawns
     * @param justKings
     * @return int number of checker pieces.
     */
    int countPlayerTokens(char[][] state, PlayerStats player, boolean justPawns, boolean justKings) {
        int count = 0;
        if (justKings && justPawns || (!justKings && !justPawns)) {
            for (int y = 0; y < 8; y ++) {
                for (int x = 0; x < 8; x++) {
                    if (state[y][x] == player.getKingChar() || state[y][x] == player.getPawnChar()) {
                        count++;
                    }
                }
            }
        }
        else if (justPawns) {
            for (int y = 0; y < 8; y ++) {
                for (int x = 0; x < 8; x++) {
                    if (state[y][x] == player.getPawnChar()) {
                        count++;
                    }
                }
            }
        }
        else {
            for (int y = 0; y < 8; y ++) {
                for (int x = 0; x < 8; x++) {
                    if (state[y][x] == player.getKingChar()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    char[][] getBoard() {
        return board;
    }

    PlayerStats getCurrentTurn() {
        return currentTurn;
    }

    ArrayList<BoardAndScore> getSuccessorEvaluations() {
        return successorEvaluations;
    }

    void changeTurn() {
        currentTurn = (currentTurn.equals(humanPlayer)) ? aiPlayer : humanPlayer;
    }

    void updateCurrentState(char[][] state) {
        board = state;
    }


    PlayerStats getAiPlayer() {
        return aiPlayer;
    }

    private char[][] deepClone(char[][] state) {
        char[][] toClone = new char[state.length][];
        for (int i=0; i <state.length; i++) {
            toClone[i] = Arrays.copyOf(state[i], state[i].length);
        }
        return toClone;
    }
}
