package checkers;

public class BoardAndScore {
    private char[][] state;
    private int score;
    BoardAndScore(char[][] state, int score) {
        this.score = score;
        this.state = state;
    }

    public char[][] getState() {
        return state;
    }

    public int getScore() {
        return score;
    }
}
