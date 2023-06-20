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

