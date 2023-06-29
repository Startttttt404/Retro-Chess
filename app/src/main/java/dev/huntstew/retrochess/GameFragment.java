package dev.huntstew.retrochess;

import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
public class GameFragment extends Fragment implements View.OnClickListener {
    private static final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0, -1.0f,     0,    0, 255, // green
            0,     0, -1.0f,    0, 255, // blue
            0,     0,     0, 1.0f,   0  // alpha
    };

    private Game game;
    private ImageView[][] viewBoard;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        game.getBoardState().observe(getViewLifecycleOwner(), boardState -> updateBoard());

        game.getOverlayState().observe(getViewLifecycleOwner(), overlayState -> updateOverlay(overlayState.getTiles(), overlayState.isClearing()));

        TableLayout boardLayout = view.findViewById(R.id.boardLayout);
        for(int i = 0; i < boardLayout.getChildCount(); i++){
            TableRow row = (TableRow)boardLayout.getChildAt(i);
            for(int j = 0; j < row.getChildCount(); j++){
                ImageView tile = (ImageView) row.getChildAt(j);
                tile.setOnClickListener(this);
                viewBoard[i][j] = tile;
            }
        }

        if(savedInstanceState == null){
            new Thread(game).start();
        }
        else{
            dirtyUpdate();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        game = new ViewModelProvider(this).get(Game.class);
        viewBoard = new ImageView[8][8];

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onClick(View v) {
        if(game.isAcceptingMove()) {
            game.setSelectedTile(getResources().getResourceEntryName(v.getId()));
            try {
                game.getMoveBarrier().await();
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updateBoard(){
        if (game.isUpdatingBoard()) {
            Piece[][] board = game.getBoard();
            for (int col = 0; col < board.length; col++) {
                for (int row = 0; row < board[col].length; row++) {
                    updateTile(col, row);
                }
            }
            try {
                game.getUpdateBarrier().await();
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void dirtyUpdate(){
        Piece[][] board = game.getBoard();
        for (int col = 0; col < board.length; col++) {
            for (int row = 0; row < board[col].length; row++) {
                updateTile(col, row);
            }
        }
    }

    private void updateTile(int col, int row){
        Optional<Piece> curTile = game.getPiece(col, row);
        ImageView tile = viewBoard[row][col];
        tile.setColorFilter(null);
        if (curTile.isPresent()) {
            if (!curTile.get().isWhite()) {
                tile.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
            }
            switch (curTile.get().getType()) {
                case PAWN:
                    tile.setImageResource(R.drawable.pawn);
                    break;
                case KING:
                    tile.setImageResource(R.drawable.king);
                    break;
                case QUEEN:
                    tile.setImageResource(R.drawable.queen);
                    break;
                case B_BISHOP:
                case W_BISHOP:
                    tile.setImageResource(R.drawable.bishop);
                    break;
                case KNIGHT:
                    tile.setImageResource(R.drawable.knight);
                    break;
                case ROOK:
                    tile.setImageResource(R.drawable.castle);
                    break;
            }
        } else {
            tile.setImageDrawable(null);
        }
    }

    public void updateOverlay(List<String> tiles, boolean clear){
        if(game.isUpdatingOverlay()) {
            if (!clear) {
                for (String tileId : tiles) {
                    ImageView tile = getTileFromId(tileId);
                    tile.setForeground(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.selection)));
                }
            } else {
                for (String tileId : tiles) {
                    ImageView tile = getTileFromId(tileId);
                    tile.setForeground(null);
                }
            }
            try {
                game.getOverlayBarrier().await();
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ImageView getTileFromId(String tileId){
        return viewBoard[8 - (tileId.charAt(1) - 48)][tileId.charAt(0) - 65];
    }
}