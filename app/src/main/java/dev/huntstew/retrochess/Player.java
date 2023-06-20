package dev.huntstew.retrochess;

import android.graphics.Path;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Player {
    private List<Piece> pieces;
    private boolean dummy;

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

        synchronized (game) {
            while (game.getSelectedTile().isEmpty()) {
                try {
                    game.wait();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                boolean isValid = false;
                for(Move move: possibleMoves){
                    if(move.getLocation().equals(game.getSelectedTile().get())){
                        isValid = true;
                    }
                }
                if(!isValid){
                    game.setSelectedTile(null);
                }
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

        synchronized (game) {
            while (game.getSelectedTile().isEmpty()) {
                try {
                    game.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(game.getSelectedTile().get().equals(firstSquare)){
                    updateOverlay(game, movesFromFirstSquare, true);
                    game.setSelectedTile(null);
                    return getMove(game, possibleMoves);
                }
                boolean isValid = false;
                for(Move move: possibleMoves){
                    if(move.getLocation().equals(firstSquare) && move.getDestination().equals(game.getSelectedTile().get())){
                        finalMove = move;
                        isValid = true;
                    }
                }
                if(!isValid){
                    game.setSelectedTile(null);
                }
            }
        }

        updateOverlay(game, movesFromFirstSquare, true);
        game.setSelectedTile(null);

        return finalMove;
    }

    private void updateOverlay(Game game, List<String> movesFromFirstSquare, boolean clear){
        game.setUpdatingBoard(true);
        game.getActivity().runOnUiThread(() -> game.getActivity().updateOverlay(movesFromFirstSquare, clear));
        synchronized (game) {
            while(game.isUpdatingBoard()){
                try {
                    game.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
