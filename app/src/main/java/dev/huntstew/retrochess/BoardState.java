package dev.huntstew.retrochess;

public class BoardState {
    private final Piece[][] board;

    /**
     * Returns the created game runnable, which ought to be passed into a thread. Uses the activity to update the UI.
     * White player always goes first
     */
    public BoardState(){
        board = new Piece[8][8];

        // "Clears" the board w/ empties
        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                board[i][j] = new Piece(PieceType.DUMMY, false, null);
            }
        }

        // Black pieces A8-H7
        board[0][0] = new Piece(PieceType.ROOK, false, "A8");
        board[1][0] = new Piece(PieceType.KNIGHT, false, "B8");
        board[2][0] = new Piece(PieceType.W_BISHOP, false, "C8");
        board[3][0] = new Piece(PieceType.QUEEN, false, "D8");
        board[4][0] = new Piece(PieceType.KING, false, "E8");
        board[5][0] = new Piece(PieceType.B_BISHOP, false, "F8");
        board[6][0] = new Piece(PieceType.KNIGHT, false, "G8");
        board[7][0] = new Piece(PieceType.ROOK, false, "H8");

        for(int i = 0; i < board.length; i++){
            board[i][1] = new Piece(PieceType.PAWN, false, (char)(i + 'A') + "7");
        }

        // White pieces A2-H1
        for(int i = 0; i < board.length; i++){
            board[i][6] = new Piece(PieceType.PAWN, true, (char)(i + 'A') + "2");
        }

        board[0][7] = new Piece(PieceType.ROOK, true, "A1");
        board[1][7] = new Piece(PieceType.KNIGHT, true, "B1");
        board[2][7] = new Piece(PieceType.B_BISHOP, true, "C1");
        board[3][7] = new Piece(PieceType.QUEEN, true, "D1");
        board[4][7] = new Piece(PieceType.KING, true, "E1");
        board[5][7] = new Piece(PieceType.W_BISHOP, true, "F1");
        board[6][7] = new Piece(PieceType.KNIGHT, true, "G1");
        board[7][7] = new Piece(PieceType.ROOK, true, "H1");
    }

    public BoardState(Piece[][] board){
        this.board = board;
    }

    public Piece[][] getBoard() {
        return board;
    }
}
