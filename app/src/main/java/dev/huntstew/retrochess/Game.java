package dev.huntstew.retrochess;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import dev.huntstew.retrochess.enums.MoveType;
import dev.huntstew.retrochess.enums.PieceType;
import dev.huntstew.retrochess.states.BoardState;
import dev.huntstew.retrochess.states.OverlayState;

/*
 * Runnable game which handles the game logic and board state.
 */
public class Game extends ViewModel implements Runnable{
    /** first player, white */
    private final Player player1 = new Player("Player 1");
    /** second player, black */
    private final Player player2 = new Player("Player 2");
    /** game board, empties for clear spaces */
    private Piece[][] board;
    /** a stack representing all previous states of the board */
    private final Stack<Piece[][]> boardStack = new Stack<>();
    /** a hashmap assigning translated board states to number of appearances */
    private final HashMap<String, Integer> threeFoldMap = new HashMap<>();
    /** the final winner of the game, empty until there is a winner */
    private Player winner;
    /** the "selected tile" connecting the UI thread to this thread */
    private String selectedTile;
    /** whether the UI thread is currently being updated */
    private boolean updatingBoard;
    /** a barrier that causes game to wait while move is being selected */
    private final CyclicBarrier moveBarrier = new CyclicBarrier(2);
    /** a barrier that causes game to wait while ui is updated */
    private final CyclicBarrier updateBarrier = new CyclicBarrier(2);

    private final CyclicBarrier overlayBarrier = new CyclicBarrier(2);
    /** Whether or not the game thread is looking for move input */
    private boolean acceptingMove = false;
    /** Keeps track of the number of moves since something interesting happens (ie. piece capture or pawn movement), after 50 moves from both players, ends the game */
    private int fiftyMoveCounter;

    private boolean updatingOverlay = false;

    private final MutableLiveData<BoardState> boardState = new MutableLiveData<>(new BoardState());

    private final MutableLiveData<OverlayState> overlayState = new MutableLiveData<>(new OverlayState(List.of(), false));

    private int turn;

    /**
     * Begins the game logic, starting with "white" player, taking turns until a winner is declared.
     */
    @Override
    public void run() {
        winner = new Player(true);
        selectedTile = "None";

        board = Objects.requireNonNull(boardState.getValue()).getBoard();

        for (Piece[] pieces : board) {
            for (int row = 0; row < 2; row++) {
                player2.getPieces().add(pieces[row]);
            }
        }

        for (Piece[] pieces : board) {
            for (int row = 6; row < pieces.length; row++) {
                player1.getPieces().add(pieces[row]);
            }
        }

        fiftyMoveCounter = 0;
        turn = 0;

        Player curPlayer = player1; /* White starts first */
        Player opponent = player2;

        updateBoard();
        while (getWinner().isEmpty()) {
            fiftyMoveCounter++;
            takeTurn(curPlayer, opponent);
            setWinner(winCheck());

            Player temp = opponent;
            opponent = curPlayer;
            curPlayer = temp;
            turn++;
        }
    }

    /**
     * Goes through move selection and win checking for curPlayer, returns the winner, if it exists
     * @param curPlayer The player taking their turn
     * @param opponent The player not taking their turn
     */
    public void takeTurn(Player curPlayer, Player opponent){
        // Move selection
        Move move;
        Set<Move> moves = getAllPossibleMoves(curPlayer, opponent);
        move = curPlayer.getMove(this, moves);
        makeMove(move);
        board[move.getDestination().charAt(0) - 65][8 - (move.getDestination().charAt(1) - 48)].confirmMove(opponent, move, this); /* Confirmation to set certain properties which should not be set during "fake moves" */
        updateBoard();

        moves = getAllPossibleMoves(curPlayer, opponent);
        moves.addAll(getAllPossibleMoves(opponent, curPlayer));
        String moveKey = moves.toString();
        threeFoldMap.merge(moveKey, 1, Integer::sum);
    }

    public Player winCheck(){
        String whiteKingTile = "";
        String blackKingTile = "";

        for(Piece piece: player1.getPieces()){
            if(piece.getType() == PieceType.KING){
                whiteKingTile = piece.getTile();
            }
        }

        for(Piece piece: player2.getPieces()){
            if(piece.getType() == PieceType.KING){
                blackKingTile = piece.getTile();
            }
        }

        Set<Move> whiteMoves = getAllPossibleMoves(player1, player2);

        if(whiteMoves.isEmpty()){
            if(tileIsInCheckBy(whiteKingTile, player2)){
                return player2;
            }
            else{
                return new Player("Stalemate!");
            }
        }

        Set<Move> blackMoves = getAllPossibleMoves(player2, player1);

        if(blackMoves.isEmpty()){
            if(tileIsInCheckBy(blackKingTile, player1)){
                return player1;
            }
            else{
                return new Player("Stalemate!");
            }
        }

        whiteMoves.addAll(blackMoves);
        Integer threeFoldValue = threeFoldMap.get(whiteMoves.toString());
        if(threeFoldValue != null && threeFoldValue >= 3){
            return new Player("Three Fold Repetition!");
        }

        if(isUnwinnable()){
            return new Player("Unwinnable!");
        }

        if(fiftyMoveCounter >= 100){
            return new Player("50-Move Rule!");
        }

        return new Player(true);
    }

    public boolean isUnwinnable(){
        Set<PieceType> whiteTypes = new HashSet<>();
        boolean whiteUnwinnable = true;
        for(Piece piece: player1.getPieces()){
            if(!getPossibleMoves(piece.getTile()).isEmpty()) {
                PieceType type = piece.getType();
                if (whiteTypes.contains(type)) {
                    whiteUnwinnable = false;
                    break;
                } else if (type == PieceType.QUEEN || type == PieceType.ROOK || type == PieceType.PAWN) {
                    whiteUnwinnable = false;
                    break;
                } else if (type == PieceType.B_BISHOP && whiteTypes.contains(PieceType.W_BISHOP)) {
                    whiteUnwinnable = false;
                    break;
                } else if (type == PieceType.W_BISHOP && whiteTypes.contains(PieceType.B_BISHOP)) {
                    whiteUnwinnable = false;
                    break;
                } else if (type == PieceType.KNIGHT && (whiteTypes.contains(PieceType.B_BISHOP) || whiteTypes.contains(PieceType.W_BISHOP))) {
                    whiteUnwinnable = false;
                    break;
                } else {
                    whiteTypes.add(type);
                }
            }
        }

        Set<PieceType> blackTypes = new HashSet<>();
        boolean blackUnwinnable = false;
        if(whiteUnwinnable){
            blackUnwinnable = true;
            for(Piece piece: player2.getPieces()){
                if(!getPossibleMoves(piece.getTile()).isEmpty()) {
                    PieceType type = piece.getType();
                    if (blackTypes.contains(type)) {
                        blackUnwinnable = false;
                        break;
                    } else if (type == PieceType.QUEEN || type == PieceType.ROOK || type == PieceType.PAWN) {
                        blackUnwinnable = false;
                        break;
                    } else if (type == PieceType.B_BISHOP && (blackTypes.contains(PieceType.W_BISHOP) || whiteTypes.contains(PieceType.W_BISHOP))) {
                        blackUnwinnable = false;
                        break;
                    } else if (type == PieceType.W_BISHOP && (blackTypes.contains(PieceType.B_BISHOP) || whiteTypes.contains(PieceType.B_BISHOP))) {
                        blackUnwinnable = false;
                        break;
                    } else if (type == PieceType.KNIGHT && (blackTypes.contains(PieceType.B_BISHOP) || blackTypes.contains(PieceType.W_BISHOP))) {
                        blackUnwinnable = false;
                        break;
                    } else {
                        blackTypes.add(type);
                    }
                }
            }
        }

        return blackUnwinnable;
    }


    /**
     * Gets a set of all possible moves for a given player.
     * There are no requirements on current turn
     * @param player the player to return moves for
     * @return moves a set of all moves, where all moves are of the form [location, destination]
     */
    public Set<Move> getAllPossibleMoves(Player player, Player opponent){
        Set<Move> moves = new TreeSet<>();
        String kingTile = "";

        for(Piece piece: player.getPieces()){
            // Determining where the king is, so moves which check it can later be removed
            if(piece.getType() == PieceType.KING){
                kingTile = piece.getTile();
            }

            moves.addAll(getPossibleMoves(piece.getTile()));
        }

        // Goes through all moves, makes sure they don't check the king
        Iterator<Move> iterator = moves.iterator();
        while(iterator.hasNext()){
            Move move = iterator.next();
            makeMove(move);
            if (move.getLocation().equals(kingTile)) {
                if (tileIsInCheckBy(move.getDestination(), opponent)) {
                    iterator.remove();
                }
            }
            else {
                if (tileIsInCheckBy(kingTile, opponent)){
                    iterator.remove();
                }
            }

            board = boardStack.pop();
            board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)].setTile(move.getLocation());
        }

        return moves;
    }

    /**
     * Gets a set of all possible moves for the piece on a provided tileId
     * @param tileId the tile in String ID form
     * @return a set of all possible moves for the piece on tileId, empty set if there is no piece
     */
    public Set<Move> getPossibleMoves(String tileId){
        // Translates tileID to integer values for board
        int col = tileId.charAt(0) - 'A';
        int row = 8 - (tileId.charAt(1) - '0');

        Set<Move> moves = new TreeSet<>();
        Optional<Piece> piece = getPiece(col, row);

        if(piece.isPresent()){
            switch(piece.get().getType()) {
                case PAWN:
                    moves.addAll(getPawnTiles(col, row));
                    break;
                case KING:
                    moves.addAll(getKingTiles(col, row));
                    break;
                case QUEEN:
                    moves.addAll(getDiagonals(col, row));
                    moves.addAll(getHorizontals(col, row));
                    break;
                case W_BISHOP:
                case B_BISHOP:
                    moves.addAll(getDiagonals(col, row));
                    break;
                case KNIGHT:
                    moves.addAll(getKnightTiles(col, row));
                    break;
                case ROOK:
                    moves.addAll(getHorizontals(col, row));
                    break;
            }
        }

        return moves;
    }

    /**
     * Checks whether or not the tile represented by tileId is in check by player
     * @param tileId the tile to be checked
     * @param player the player who may be checking the tile
     * @return true if the tile is in check, false if it not
     */
    public boolean tileIsInCheckBy(String tileId, Player player){
        for (Piece piece: player.getPieces()){
            Set<Move> moves = getPossibleMoves(piece.getTile());
            for (Move move : moves) {
                if (move.getDestination().equals(tileId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given column and row lies within board bounds
     * @param col the column of the tile
     * @param row the row of the tile
     * @return true if the tile is in bounds, false otherwise
     */
    public boolean tileIsInBounds(int col, int row){
        return row >= 0 && row < board.length && col >=0 && col < board.length;
    }

    /**
     * Gets a set of Move values of all tiles according to pawn movement from the given column and row
     * Only functions if there is a piece on the given tile
     * @param col the column of the tile to check
     * @param row the row of the tile to check
     * @return a set of the tiles
     */
    public Set<Move> getPawnTiles(int col, int row){
        Set<Move> tiles = new TreeSet<>();

        Optional<Piece> piece = getPiece(col, row);

        if(piece.isPresent()) {
            // This determines whether to check up or down on the board
            int sign = 1;
            if (piece.get().isWhite()) {
                sign *= -1;
            }

            // Non-capture, normal movement
            if (tileIsInBounds(col, row + sign) && getPiece(col, row + sign).isEmpty()) {
                tiles.add(new Move(col, row, col, row + sign, MoveType.NORMAL));
                if (!piece.get().hasMoved()) {
                    if (tileIsInBounds(col, row + 2 * sign) && getPiece(col, row + 2 * sign).isEmpty()) {
                        tiles.add(new Move(col, row, col, row + 2 * sign, MoveType.NORMAL));
                    }
                }
            }

            // Normal Capture movements
            if (tileIsInBounds(col + 1, row + sign) && getPiece(col + 1, row + sign).isPresent()){      /* Right Capture */
                if(getPiece(col + 1, row + sign).get().isWhite() ^ piece.get().isWhite()){
                    tiles.add(new Move(col, row, col + 1, row + sign, MoveType.NORMAL));
                }
            }
            if (tileIsInBounds(col - 1, row + sign) && getPiece(col - 1, row + sign).isPresent()){      /* Left Capture */
                if(getPiece(col - 1, row + sign).get().isWhite() ^ piece.get().isWhite()){
                    tiles.add(new Move(col, row, col - 1, row + sign, MoveType.NORMAL));
                }
            }

            // En passante movements
            if (tileIsInBounds(col + 1, row + sign) && tileIsInBounds(col + 1, row) && getPiece(col + 1, row).isPresent()){
                if(getPiece(col + 1, row).get().isWhite() ^ piece.get().isWhite() && getPiece(col + 1, row).get().getTurnMoved().isPresent() && getPiece(col + 1, row).get().getTurnMoved().get() == turn - 1){
                    tiles.add(new Move(col, row, col + 1, row + sign, MoveType.PASSANTE));
                }
            }
            if (tileIsInBounds(col - 1, row + sign) && tileIsInBounds(col - 1, row) && getPiece(col - 1, row).isPresent()){
                if(getPiece(col - 1, row).get().isWhite() ^ piece.get().isWhite() && getPiece(col - 1, row).get().getTurnMoved().isPresent() && getPiece(col - 1, row).get().getTurnMoved().get() == turn - 1){
                    tiles.add(new Move(col, row, col - 1, row + sign, MoveType.PASSANTE));
                }
            }
        }

        return tiles;
    }

    /**
     * Gets a set of Move values of all tiles in diagonals from the given column and row
     * Only functions if there is a piece on the given tile
     * @param col the column of the tile to check
     * @param row the row of the tile to check
     * @return a set of the tiles
     */
    public Set<Move> getDiagonals(int col, int row){
        Set<Move> tiles = new TreeSet<>();

        // Directional sets for up/down left/right
        Set<Integer> leftAndRight = Set.of(1, -1);
        Set<Integer> upAndDown = Set.of(1, -1);

        Optional<Piece> piece = getPiece(col, row);

        if(piece.isPresent()) {
            // All directions on both y and x axis
            for (int directionX : leftAndRight) {
                for (int directionY : upAndDown) {
                    int counter = 1;
                    while (tileIsInBounds(col + counter * directionX, row + counter * directionY) && getPiece(col + counter * directionX, row + counter * directionY).isEmpty()) {
                        tiles.add(new Move(col, row, col + counter * directionX, row + counter * directionY, MoveType.NORMAL));       /* Subtracts counter, as board ID is flipped orientation */
                        counter++;
                    }

                    // Checking if the final tile in each direction is enemy
                    if (tileIsInBounds(col + counter * directionX, row + counter * directionY) && getPiece(col + counter * directionX, row + counter * directionY).isPresent()) {
                        if (getPiece(col + counter * directionX, row + counter * directionY).get().isWhite() ^ piece.get().isWhite()) {
                            tiles.add(new Move(col, row, col + counter * directionX, row + counter * directionY, MoveType.NORMAL));
                        }
                    }
                }
            }
        }

        return tiles;
    }

    /**
     * Gets a set of Move values of all tiles horizontal from the given column and row
     * Only functions if there is a piece on the given tile
     * @param col the column of the tile to check
     * @param row the row of the tile to check
     * @return a set of the tiles
     */
    public Set<Move> getHorizontals(int col, int row){
        Set<Move> tiles = new TreeSet<>();

        Set<Integer> negativeAndPositiveDirections = Set.of(1, -1);

        Optional<Piece> piece = getPiece(col, row);

        if(piece.isPresent()) {
            for (int direction : negativeAndPositiveDirections) {
                // Left and right
                int counter = 1;
                while (tileIsInBounds(col + counter * direction, row) && getPiece(col + counter * direction, row).isEmpty()) {
                    tiles.add(new Move(col, row, col + counter * direction, row, MoveType.NORMAL));
                    counter++;
                }

                // Capture
                if (tileIsInBounds(col + counter * direction, row) && getPiece(col + counter * direction, row).isPresent()) {
                    if (getPiece(col + counter * direction, row).get().isWhite() ^ piece.get().isWhite()) {
                        tiles.add(new Move(col, row, col + counter * direction, row, MoveType.NORMAL));
                    }
                }

                // Up and down
                counter = 1;
                while (tileIsInBounds(col, row + counter * direction) && getPiece(col, row + counter * direction).isEmpty()) {
                    tiles.add(new Move(col, row, col, row + counter * direction, MoveType.NORMAL));
                    counter++;
                }

                // Capture
                if (tileIsInBounds(col, row + counter * direction) && getPiece(col, row + counter * direction).isPresent()) {
                    if (getPiece(col, row + counter * direction).get().isWhite() ^ piece.get().isWhite()) {
                        tiles.add(new Move(col, row, col, row + counter * direction, MoveType.NORMAL));
                    }
                }
            }
        }

        return tiles;
    }

    /**
     * Gets a set of Move values of tiles according to knight movement from the given column and row
     * Only functions if there is a piece on the given tile
     * @param col the column of the tile to check
     * @param row the row of the tile to check
     * @return a set of the tiles
     */
    public Set<Move> getKnightTiles(int col, int row){
        Set<Move> tiles = new TreeSet<>();

        // Directional sets for up/down left/right
        Set<Integer> leftAndRight = Set.of(1, -1);
        Set<Integer> upAndDown = Set.of(1, -1);

        Optional<Piece> piece = getPiece(col, row);

        if(piece.isPresent()) {
            // All directions on both y and x axis
            for (int directionX : leftAndRight) {
                for (int directionY : upAndDown) {
                    // Movement
                    if (tileIsInBounds(col + 2 * directionX, row + directionY) && getPiece(col + 2 * directionX, row + directionY).isEmpty()) {
                        tiles.add(new Move(col, row, col + 2 * directionX, row + directionY, MoveType.NORMAL));
                    }
                    if (tileIsInBounds(col + directionX, row + 2 * directionY) && getPiece(col + directionX, row + 2 * directionY).isEmpty()) {
                        tiles.add(new Move(col, row, col + directionX, row + 2 * directionY, MoveType.NORMAL));
                    }

                    // Captures
                    if (tileIsInBounds(col + 2 * directionX, row + directionY) && getPiece(col + 2 * directionX, row + directionY).isPresent()) {
                        if(getPiece(col + 2 * directionX, row + directionY).get().isWhite() ^ piece.get().isWhite()){
                            tiles.add(new Move(col, row, col + 2 * directionX, row + directionY, MoveType.NORMAL));
                        }
                    }
                    if (tileIsInBounds(col + directionX, row + 2 * directionY) && getPiece(col + directionX, row + 2 * directionY).isPresent()) {
                        if (getPiece(col + directionX, row + 2 * directionY).get().isWhite() ^ piece.get().isWhite()) {
                            tiles.add(new Move(col, row, col + directionX, row + 2 * directionY, MoveType.NORMAL));
                        }
                    }
                }
            }
        }

        return tiles;
    }

    /**
     * Gets a set of Move values in the squared-circle from the given column and row
     * Only functions if there is a piece on the given tile
     * @param col the column of the tile to check
     * @param row the row of the tile to check
     * @return a set of the tiles
     */
    public Set<Move> getKingTiles(int col, int row){
        Set<Move> tiles = new TreeSet<>();

        // Directional sets for up/down left/right
        Set<Integer> leftAndRight = Set.of(1, 0, -1);
        Set<Integer> upAndDown = Set.of(1, 0, -1);

        Optional<Piece> piece = getPiece(col, row);

        if(piece.isPresent()) {
            // All directions on both y and x axis
            for (int directionX : leftAndRight) {
                for (int directionY : upAndDown) {
                    if (tileIsInBounds(col + directionX, row + directionY) && getPiece(col + directionX, row + directionY).isEmpty()) {
                        tiles.add(new Move(col, row,col + directionX, row + directionY, MoveType.NORMAL));
                    }
                    if (tileIsInBounds(col + directionX, row + directionY) && getPiece(col + directionX, row + directionY).isPresent()) {
                        if (getPiece(col + directionX, row + directionY).get().isWhite() ^ piece.get().isWhite()) {
                            tiles.add(new Move(col, row, col + directionX, row + directionY, MoveType.NORMAL));
                        }
                    }
                }
            }

            // Castling
            if(!piece.get().hasMoved()){
                // King side
                if(getPiece(board.length - 1, row).isPresent()){
                    Piece endRowPiece = getPiece(board.length - 1, row).get();
                    if(endRowPiece.getType() == PieceType.ROOK && (endRowPiece.isWhite() == endRowPiece.isWhite()) && !endRowPiece.hasMoved()){
                        if(getPiece(col + 1, row).isEmpty() && getPiece(col + 2, row).isEmpty()) {
                            tiles.add(new Move(col, row, col + 2, row, MoveType.CASTLE));
                        }
                    }
                }

                // Queen side
                if(getPiece(0, row).isPresent()){
                    Piece startRowPiece = getPiece(0, row).get();
                    if(startRowPiece.getType() == PieceType.ROOK && (startRowPiece.isWhite() == startRowPiece.isWhite()) && !startRowPiece.hasMoved()){
                        if(getPiece(col - 1, row).isEmpty() && getPiece(col - 2, row).isEmpty() && getPiece(col - 3, row).isEmpty()) {
                            tiles.add(new Move(col, row, col - 2, row, MoveType.CASTLE));
                        }
                    }

                }
            }
        }

        return tiles;
    }

    /**
     * Rearranges the board according to the features of move
     * @param move the move to use as parameters
     */
    public void makeMove(Move move){
        Optional<Piece> piece = getPiece(move.getLocation().charAt(0) - 'A', 8 - (move.getLocation().charAt(1) - '0'));

        if(piece.isPresent()){
            boardStack.push(board);

            // Need to deep-copy the array, or else all instances on the stack will be identical!
            Piece[][] boardCopy = new Piece[board.length][board[0].length];
            for (int i = 0; i < boardCopy.length; i++) {
                System.arraycopy(board[i], 0, boardCopy[i], 0, boardCopy[i].length);
            }
            board = boardCopy;

            switch (move.getType()) {
                case PASSANTE:
                    if(getPiece(move.getDestinationCol(), move.getLocationRow()).isPresent()){
                        board[move.getDestinationCol()][move.getLocationRow()] = new Piece(PieceType.DUMMY, false, null);

                        piece.get().setTile(move.getDestination());
                        board[move.getDestination().charAt(0) - 65][8 - (move.getDestination().charAt(1) - 48)] = board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)];
                        board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)] = new Piece(PieceType.DUMMY, false, null);
                        break;
                    }
                    else{
                        // En passante should not occur if opponent pawn is not next to moving pawn
                        throw new RuntimeException();
                    }
                case CASTLE:
                    // Kingside castle
                    if(getPiece(board.length - 1, move.getLocationRow()).isPresent() && move.getDestinationCol() > move.getLocationCol()){
                        Piece castle = getPiece(board.length - 1, move.getLocationRow()).get();
                        castle.setTile( (char) (move.getLocationCol() + 1 + 'A') + "" + (char)((8 - move.getLocationRow()) + '0'));
                        board[move.getLocationCol() + 1][move.getLocationRow()] = board[board.length - 1][move.getLocationRow()];
                        board[board.length - 1][move.getLocationRow()] = new Piece(PieceType.DUMMY, false, null);
                    }
                    // Queenside castle
                    else if(getPiece(0, move.getLocationRow()).isPresent()){
                        Piece castle = getPiece(0, move.getLocationRow()).get();
                        castle.setTile( (char) (move.getLocationCol() - 1 + 'A') + "" + (char)((8 - move.getLocationRow()) + '0'));
                        board[move.getLocationCol() - 1][move.getLocationRow()] = board[0][move.getLocationRow()];
                        board[0][move.getLocationRow()] = new Piece(PieceType.DUMMY, false, null);
                    }
                    else{
                        // This should never happen, if castling is supposed to take place, castle should be in position
                        throw new RuntimeException();
                    }

                    piece.get().setTile(move.getDestination());
                    board[move.getDestination().charAt(0) - 65][8 - (move.getDestination().charAt(1) - 48)] = board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)];
                    board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)] = new Piece(PieceType.DUMMY, false, null);
                    break;

                case NORMAL:
                    // "Swapping" the tiles
                    piece.get().setTile(move.getDestination());
                    board[move.getDestination().charAt(0) - 65][8 - (move.getDestination().charAt(1) - 48)] = board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)];
                    board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)] = new Piece(PieceType.DUMMY, false, null);
                    break;
            }
        }
    }

    /**
     * Pauses the current thread so the UI thread can update
     * Update the whole board
     */
    public void updateBoard(){
        updatingBoard = true;
        getUpdateBarrier().reset();
        boardState.postValue(new BoardState(board));
        while (updatingBoard) {
            try {
                getUpdateBarrier().await();
                updatingBoard = false;
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sets the currently selected tile to selectedTile. Passing null results in "no tile" being selected
     * @param tile the tile to be set
     */
    public void setSelectedTile(String tile) {
        if(tile == null){
            this.selectedTile = "None";
        }
        else if(tile.length() == 2){
            // Acceptable range of characters: A-H, 1-8
            if(tile.charAt(0) >= 'A' && tile.charAt(0) <= 'H' && tile.charAt(1) >= '1' && tile.charAt(1) <= '8'){
                this.selectedTile = tile;
            }
        }
    }

    /**
     * Sets the winner, ought to be called only once
     * @param winner the winner of the game
     */
    public void setWinner(Player winner) {
        this.winner = winner;
    }

    /**
     * Gets the piece located on a tile specified by row and column
     * @param col the column of the tile
     * @param row the row of the tile
     * @return the piece at the tile, if it exists
     */
    public Optional<Piece> getPiece(int col, int row){
        Piece piece = board[col][row];
        if(piece.getType() == PieceType.DUMMY){
            return Optional.empty();
        }
        return Optional.of(piece);
    }

    /**
     * Gets the tileId that has most recently been selected by the user
     * @return the tileId of the selected tile, if it exists
     */
    public Optional<String> getSelectedTile() {
        if(selectedTile.equals("None")){
            return Optional.empty();
        }
        return Optional.of(selectedTile);
    }

    /**
     * Gets the board
     * @return the board of pieces
     */
    public Piece[][] getBoard() {
        return board;
    }

    /**
     * Gets the winner
     * @return the winner, if it has been declared
     */
    public Optional<Player> getWinner() {
        if(winner.isDummy()){
            return Optional.empty();
        }
        return Optional.of(winner);
    }

    /**
     * Checks if the board is currently updating
     * @return true if the board is updating, false otherwise
     */
    public boolean isUpdatingBoard() {
        return updatingBoard;
    }

    /**
     * Gets the barrier that prevents game from continuing when a move is being selected
     * @return the barrier
     */
    protected CyclicBarrier getMoveBarrier() {
        return moveBarrier;
    }

    /**
     * Gets the barrier that prevents game from continuing when UI is updated
     * @return the barrier
     */
    protected CyclicBarrier getUpdateBarrier() {
        return updateBarrier;
    }

    /**
     * Gets whether the game thread is currently accepting input
     * @return true if accepting a move, false otherwise
     */
    public boolean isAcceptingMove() {
        return acceptingMove;
    }

    public void setAcceptingMove(boolean acceptingMove) {
        this.acceptingMove = acceptingMove;
    }

    public void resetFiftyMoveCounter(){
        fiftyMoveCounter = 0;
    }

    public MutableLiveData<BoardState> getBoardState() {
        return boardState;
    }

    public MutableLiveData<OverlayState> getOverlayState() {
        return overlayState;
    }

    public void postOverlay(List<String> movesFromFirstSquare, boolean clear){
        overlayState.postValue(new OverlayState(movesFromFirstSquare, clear));
    }

    public boolean isUpdatingOverlay(){
        return updatingOverlay;
    }

    public void setUpdatingOverlay(boolean updatingOverlay) {
        this.updatingOverlay = updatingOverlay;
    }

    public CyclicBarrier getOverlayBarrier() {
        return overlayBarrier;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public int getTurn() {
        return turn;
    }
}
