package dev.huntstew.retrochess;

public class Move {
    private final String location;

    private final String destination;

    private final MoveType type;

    public Move(String location, String destination, MoveType type){
        this.location = location;
        this.destination = destination;
        this.type = type;
    }

    public Move(int col1, int row1, int col2, int row2, MoveType type){
        this.location = (char) (col1 + 'A') + "" + (char)((8 - row1) + '0');
        this.destination = (char) (col2 + 'A') + "" + (char)((8 - row2) + '0');
        this.type = type;
    }

    public MoveType getType() {
        return type;
    }

    public String getDestination() {
        return destination;
    }

    public String getLocation() {
        return location;
    }
}

