package dev.huntstew.retrochess;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public class Player {
    private List<Piece> pieces;
    private final boolean dummy;

    public Player(){
        this.dummy = false;
        this.pieces = new ArrayList<>();
    }
    public Player(List<Piece> pieces){
        this.dummy = false;
        this.pieces = pieces;
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
            game.setSelectedTile(null);
            game.getMoveBarrier().reset();
            try {
                game.getMoveBarrier().await();
                boolean isValid = false;
                for (Move move : possibleMoves) {
                    if (move.getLocation().equals(game.getSelectedTile().get())) {
                        isValid = true;
                    }
                }
                if (!isValid) {
                    game.setSelectedTile(null);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (BrokenBarrierException e) {
                game.getUpdateBarrier().reset();
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
            game.setSelectedTile(null);
            game.getMoveBarrier().reset();
            try {
                game.getMoveBarrier().await();
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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (BrokenBarrierException e) {
                game.getUpdateBarrier().reset();
            }
        }

        updateOverlay(game, movesFromFirstSquare, true);
        game.setSelectedTile(null);
        game.getMoveBarrier().reset();

        return finalMove;
    }

    private void updateOverlay(Game game, List<String> movesFromFirstSquare, boolean clear){
        game.setUpdatingBoard(true);
        game.getActivity().runOnUiThread(() -> game.getActivity().updateOverlay(movesFromFirstSquare, clear));
        while(game.isUpdatingBoard()){
            try {
                game.getUpdateBarrier().await();
            } catch (BrokenBarrierException | InterruptedException e) {
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
}
