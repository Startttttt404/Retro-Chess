package dev.huntstew.retrochess;

import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/*
 * Runnable game which handles the game logic and board state.
 */
public class Game implements Runnable{
    /** UI activity */
    private final GameActivity activity;
    /** first player, white */
    private final Player player1;
    /** second player, black */
    private final Player player2;
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
    /** Whether or not the game thread is looking for move input */
    private boolean acceptingMove = false;
    /** Keeps track of the number of moves since something interesting happens (ie. piece capture or pawn movement), after 50 moves from both players, ends the game */
    private int fiftyMoveCounter;

    /**
     * Returns the created game runnable, which ought to be passed into a thread. Uses the activity to update the UI.
     * White player always goes first
     *
     * @param activity a game activity whose UI is to be updated
     * @param whitePlayer the white player, goes first
     * @param blackPlayer the black player, goes second
     */
    public Game(GameActivity activity, Player whitePlayer, Player blackPlayer){
        this.activity = activity;
        winner = new Player(true);
        selectedTile = "None";

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

        // Pieces are added to Player classes. This is to create/use the tile ID from the context of a piece rather than board.
        player1 = whitePlayer;
        player2 = blackPlayer;

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
    }

    /**
     * Begins the game logic, starting with "white" player, taking turns until a winner is declared.
     */
    @Override
    public void run() {
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

            activity.runOnUiThread(() -> Toast.makeText(activity, String.valueOf(fiftyMoveCounter), Toast.LENGTH_LONG).show());
        }

        activity.runOnUiThread(() -> Toast.makeText(activity, "Winner is: " + getWinner().get(), Toast.LENGTH_LONG).show());
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
        board[move.getDestination().charAt(0) - 65][8 - (move.getDestination().charAt(1) - 48)].confirmMove(opponent, move.getDestination(), this); /* Confirmation to set certain properties which should not be set during "fake moves" */
        updateBoard(move);

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
        boolean blackUnwinnable = true;
        if(whiteUnwinnable){
            for(Piece piece: player2.getPieces()){
                if(!getPossibleMoves(piece.getTile()).isEmpty()) {
                    PieceType type = piece.getType();
                    if (whiteTypes.contains(type)) {
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

        return blackUnwinnable && whiteUnwinnable;
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

            // Capture movement
            if (tileIsInBounds(col + 1, row + sign) && getPiece(col + 1, row + sign).isPresent()){
                if(getPiece(col + 1, row + sign).get().isWhite() ^ piece.get().isWhite()){
                    tiles.add(new Move(col, row, col + 1, row + sign, MoveType.NORMAL));
                }
            }
            if (tileIsInBounds(col - 1, row + sign) && getPiece(col - 1, row + sign).isPresent()){
                if(getPiece(col - 1, row + sign).get().isWhite() ^ piece.get().isWhite()){
                    tiles.add(new Move(col, row, col - 1, row + sign, MoveType.NORMAL));
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
                case NORMAL:
                    // "Swapping" the tiles
                    piece.get().setTile(move.getDestination());
                    board[move.getDestination().charAt(0) - 65][8 - (move.getDestination().charAt(1) - 48)] = board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)];
                    board[move.getLocation().charAt(0) - 65][8 - (move.getLocation().charAt(1) - 48)] = new Piece(PieceType.DUMMY, false, null);
                    break;
                case CASTLE:
                    break;
                case PASSANTE:
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
        activity.runOnUiThread(activity::updateBoard);
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
     * Pauses the current thread so the UI thread can update based on a Move
     * @param move the move the board needs to update to
     */
    public void updateBoard(Move move){
        updatingBoard = true;
        getUpdateBarrier().reset();
        activity.runOnUiThread(() -> activity.updateBoard(move));
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
     * Sets the board to updating to prevent game advancement during ui updates
     * @param updatingBoard whether the board is updating or not
     */
    public void setUpdatingBoard(boolean updatingBoard) {
        this.updatingBoard = updatingBoard;
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
     * Gets the UI activity
     * @return the activity
     */
    public GameActivity getActivity() {
        return activity;
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
}
