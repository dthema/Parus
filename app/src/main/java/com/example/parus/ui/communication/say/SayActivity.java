package com.example.parus.ui.communication.say;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.parus.R;
import com.example.parus.databinding.ActivitySayBinding;
import com.example.parus.databinding.ActivitySeeBinding;
import com.example.parus.viewmodels.SayViewModel;
import com.example.parus.viewmodels.TTSViewModel;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.util.Log.d;
import static android.util.Log.w;

public class SayActivity extends AppCompatActivity {

    private boolean oftenWordsFlag = false;
    private boolean collectionsFlag = false;
    private boolean collectionWordsFlag = false;
    private Button[] collectionsButtons;
    private Button lastCollection;
    private SayViewModel sayViewModel;
    private TTSViewModel TTS;
    private ActivitySayBinding binding;

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_say);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Говорить");
        }
        initViewModels();
        initObservers();
        initDialogs();
        binding.btnSay.setOnClickListener(t -> {
            if (binding.btnSay.getText().toString().equals("Сказать"))
                speak(binding.editSayText.getText().toString());
            else {
                binding.btnSay.setText("Сказать");
                TTS.stopSpeech();
            }
        });
        binding.btnShow.setOnClickListener(t -> show(binding.editSayText.getText().toString()));
    }

    private void initViewModels() {
        TTS = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(TTSViewModel.class);
        sayViewModel = new ViewModelProvider(this).get(SayViewModel.class);
    }

    private void initObservers() {
        TTS.getSayListenLiveData().observe(this, isSaying -> {
            if (isSaying)
                binding.btnSay.setText("Сказать");
            else
                binding.btnSay.setText("Стоп");
        });
        sayViewModel.getOftenWords().observe(this, list -> {
            if (list != null)
                updateOftenWords(list);
        });
        sayViewModel.getCollections().observe(this, list -> {
            if (list == null)
                return;
            updateCollections(list);
            boolean flag = true;
            if (lastCollection != null) {
                for (Button button : collectionsButtons)
                    if (button.getText().toString().equals(lastCollection.getText().toString())) {
                        button.setBackgroundColor(Color.rgb(105, 161, 255));
                        button.setPaintFlags(1);
                        updateCollectionWords(button.getText().toString());
                        flag = false;
                    }
                if (flag)
                    updateCollectionWords(null);
            }
        });
        sayViewModel.getSettingsLiveData().observe(this, arr -> {
            TTS.setSpeed((Double) arr[0]);
            TTS.setPitch((Double) arr[1]);
        });
    }

    private void initDialogs() {
        binding.btnAddSayCollection.setOnClickListener(e -> {
            checkLastButton();
            DialogSayAddCollection dialogSayAddCollection = new DialogSayAddCollection();
            dialogSayAddCollection.show(getSupportFragmentManager(), "DialogCollections");
        });
        binding.btnDeleteSayCollection.setOnClickListener(e -> {
            checkLastButton();
            DialogSayDeleteCollection dialogSayDeleteCollection = new DialogSayDeleteCollection();
            dialogSayDeleteCollection.show(getSupportFragmentManager(), "DialogDeleteCollections");
        });
        binding.btnAddSayWord.setOnClickListener(e -> {
            checkLastButton();
            DialogSayAddWord dialogSayAddWord = new DialogSayAddWord();
            dialogSayAddWord.show(getSupportFragmentManager(), "DialogAddWord");
        });
        binding.btnDeleteSayWord.setOnClickListener(e -> {
            checkLastButton();
            DialogSayDeleteWord dialogSayDeleteWord = new DialogSayDeleteWord();
            dialogSayDeleteWord.show(getSupportFragmentManager(), "DialogDeleteWord");
        });
    }

    @Override
    protected void onPause() {
        TTS.stopSpeech();
        checkLastButton();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        if (Build.VERSION.SDK_INT >= 26)
            menu.getItem(0).setContentDescription("Настройки");
        else
            MenuItemCompat.setContentDescription(menu.getItem(0), "Настройки");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settingsButton) {
            checkLastButton();
            DialogSayOptions dialogSayOptions = new DialogSayOptions();
            dialogSayOptions.show(getSupportFragmentManager(), "DialogSayOptions");
        }
        return super.onOptionsItemSelected(item);
    }

    // показать текст в отдельной активности
    private void show(String text) {
        text = text.trim();
        if (!text.equals("")) {
            checkLastButton();
            sayViewModel.addOftenWord(text);
            Intent intent = new Intent(SayActivity.this, SayShowActivity.class);
            intent.putExtra("word", text);
            startActivity(intent);
        } else
            Toast.makeText(this, "Введите текст для показа", Toast.LENGTH_LONG).show();
    }

    // воспроизвести речь с текстом
    private void speak(String text) {
        text = text.trim();
        if (!text.equals("")) {
            TTS.speak(text);
            sayViewModel.addOftenWord(text);
        }
    }

    private void checkLastButton() {
        if (collectionsButtons != null)
            for (Button btn : collectionsButtons)
                if (btn.getPaintFlags() == 1) {
                    lastCollection = btn;
                    break;
                } else
                    lastCollection = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void updateOftenWords(List<String> list) {
        if (oftenWordsFlag)
            binding.oftenWordLayout.removeAllViews();
        List<Button> buttons = new ArrayList<>();
        int i = 0;
        for (String str : list) {
            oftenWordsFlag = true;
            i++;
            Button b = new Button(this);
            str = str.replaceAll("\n", " ");
            str = str.trim();
            b.setText(str);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            params.rightMargin = 10;
            b.setLayoutParams(params);
            b.setPadding(10, 0, 10, 0);
            b.setTextSize(20f);
            b.setAllCaps(false);
            b.setId(1001010 + i);
            b.setBackground(ContextCompat.getDrawable(this, R.drawable.say_word_btn));
            b.setOnClickListener(v -> speak(b.getText().toString()));
            b.setOnLongClickListener(v -> {
                show(b.getText().toString());
                return true;
            });
            buttons.add(b);
        }
        Object[] arr = buttons.toArray();
        if (arr != null)
            for (int j = list.size() - 1; j >= 0; j--)
                binding.oftenWordLayout.addView((View) arr[j]);
    }

    private void updateCollections(List<String> list) {
        if (collectionsFlag)
            binding.linearCollections.removeViews(0, binding.linearCollections.getChildCount());
        List<Button> buttons = new ArrayList<>();
        int i = 0;
        collectionsButtons = new Button[list.size()];
        for (String str : list) {
            collectionsFlag = true;
            Button b = new Button(this);
            str = str.replaceAll("\n", " ");
            str = str.trim();
            b.setText(str);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            params.bottomMargin = 30;
            params.rightMargin = 20;
            b.setLayoutParams(params);
            b.setTextSize(20f);
            b.setContentDescription("Коллекция " + b.getText());
            b.setAllCaps(false);
            b.setId(110101001 + i);
            b.setBackgroundColor(Color.rgb(214, 214, 214));
            b.setOnClickListener(v -> {
                updateCollectionWords(String.valueOf(b.getText()));
                b.setBackgroundColor(Color.rgb(105, 161, 255));
                b.setPaintFlags(1);
                for (Button btn : collectionsButtons) {
                    if (btn != b) {
                        btn.setBackgroundColor(Color.rgb(214, 214, 214));
                        btn.setPaintFlags(0);
                    }
                }
            });
            buttons.add(b);
            collectionsButtons[i] = b;
            i++;
        }
        Object[] arr = buttons.toArray();
        if (arr != null)
            for (int j = 0; j < list.size(); j++)
                binding.linearCollections.addView((View) arr[j]);
    }

    private void updateCollectionWords(String collectionName) {
        long columns = Long.parseLong(sayViewModel.getSettings()[2].toString());
        int columnCount = (int) columns;
        if (collectionWordsFlag) {
            binding.gridWords.removeViews(0, binding.gridWords.getChildCount());
        }
        if (Build.VERSION.SDK_INT > 20) {
            binding.gridWords.setColumnCount(columnCount);
        } else {
            binding.gridWords.setColumnCount(1);
        }
        List<String> list = sayViewModel.getCollectionWords(collectionName);
        List<Button> buttons = new ArrayList<>();
        int i = 0;
        if (list != null) {
            for (String str : list) {
                collectionWordsFlag = true;
                Button b = new Button(this);
                str = str.trim();
                b.setText(str);
                GridLayout.LayoutParams params =
                        new GridLayout.LayoutParams(binding.gridWords.getLayoutParams());
                if (Build.VERSION.SDK_INT < 21) {
                    params.rowSpec = GridLayout.spec(i, 1);
                    params.columnSpec = GridLayout.spec(0, 1);
                    params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.setGravity(Gravity.FILL);
                } else {
                    params.rowSpec = GridLayout.spec(i / columnCount, 1);
                    params.columnSpec = GridLayout.spec(i % columnCount, 1f);
                    params.width = 0;
                    params.height = GridLayout.LayoutParams.MATCH_PARENT;
                    params.setGravity(Gravity.FILL);
                }
                params.bottomMargin = 30;
                params.rightMargin = 10;
                b.setPadding(10, 0, 10, 0);
                b.setBackground(ContextCompat.getDrawable(this, R.drawable.say_word_btn));
                b.setLayoutParams(params);
                b.setTextSize(20f);
                b.setAllCaps(false);
                b.setId(110101001 + i);
                b.setOnClickListener(v -> speak(b.getText().toString()));
                b.setOnLongClickListener(v -> {
                    show(b.getText().toString());
                    return true;
                });
                buttons.add(b);
                i++;
            }
            Object[] arr = buttons.toArray();
            if (arr != null)
                for (int j = 0; j < list.size(); j++)
                    binding.gridWords.addView((View) arr[j]);
        }
    }
}
