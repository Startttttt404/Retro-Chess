package dev.huntstew.retrochess;

import androidx.annotation.NonNull;

/**
 * Represents a move that may or may not happen
 * A "move" is defined as a tile to another tile via some method, moveType
 */
public class Move implements Comparable<Move>{
    /** A tileId string representing the original tile */
    private final String location;
    /** A tileId string representing the destination tile */
    private final String destination;
    /** The column of the location tile */
    private final int locationCol;
    /** The row of the location tile */
    private final int locationRow;
    /** The column of the destination tile */
    private final int destinationCol;
    /** The row of the destination tile */
    private final int destinationRow;
    /** The type of move, determining the way Game.makeMove operates */
    private final MoveType type;

    /**
     * Takes a starting tile (col1, row1) and a destination (col2, row2), translates the tiles to the corresponding Id, and creates the move
     * Also takes a moveType, which determines how the move is made
     * @param col1 the starting tiles column
     * @param row1 the starting tiles row
     * @param col2 the destination's column
     * @param row2 the destination's row
     * @param type the type of move
     */
    public Move(int col1, int row1, int col2, int row2, MoveType type){
        this.location = (char) (col1 + 'A') + "" + (char)((8 - row1) + '0');        /* Subtracts row from 8, as the actual board starts from the bottom, but the representation starts from the top */
        this.locationCol = col1;
        this.locationRow = row1;
        this.destination = (char) (col2 + 'A') + "" + (char)((8 - row2) + '0');
        this.destinationCol = col2;
        this.destinationRow = row2;
        this.type = type;
    }

    /**
     * Gets the type of the move
     * @return the moveType of the move
     */
    public MoveType getType() {
        return type;
    }

    /**
     * Gets the destination tile as a tileId
     * @return the tileId of the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Gets the column of the destination tile
     * @return the column
     */
    public int getDestinationCol() {
        return destinationCol;
    }

    /**
     * Gets teh row of the destination tile
     * @return the row
     */
    public int getDestinationRow() {
        return destinationRow;
    }

    /**
     * Gets the initial tile as a tileId
     * @return the tileId of the initial location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the column of the location tile
     * @return the column
     */
    public int getLocationCol() {
        return locationCol;
    }

    /**
     * Gets the row of the location tile
     * @return the row
     */
    public int getLocationRow() {
        return locationRow;
    }

    @Override
    public int compareTo(Move oMove) {
        String value1 = location + destination;
        String value2 = oMove.getLocation() + oMove.getDestination();
        return value1.compareTo(value2);
    }

    @NonNull
    @Override
    public String toString() {
        return location + destination;
    }
}

