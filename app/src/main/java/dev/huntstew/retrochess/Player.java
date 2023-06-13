package dev.huntstew.retrochess;

import android.graphics.Path;

import java.util.List;
import java.util.Optional;

public class Player {
    private List<String> pieces;

    public Player(List<String> pieces){
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
                if(!pieces.contains(game.getSelectedTile().get())){
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

    public List<String> getPieces() {
        return pieces;
    }
}
