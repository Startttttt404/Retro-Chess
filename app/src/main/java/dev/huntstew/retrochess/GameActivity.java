package dev.huntstew.retrochess;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.List;
import java.util.Optional;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {
    private static final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0, -1.0f,     0,    0, 255, // green
            0,     0, -1.0f,    0, 255, // blue
            0,     0,     0, 1.0f,   0  // alpha
    };

    private final Game game = new Game(this, new Player(), new Player());

    private final ImageView[][] viewBoard = new ImageView[game.getBoard().length][game.getBoard().length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        TableLayout boardLayout = findViewById(R.id.boardLayout);
        for(int i = 0; i < boardLayout.getChildCount(); i++){
            TableRow row = (TableRow)boardLayout.getChildAt(i);
            for(int j = 0; j < row.getChildCount(); j++){
                ImageView tile = (ImageView) row.getChildAt(j);
                tile.setOnClickListener(this);
                viewBoard[i][j] = tile;
            }
        }

        updateBoard();
        Thread gameThread = new Thread(game);
        gameThread.start();
    }

    @Override
    public void onClick(View v) {
        synchronized (game){
            game.setSelectedTile(getResources().getResourceEntryName(v.getId()));
            game.notify();
        }
    }

    public void updateBoard(){
        synchronized (game) {
            Piece[][] board = game.getBoard();
            for (int col = 0; col < board.length; col++) {
                for (int row = 0; row < board[col].length; row++) {
                    Optional<Piece> curTile = game.getPiece(col, row);
                    ImageView tile = viewBoard[row][col];
                    tile.setColorFilter(null);
                    if (curTile.isPresent()) {
                        switch (curTile.get().getType()) {
                            case PAWN:
                                tile.setImageResource(R.drawable.chess_plt45);
                                break;
                            case KING:
                                tile.setImageResource(R.drawable.chess_klt45);
                                break;
                            case QUEEN:
                                tile.setImageResource(R.drawable.chess_qlt45);
                                break;
                            case BISHOP:
                                tile.setImageResource(R.drawable.chess_blt45);
                                break;
                            case KNIGHT:
                                tile.setImageResource(R.drawable.chess_nlt45);
                                break;
                            case ROOK:
                                tile.setImageResource(R.drawable.chess_rlt45);
                                break;
                        }
                        if (!curTile.get().isWhite()) {
                            tile.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
                        }
                    } else {
                        tile.setImageDrawable(null);
                    }
                }
            }
            game.setUpdatingBoard(false);
            game.notify();
        }
    }

    public void updateOverlay(List<String> tiles, boolean clear){
        synchronized (game) {
            if(!clear){
                for (String tileId : tiles) {
                    ImageView tile = getTileFromId(tileId);
                    tile.setForeground(new ColorDrawable(getResources().getColor(com.google.android.material.R.color.material_dynamic_neutral20)));
                }
            }
            else{
                for (String tileId : tiles) {
                    ImageView tile = getTileFromId(tileId);
                    tile.setForeground(null);
                }
            }
            game.setUpdatingBoard(false);
            game.notify();
        }
    }

    public ImageView getTileFromId(String tileId){
        return viewBoard[8 - (tileId.charAt(1) - 48)][tileId.charAt(0) - 65];
    }
}