package dev.huntstew.retrochess.states;

import java.util.List;

public class OverlayState {
    private final List<String> tiles;
    private final boolean clearing;

    public OverlayState(List<String> tiles, boolean clearing){
        this.tiles = tiles;
        this.clearing = clearing;
    }

    public List<String> getTiles() {
        return tiles;
    }

    public boolean isClearing() {
        return clearing;
    }
}
