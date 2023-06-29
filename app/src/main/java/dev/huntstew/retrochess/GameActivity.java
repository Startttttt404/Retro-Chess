package dev.huntstew.retrochess;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class GameActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ((TextView)findViewById(R.id.Player1Text)).setText("Hunter");
        ((TextView)findViewById(R.id.Player2Text)).setText("Ella");

        ((ImageView)findViewById(R.id.Player1ImageView)).setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.pawn));
        ((ImageView)findViewById(R.id.Player2ImageView)).setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.pawn));
    }
}