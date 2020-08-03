package com.example.parus.ui.communication.say;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.parus.R;

public class SayShowActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_say_show);
        Intent intent = getIntent();
        String word = intent.getStringExtra("word");
        TextView textView = findViewById(R.id.showText);
        float min = 500;
        // выбор оптимального размера шрифта
        for (String s : word.split(" "))
            switch (s.length()) {
                case 1:
                    textView.setTextSize(300);
                    if (300 < min)
                        min = 300;
                    break;
                case 2:
                    textView.setTextSize(180);
                    if (180 < min)
                        min = 180;
                    break;
                case 3:
                    textView.setTextSize(120);
                    if (120 < min)
                        min = 120;
                    break;
                case 4:
                    textView.setTextSize(90);
                    if (90 < min)
                        min = 90;
                    break;
                case 5:
                    textView.setTextSize(70);
                    if (70 < min)
                        min = 70;
                    break;
                case 6:
                    textView.setTextSize(60);
                    if (60 < min)
                        min = 60;
                    break;
                case 7:
                    textView.setTextSize(50);
                    if (50 < min)
                        min = 50;
                    break;
                case 8:
                    textView.setTextSize(48);
                    if (48 < min)
                        min = 48;
                    break;
                case 9:
                    textView.setTextSize(40);
                    if (40 < min)
                        min = 40;
                    break;
                default:
                    textView.setTextSize(30);
                    if (30 < min)
                        min = 30;
                    break;
        }
        textView.setTextSize(min);
        textView.setText(word);
    }
}