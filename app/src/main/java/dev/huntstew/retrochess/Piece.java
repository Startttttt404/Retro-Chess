package dev.huntstew.retrochess;

import java.util.Optional;

public class Piece{
    private final PieceType type;
    private final boolean isWhite;
    private boolean hasMoved;
    private String tile;

    public Piece(PieceType type, boolean isWhite, String tile){
        this.type = type;
        this.isWhite = isWhite;
        this.tile = tile;
        hasMoved = false;
    }

    public void setTile(String tile) {
        this.tile = tile;
    }

    public PieceType getType() {
        return type;
    }

    public String getTile() {
        return tile;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void confirmMove(Player opponent, String destination){
        for(int i = 0; i < opponent.getPieces().size(); i++){
            if(opponent.getPieces().get(i).getTile().equals(destination)){
                opponent.getPieces().remove(opponent.getPieces().get(i));
            }
        }
        hasMoved = true;
    }
}
