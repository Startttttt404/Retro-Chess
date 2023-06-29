package dev.huntstew.retrochess;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;

public class GameActivity extends AppCompatActivity {
    private GameFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        fragment = (GameFragment)getSupportFragmentManager().findFragmentById(R.id.gameFragment);
    }

    public void updateBoard() {
        fragment.updateBoard();
    }

    public void updateBoard(Move move) {
        fragment.updateBoard(move);
    }

    public void updateOverlay(List<String> tiles, boolean clear){
        fragment.updateOverlay(tiles, clear);
    }
}