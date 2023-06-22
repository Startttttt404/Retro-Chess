package dev.huntstew.retrochess;

public class Move {
    private final String location;

    private final String destination;

    private final int locationCol;
    private final int locationRow;
    private final int destinationCol;
    private final int destinationRow;

    private final MoveType type;

//    /**
//     * Takes the starting tile and destination tile, and creates the move
//     * Also takes a moveType, which determines how the move is made
//     * @param location the starting tile
//     * @param destination the destination tile
//     * @param type the type of move
//     */
//    public Move(String location, String destination, MoveType type){
//        this.location = location;
//        this.destination = destination;
//        this.type = type;
//    }

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

    public int getDestinationCol() {
        return destinationCol;
    }

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

    public int getLocationCol() {
        return locationCol;
    }

    public int getLocationRow() {
        return locationRow;
    }
}

