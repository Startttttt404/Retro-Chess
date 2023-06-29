package dev.huntstew.retrochess;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public class Player {
    private String name;
    private List<Piece> pieces;
    private final boolean dummy;

    public Player(String name){
        this.name = name;
        this.dummy = false;
        this.pieces = new ArrayList<>();
    }

    public Player(boolean dummy){
        if(dummy){
            this.dummy = true;
        }
        else{
            this.dummy = false;
            this.pieces = new ArrayList<>();
        }
    }

    public Move getMove(Game game, Set<Move> possibleMoves){
        String firstSquare;

        Move finalMove = null;

        game.setSelectedTile(null);

        while(game.getSelectedTile().isEmpty()) {
            waitForSelection(game);
            boolean isValid = false;
            for (Move move : possibleMoves) {
                if (move.getLocation().equals(game.getSelectedTile().get())) {
                    isValid = true;
                }
            }
            if (!isValid) {
                game.setSelectedTile(null);
            }
        }

        firstSquare = game.getSelectedTile().get();

        List<String> movesFromFirstSquare = new ArrayList<>();
        for(Move move : possibleMoves){
            if(move.getLocation().equals(firstSquare)){
                movesFromFirstSquare.add(move.getDestination());
            }
        }
        updateOverlay(game, movesFromFirstSquare, false);

        game.setSelectedTile(null);

        while(game.getSelectedTile().isEmpty()) {
            waitForSelection(game);
            if (game.getSelectedTile().get().equals(firstSquare)) {
                updateOverlay(game, movesFromFirstSquare, true);
                return getMove(game, possibleMoves);
            }
            boolean isValid = false;
            for (Move move : possibleMoves) {
                if (move.getLocation().equals(firstSquare) && move.getDestination().equals(game.getSelectedTile().get())) {
                    finalMove = move;
                    isValid = true;
                }
            }
            if (!isValid) {
                game.setSelectedTile(null);
            }
        }
        game.getMoveBarrier().reset();
        updateOverlay(game, movesFromFirstSquare, true);
        game.setSelectedTile(null);

        return finalMove;
    }

    public void waitForSelection(Game game){
        game.setAcceptingMove(true);
        game.getMoveBarrier().reset();
        while(game.isAcceptingMove()){
            try {
                game.getMoveBarrier().await();
                game.setAcceptingMove(false);
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateOverlay(Game game, List<String> movesFromFirstSquare, boolean clear){
        game.setUpdatingOverlay(true);
        game.getUpdateBarrier().reset();
        game.postOverlay(movesFromFirstSquare, clear);
        while(game.isUpdatingOverlay()){
            try {
                game.getOverlayBarrier().await();
                game.setUpdatingOverlay(false);
            }catch(InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public boolean isDummy() {
        return dummy;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
