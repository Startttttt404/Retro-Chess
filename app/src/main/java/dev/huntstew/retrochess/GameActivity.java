package dev.huntstew.retrochess;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Optional;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {
    private static final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0, -1.0f,     0,    0, 255, // green
            0,     0, -1.0f,    0, 255, // blue
            0,     0,     0, 1.0f,   0  // alpha
    };

    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        TableLayout boardLayout = findViewById(R.id.boardLayout);
        for(int i = 0; i < boardLayout.getChildCount(); i++){
            TableRow row = (TableRow)boardLayout.getChildAt(i);
            for(int j = 0; j < row.getChildCount(); j++){
                row.getChildAt(j).setOnClickListener(this);
            }
        }

        game = new Game(this);
        updateBoard();
        Thread gameThread = new Thread(game);
        gameThread.start();
    }

    @Override
    public void onClick(View v) {
        synchronized (game){
            game.setSelectedTile(Optional.of(getResources().getResourceEntryName(v.getId())));
            game.notify();
        }
    }

    public void updateBoard(){
        Optional<Piece>[][] board = game.getBoard();
        for(int i = 0; i < board.length; i++){
            for(int j = 0; j < board[i].length; j++){
                Optional<Piece> curTile = board[i][j];
                String tileId = String.valueOf((char)(i + 65)) + String.valueOf((char)(56 - j));
                ImageView tile = findViewById(getResources().getIdentifier(tileId, "id", getPackageName()));
                tile.setColorFilter(null);
                if(curTile.isPresent()){
                    switch(curTile.get()){
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
                    if(game.getPlayer2().getPieces().contains(tileId)){
                        tile.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
                    }
                }
                else{
                    tile.setImageDrawable(null);
                }
            }
        }
    }
}