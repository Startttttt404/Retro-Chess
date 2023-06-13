package dev.huntstew.retrochess;

import android.graphics.Path;

import java.util.List;
import java.util.Optional;

public class Player {
    private List<Piece> pieces;

    public Player(List<Piece> pieces){
        this.pieces = pieces;
    }

    public String[] getMove(Game game){
        String firstSquare;
        String secondSquare;

        synchronized (game) {
            while (game.getSelectedTile().isEmpty()) {
                try {
                    game.wait();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                boolean isValid = false;
                for(Piece piece : pieces){
                    if(piece.getTile().equals(game.getSelectedTile().get())){
                        isValid = true;
                    }
                }
                if(!isValid){
                    game.setSelectedTile(Optional.empty());
                }
            }
        }
        firstSquare = game.getSelectedTile().get();
        game.setSelectedTile(Optional.empty());

        synchronized (game) {
            while (game.getSelectedTile().isEmpty()) {
                try {
                    game.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(game.getSelectedTile().get().equals(firstSquare)){
                    return getMove(game);
                }
            }
        }
        secondSquare = game.getSelectedTile().get();
        game.setSelectedTile(Optional.empty());

        return new String[]{firstSquare, secondSquare};
    }

    public List<Piece> getPieces() {
        return pieces;
    }
}
