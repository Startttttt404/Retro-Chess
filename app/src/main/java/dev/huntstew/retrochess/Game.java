package dev.huntstew.retrochess;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class Game implements Runnable{
    private Optional<String> selectedTile;
    private Optional<Piece>[][] board;

    Stack<Optional<Piece>[][]> boardStack = new Stack<>();
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
            move = player1.getMove(this, getAllPossibleMoves(player1));
            makeMove(move);
            board[move[1].charAt(0) - 65][8 - (move[1].charAt(1) - 48)].get().confirmMove();

            new Handler(Looper.getMainLooper()).post(() -> activity.updateBoard());

            move = player2.getMove(this, getAllPossibleMoves(player2));
            makeMove(move);
            board[move[1].charAt(0) - 65][8 - (move[1].charAt(1) - 48)].get().confirmMove();

            new Handler(Looper.getMainLooper()).post(() -> activity.updateBoard());
        }
    }

    public List<String[]> getAllPossibleMoves(Player player){
        List<String[]> moves = new ArrayList<>();
        List<String[]> opponentMoves = new ArrayList<>();
        String kingTileWhite = "";
        String kingTileBlack = "";

        for(Piece piece: player1.getPieces()){
            if(piece.getType() == PieceType.KING){
                kingTileWhite = piece.getTile();
            }
            for(String newTile: getPossibleMoves(piece.getTile())){
                moves.add(new String[]{piece.getTile(), newTile});
            }
        }
        for(Piece piece: player2.getPieces()){
            if(piece.getType() == PieceType.KING){
                kingTileBlack = piece.getTile();
            }
            for(String newTile: getPossibleMoves(piece.getTile())){
                opponentMoves.add(new String[]{piece.getTile(), newTile});
            }
        }

        if(player.equals(player2)){
            List<String[]> temp = moves;
            moves = opponentMoves;
            opponentMoves = temp;
            removeKingCheckingMoves(moves, opponentMoves, kingTileBlack);
        }
        else{
            removeKingCheckingMoves(moves, opponentMoves, kingTileWhite);
        }

        return moves;
    }
    public List<String> getPossibleMoves(String tileId){
        Piece piece;
        int col = tileId.charAt(0) - 65;
        int row = 8 - (tileId.charAt(1) - 48);
        List<String> moves = new ArrayList<>();

        if(board[col][row].isPresent()){
            piece = board[col][row].get();

            switch(piece.getType()) {
                case PAWN:
                    int sign = 1;
                    if (!piece.isWhite()) {
                        sign *= -1;
                    }

                    if (!piece.hasMoved()) {
                        if (row - 2 * sign < board.length && row - 2 * sign >= 0 && board[col][row - 2 * sign].isEmpty()) {
                            moves.add(String.valueOf((char) (col + 65)) + String.valueOf((char) (56 - row + 2 * sign)));
                        }
                    }
                    if (row - sign < board.length && row - sign >= 0 && board[col][row - sign].isEmpty()){
                        moves.add(String.valueOf((char) (col + 65)) + String.valueOf((char) (56 - row + sign)));
                    }
                    break;
                case KING:
                    if(row + 1 < board.length && board[col][row + 1].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 65)) + String.valueOf((char) (56 - row - 1)));
                    }
                    if(row - 1 >= 0 && board[col][row - 1].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 65)) + String.valueOf((char) (56 - row + 1)));
                    }
                    if(col + 1 < board.length && board[col + 1][row].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 1 + 65)) + String.valueOf((char) (56 - row)));
                    }
                    if(col - 1 >= 0 && board[col - 1][row].isEmpty()) {
                        moves.add(String.valueOf((char) (col - 1 + 65)) + String.valueOf((char) (56 - row)));
                    }
                    break;
                case QUEEN:
                    addDiagonals(moves, tileId);
                    addHorizontals(moves, tileId);
                    break;
                case BISHOP:
                    addDiagonals(moves, tileId);
                    break;
                case KNIGHT:
                    if(col + 2 < board.length && row + 1 < board.length && board[col + 2][row + 1].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 2 + 65)) + String.valueOf((char) (56 - row - 1)));
                    }
                    if(col + 2 < board.length && row - 1 >= 0 && board[col + 2][row - 1].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 2 + 65)) + String.valueOf((char) (56 - row + 1)));
                    }
                    if(col + 1 < board.length && row + 2 < board.length && board[col + 1][row + 2].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 1 + 65)) + String.valueOf((char) (56 - row - 2)));
                    }
                    if(col + 1 < board.length && row - 2 >= 0 && board[col + 1][row - 2].isEmpty()) {
                        moves.add(String.valueOf((char) (col + 1 + 65)) + String.valueOf((char) (56 - row + 2)));
                    }
                    if(col - 2 >= 0 && row + 1 < board.length && board[col - 2][row + 1].isEmpty()) {
                        moves.add(String.valueOf((char) (col - 2 + 65)) + String.valueOf((char) (56 - row - 1)));
                    }
                    if(col - 2 >= 0 && row - 1 >= 0 && board[col - 2][row - 1].isEmpty()) {
                        moves.add(String.valueOf((char) (col - 2 + 65)) + String.valueOf((char) (56 - row + 1)));
                    }
                    if(col - 1 >= 0 && row + 2 < board.length && board[col - 1][row + 2].isEmpty()) {
                        moves.add(String.valueOf((char) (col - 1 + 65)) + String.valueOf((char) (56 - row - 2)));
                    }
                    if(col - 1 >= 0 && row - 2 >= 0 && board[col - 1][row - 2].isEmpty()) {
                        moves.add(String.valueOf((char) (col - 1 + 65)) + String.valueOf((char) (56 - row + 2)));
                    }
                    break;
                case ROOK:
                    addHorizontals(moves, tileId);
                    break;
            }
            return moves;
        }
        else{
            return List.of();
        }
    }

    public void removeKingCheckingMoves(List<String[]> moves, List<String[]> opponentMoves, String kingTile){
        for(String[] move: moves) {
            if (makeMove(move)) {
                for (String[] opponentMove : opponentMoves) {
                    if (opponentMove[1].equals(kingTile)) {
                        List<String> newPossibleMoves = getPossibleMoves(opponentMove[0]);
                        for(String newPossibleMove: newPossibleMoves) {
                            if (opponentMove[1].equals(newPossibleMove)) {
                                moves.remove(move);
                            }
                        }
                    }
                }
                board = boardStack.pop();
                board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)].get().setTile(move[0]);
            }
        }
    }

    public void addDiagonals(List<String> moves, String tileId){
        int col = tileId.charAt(0) - 65;
        int row = 8 - (tileId.charAt(1) - 48);

        int counter = 1;
        while(col + counter < board.length && row + counter < board.length && board[col + counter][row + counter].isEmpty()){
            moves.add(String.valueOf((char)(col + counter + 65)) + String.valueOf((char)(56 - row - counter)));
            counter++;
        }

        counter = 1;
        while(col + counter < board.length && row - counter >= 0 && board[col + counter][row - counter].isEmpty()){
            moves.add(String.valueOf((char)(col + counter + 65)) + String.valueOf((char)(56 - row + counter)));
            counter++;
        }

        counter = 1;
        while(col - counter >= 0 && row + counter < board.length && board[col - counter][row + counter].isEmpty()){
            moves.add(String.valueOf((char)(col - counter + 65)) + String.valueOf((char)(56 - row - counter)));
            counter++;
        }

        counter = 1;
        while(col - counter >= 0 && row - counter >= 0 && board[col - counter][row - counter].isEmpty()){
            moves.add(String.valueOf((char)(col - counter + 65)) + String.valueOf((char)(56 - row + counter)));
            counter++;
        }
    }

    public void addHorizontals(List<String> moves, String tileId){
        int col = tileId.charAt(0) - 65;
        int row = 8 - (tileId.charAt(1) - 48);

        int counter = 1;
        while(col + counter < board.length && board[col + counter][row].isEmpty()){
            moves.add(String.valueOf((char)(col + counter + 65)) + String.valueOf((char)(56 - row)));
            counter++;
        }

        counter = 1;
        while(row + counter < board.length && board[col][row + counter].isEmpty()){
            moves.add(String.valueOf((char)(col + 65)) + String.valueOf((char)(56 - row - counter)));
            counter++;
        }

        counter = 1;
        while(col - counter >= 0 && board[col - counter][row].isEmpty()){
            moves.add(String.valueOf((char)(col - counter + 65)) + String.valueOf((char)(56 - row)));
            counter++;
        }

        counter = 1;
        while(row - counter >= 0 && board[col][row - counter].isEmpty()){
            moves.add(String.valueOf((char)(col + 65)) + String.valueOf((char)(56 - row + counter)));
            counter++;
        }
    }

    public boolean makeMove(String[] move){
        if(board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)].isPresent()) {
            boardStack.push(board);

            Optional<Piece>[][] boardCopy = new Optional[board.length][board.length];
            for(int i = 0; i < boardCopy.length; i++){
                for(int j = 0; j < boardCopy[i].length; j++){
                    boardCopy[i][j] = board[i][j];
                }
            }
            board = boardCopy;

            board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)].get().setTile(move[1]);
            board[move[1].charAt(0) - 65][8 - (move[1].charAt(1) - 48)] = board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)];
            board[move[0].charAt(0) - 65][8 - (move[0].charAt(1) - 48)] = Optional.empty();
            return true;
        }
        return false;
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

    public GameActivity getActivity() {
        return activity;
    }
}
