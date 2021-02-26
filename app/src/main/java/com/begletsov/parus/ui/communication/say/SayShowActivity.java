package com.begletsov.parus.ui.communication.say;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;

import com.begletsov.parus.R;
import com.begletsov.parus.databinding.ActivitySayShowBinding;

import java.util.Objects;

public class SayShowActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySayShowBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_say_show);
        Intent intent = getIntent();
        String word = intent.getStringExtra("word");
        float min = 500;
        // выбор оптимального размера шрифта
        for (String s : Objects.requireNonNull(word).split(" "))
            switch (s.length()) {
                case 1:
                    binding.showText.setTextSize(300);
                    if (300 < min)
                        min = 300;
                    break;
                case 2:
                    binding.showText.setTextSize(180);
                    if (180 < min)
                        min = 180;
                    break;
                case 3:
                    binding.showText.setTextSize(120);
                    if (120 < min)
                        min = 120;
                    break;
                case 4:
                    binding.showText.setTextSize(90);
                    if (90 < min)
                        min = 90;
                    break;
                case 5:
                    binding.showText.setTextSize(70);
                    if (70 < min)
                        min = 70;
                    break;
                case 6:
                    binding.showText.setTextSize(60);
                    if (60 < min)
                        min = 60;
                    break;
                case 7:
                    binding.showText.setTextSize(50);
                    if (50 < min)
                        min = 50;
                    break;
                case 8:
                    binding.showText.setTextSize(48);
                    if (48 < min)
                        min = 48;
                    break;
                case 9:
                    binding.showText.setTextSize(40);
                    if (40 < min)
                        min = 40;
                    break;
                default:
                    binding.showText.setTextSize(30);
                    if (30 < min)
                        min = 30;
                    break;
        }
        binding.showText.setTextSize(min);
        binding.showText.setText(word);
    }
}