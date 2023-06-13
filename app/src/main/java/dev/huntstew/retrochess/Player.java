package dev.huntstew.retrochess;

import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Player {
    private List<Piece> pieces;

    public Player(List<Piece> pieces){
        this.pieces = pieces;
    }

    public String[] getMove(Game game, List<String[]> possibleMoves){
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
                for(String[] move: possibleMoves){
                    if(move[0].equals(game.getSelectedTile().get())){
                        isValid = true;
                    }
                }
                if(!isValid){
                    game.setSelectedTile(Optional.empty());
                }
            }
        }

        firstSquare = game.getSelectedTile().get();

        List<String> movesFromFirstSquare = new ArrayList<>();
        for(String[] move : possibleMoves){
            if(move[0].equals(firstSquare)){
                movesFromFirstSquare.add(move[1]);
            }
        }

        new Handler(Looper.getMainLooper()).post(() -> game.getActivity().setOverlay(movesFromFirstSquare));

        game.setSelectedTile(Optional.empty());

        synchronized (game) {
            while (game.getSelectedTile().isEmpty()) {
                try {
                    game.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(game.getSelectedTile().get().equals(firstSquare)){
                    new Handler(Looper.getMainLooper()).post(() -> game.getActivity().clearOverlay(movesFromFirstSquare));
                    game.setSelectedTile(Optional.empty());
                    return getMove(game, possibleMoves);
                }
                boolean isValid = false;
                for(String[] move: possibleMoves){
                    if(move[0].equals(firstSquare) && move[1].equals(game.getSelectedTile().get())){
                        isValid = true;
                    }
                }
                if(!isValid){
                    game.setSelectedTile(Optional.empty());
                }
            }
        }

        new Handler(Looper.getMainLooper()).post(() -> game.getActivity().clearOverlay(movesFromFirstSquare));
        secondSquare = game.getSelectedTile().get();
        game.setSelectedTile(Optional.empty());

        return new String[]{firstSquare, secondSquare};
    }

    public List<Piece> getPieces() {
        return pieces;
    }
}
