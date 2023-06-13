package dev.huntstew.retrochess;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Game implements Runnable{
    private Optional<String> selectedTile;
    private Optional<Piece>[][] board;
    private Player player1;
    private Player player2;
    private Optional<Piece> winner;

    private GameActivity activity;
    public Game(GameActivity activity){
        this.activity = activity;

        selectedTile = Optional.empty();
        board = new Optional[8][8];

        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                board[i][j] = Optional.empty();
            }
        }

        board[0][0] = Optional.of(new Piece(PieceType.ROOK, false, "A8"));
        board[1][0] = Optional.of(new Piece(PieceType.KNIGHT, false, "B8"));
        board[2][0] = Optional.of(new Piece(PieceType.BISHOP, false, "C8"));
        board[3][0] = Optional.of(new Piece(PieceType.QUEEN, false, "D8"));
        board[4][0] = Optional.of(new Piece(PieceType.KING, false, "E8"));
        board[5][0] = Optional.of(new Piece(PieceType.BISHOP, false, "F8"));
        board[6][0] = Optional.of(new Piece(PieceType.KNIGHT, false, "G8"));
        board[7][0] = Optional.of(new Piece(PieceType.ROOK, false, "H8"));

        for(int i = 0; i < board.length; i++){
            board[i][1] = Optional.of(new Piece(PieceType.PAWN, false, String.valueOf((char)(i + 65)) + "7"));
        }

        for(int i = 0; i < board.length; i++){
            board[i][6] = Optional.of(new Piece(PieceType.PAWN, true, String.valueOf((char)(i + 65)) + "2"));
        }

        board[0][7] = Optional.of(new Piece(PieceType.ROOK, true, "A1"));
        board[1][7] = Optional.of(new Piece(PieceType.KNIGHT, true, "B1"));
        board[2][7] = Optional.of(new Piece(PieceType.BISHOP, true, "C1"));
        board[3][7] = Optional.of(new Piece(PieceType.QUEEN, true, "D1"));
        board[4][7] = Optional.of(new Piece(PieceType.KING, true, "E1"));
        board[5][7] = Optional.of(new Piece(PieceType.BISHOP, true, "F1"));
        board[6][7] = Optional.of(new Piece(PieceType.KNIGHT, true, "G1"));
        board[7][7] = Optional.of(new Piece(PieceType.ROOK, true, "H1"));

        ArrayList<Piece> player1Pieces = new ArrayList<>();
        ArrayList<Piece> player2Pieces = new ArrayList<>();

        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < 2; j++){
                player2Pieces.add(board[i][j].get());
            }
        }

        for(int i = 0; i < board.length; i++){
            for(int j = 6; j < board[i].length; j++){
                player1Pieces.add(board[i][j].get());
            }
        }

        player1 = new Player(player1Pieces);
        player2 = new Player(player2Pieces);
        winner = Optional.empty();
    }

    @Override
    public void run() {
        while(winner.isEmpty()) {
            String[] move;
            move = player1.getMove(this);
            makeMove(move);

            new Handler(Looper.getMainLooper()).post(() -> activity.updateBoard());

            move = player2.getMove(this);
            makeMove(move);

            new Handler(Looper.getMainLooper()).post(() -> activity.updateBoard());
        }
    }

//    public List<String> getValidMoves(String tileId){
//        Piece pieceType;
//        int col = tileId.charAt(0) - 65;
//        int row = 8 - (tileId.charAt(1) - 48);
//
//        if(board[col][row].isPresent()){
//            pieceType = board[col][row].get();
//        }
//        else{
//            return List.of();
//        }
//
//        switch(pieceType){
//            case PAWN:
//                return List.of(String.valueOf((char)(col + 65)) + String.valueOf((char)(56 - row)));
//            case KING:
//                break;
//            case QUEEN:
//                break;
//            case BISHOP:
//                break;
//            case KNIGHT:
//                break;
//            case ROOK:
//                break;
//        }
//    }
//
//    public boolean willNotCheckKing(String[] move){
//
//    }

    public void makeMove(String[] move){
        board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)].get().setTile(move[1]);
        board[move[1].charAt(0) - 65][8 - (move[1].charAt(1) - 48)] = board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)];
        board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)] = Optional.empty();
    }

    public void setSelectedTile(Optional<String> selectedTile) {
        this.selectedTile = selectedTile;
    }

    public Optional<String> getSelectedTile() {
        return selectedTile;
    }

    public Optional<Piece>[][] getBoard() {
        return board;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Optional<Piece> getWinner() {
        return winner;
    }
}
