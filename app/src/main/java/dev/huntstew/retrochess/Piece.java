package dev.huntstew.retrochess;

public class Piece{
    private final PieceType type;
    private final boolean isWhite;
    private String tile;

    public Piece(PieceType type, boolean isWhite, String tile){
        this.type = type;
        this.isWhite = isWhite;
        this.tile = tile;
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
}