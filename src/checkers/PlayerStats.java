package checkers;

import javafx.scene.paint.Color;

public enum PlayerStats {
    w(Color.WHITE, 1,'w','W', 0),
    b(Color.RED,-1,'b','B',7);

     private final Color color; // The colour on the board.
     private final int forwardDirection; // Which direction forwards is on the 2D char array board.
     private final char pawnChar; // char representation of the pawn
     private final char kingChar; // char representation of the king
     private  PlayerStats opponent; // the opponent.
     private final int kingsRow; // what row index is their kings row.
     static {
         w.opponent = PlayerStats.b;
         b.opponent = PlayerStats.w;
     }
     PlayerStats(Color color, int forwardDirection, char pawnChar, char kingChar, int kingsRow) {
        this.color = color;
        this.forwardDirection = forwardDirection;
        this.pawnChar = pawnChar;
        this.kingChar = kingChar;
        this.kingsRow = kingsRow;
    }

    public int getKingsRow() {
        return kingsRow;
    }

    public PlayerStats getOpponent() {
        return opponent;
    }

    public Color getColor() {
        return color;
    }

    public int getForwardDirection() {
        return forwardDirection;
    }

    public char getPawnChar() {
        return pawnChar;
    }

    public char getKingChar() {
        return kingChar;
    }
}
