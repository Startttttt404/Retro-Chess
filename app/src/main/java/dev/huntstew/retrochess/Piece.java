package dev.huntstew.retrochess;

import java.util.Optional;

import dev.huntstew.retrochess.enums.MoveType;
import dev.huntstew.retrochess.enums.PieceType;

public class Piece{
    private final PieceType type;
    private final boolean isWhite;
    private int turnMoved;
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

    public Optional<Integer> getTurnMoved() {
        if(turnMoved < 0){
            return Optional.empty();
        }
        else{
            return Optional.of(turnMoved);
        }
    }

    public void confirmMove(Player opponent, Move move, Game game){
        if(getType() == PieceType.PAWN){
            game.resetFiftyMoveCounter();
        }

        for(int i = 0; i < opponent.getPieces().size(); i++){
            if(opponent.getPieces().get(i).getTile().equals(move.getDestination())){
                opponent.getPieces().remove(opponent.getPieces().get(i));
                game.resetFiftyMoveCounter();
            }
            if(move.getType() == MoveType.PASSANTE){
                if(opponent.getPieces().get(i).getTile().equals((char) (move.getDestinationCol() + 'A') + "" + (char)((8 - move.getDestinationRow()) + '0'))){
                    opponent.getPieces().remove(opponent.getPieces().get(i));
                    game.resetFiftyMoveCounter();
                }
            }
        }

        if(!hasMoved) {
            hasMoved = true;
            turnMoved = game.getTurn();
        }
    }
}
