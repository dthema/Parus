package com.example.parus.ui.communication.say;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentManager;

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
import com.example.parus.data.User;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.util.Log.d;

public class SayActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private User user;
    private EditText txt;
    private LinearLayout oftenWordsLayout;
    private LinearLayout collectionsLayout;
    private GridLayout collectionWordsGrid;
    private boolean oftenWordsFlag = false;
    private boolean collectionsFlag = false;
    private boolean collectionWordsFlag = false;
    private ListenerRegistration listenerRegistration;
    private Button[] collectionsButtons;
    private Button lastCollection;
    private Long Column_Count;
    private Double TTS_Speed;
    private Double TTS_Pitch;

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_say);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Говорить");
        }
        Button say = findViewById(R.id.btnSay);
        tts = new TextToSpeech(getApplicationContext(), status -> {
        });
        tts.setLanguage(Locale.getDefault());
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                say.setText("Стоп");
            }

            @Override
            public void onDone(String utteranceId) {
                say.setText("Сказать");
            }

            @Override
            public void onError(String utteranceId) {
                say.setText("Сказать");
            }
        });
        user = new User();
        Button addCollection = findViewById(R.id.btnAddSayCollection);
        Button addWord = findViewById(R.id.btnAddSayWord);
        Button deleteCollection = findViewById(R.id.btnDeleteSayCollection);
        Button deleteWord = findViewById(R.id.btnDeleteSayWord);
        Button show = findViewById(R.id.btnShow);
        txt = findViewById(R.id.editSayText);
        oftenWordsLayout = findViewById(R.id.oftenWordLayout);
        collectionsLayout = findViewById(R.id.linearCollections);
        collectionWordsGrid = findViewById(R.id.gridWords);
        say.setOnClickListener(t ->{
            if (say.getText().toString().equals("Сказать"))
                speak(txt.getText().toString());
            else {
                say.setText("Сказать");
                tts.stop();
            }
        });
        show.setOnClickListener(t -> show(txt.getText().toString()));
        addCollection.setOnClickListener(e ->{
            Button button = null;
            for (Button btn : collectionsButtons){
                if (btn.getPaintFlags() == 1) {
                    button = btn;
                    d("collections_deleteFlag", "btn find = " + btn.getText().toString());
                }
            }
            FragmentManager manager = getSupportFragmentManager();
            user.updateCollection().addOnSuccessListener(l-> {
                        DialogSayAddCollection dialogSayAddCollection = new DialogSayAddCollection();
                        dialogSayAddCollection.show(manager, "DialogCollections");
                    });
            lastCollection = button;
        });
        deleteCollection.setOnClickListener(e ->{
            Button button = null;
            for (Button btn : collectionsButtons){
                if (btn.getPaintFlags() == 1) {
                    button = btn;
                    d("collections_deleteFlag", "btn find = " + btn.getText().toString());
                }
            }
            FragmentManager manager = getSupportFragmentManager();
            user.updateCollection().addOnSuccessListener(l->{
                new DialogSayDeleteCollection();
                DialogSayDeleteCollection dialogSayDeleteCollection = DialogSayDeleteCollection.newInstance(user.getCollectionsString());
                dialogSayDeleteCollection.show(manager, "DialogDeleteCollections");
            });
            lastCollection = button;
        });
        addWord.setOnClickListener(e->{
            Button button = null;
            for (Button btn : collectionsButtons){
                if (btn.getPaintFlags() == 1) {
                    button = btn;
                }
            }
            user.updateCollection().addOnSuccessListener(l-> {
                new DialogSayAddWord();
                DialogSayAddWord dialogSayAddWord = DialogSayAddWord.newInstance(user.getCollectionsString());
                        dialogSayAddWord.show(getSupportFragmentManager(), "DialogAddWord");
                    });
            lastCollection = button;
        });
        deleteWord.setOnClickListener(e ->{
            Button button = null;
            for (Button btn : collectionsButtons){
                if (btn.getPaintFlags() == 1) {
                    button = btn;
                }
            }
            lastCollection = button;
            user.updateCollection().addOnSuccessListener(l-> {
                new DialogSayDeleteWord();
                DialogSayDeleteWord dialogSayDeleteWord = DialogSayDeleteWord.newInstance(user.getCollectionsString(), user);
                        dialogSayDeleteWord.show(getSupportFragmentManager(), "DialogDeleteWord");
                    });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        user.updateOftenWords().addOnSuccessListener(t-> updateOftenWords(user.getWords()));
        user.updateSaySettings().addOnSuccessListener(t->{
            TTS_Speed = (Double) user.getSettings()[0];
            TTS_Pitch = (Double) user.getSettings()[1];
            tts.setSpeechRate(Float.parseFloat(TTS_Speed.toString()));
            tts.setPitch(Float.parseFloat(TTS_Pitch.toString()));
            Column_Count = (Long) user.getSettings()[2];
        });
        // обновление коллекций и фраз
        listenerRegistration = user.getDatabase().collection("users").document(user.getUser().getUid()).collection("Say").document("Collections")
                .addSnapshotListener((documentSnapshot, e) -> user.updateCollection().addOnSuccessListener(l->{
                    updateCollections(user.getCollections());
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
                }));
    }

    @Override
    protected void onPause() {
        if (listenerRegistration != null)
            listenerRegistration.remove();
        Button button = null;
        for (Button btn : collectionsButtons){
            if (btn.getPaintFlags() == 1) {
                button = btn;
            }
        }
        lastCollection = button;
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
        int id = item.getItemId();
        if (id == R.id.settingsButton) {
            user.updateSaySettings().addOnSuccessListener(t->{
                Button button = null;
                for (Button btn : collectionsButtons){
                    if (btn.getPaintFlags() == 1) {
                        button = btn;
                    }
                }
                Button finalButton = button;
                new DialogSayOptions();
                DialogSayOptions dialogSayOptions = DialogSayOptions.newInstance(user);
                dialogSayOptions.show(getSupportFragmentManager(), "DialogSayOptions");
                // обновление настроек
                new Thread(()->{
                    Looper.prepare();
                    while (true){
                        if (dialogSayOptions.isFlag()){
                            user.updateSaySettings().addOnSuccessListener(t1 -> {
                                TTS_Speed = (Double) user.getSettings()[0];
                                TTS_Pitch = (Double) user.getSettings()[1];
                                tts.setSpeechRate(Float.parseFloat(TTS_Speed.toString()));
                                tts.setPitch(Float.parseFloat(TTS_Pitch.toString()));
                                Column_Count = (Long) user.getSettings()[2];
                                if (finalButton != null) {
                                    for (Button btn : collectionsButtons) {
                                        if (btn.getText().toString().equals(finalButton.getText().toString())) {
                                            updateCollectionWords(String.valueOf(finalButton.getText()));
                                        }
                                    }
                                }
                            });
                            break;
                        }
                        if (dialogSayOptions.isClosed()){
                            break;
                        }
                    }
                }).start();
            });
        }
        return super.onOptionsItemSelected(item);
    }

    // показать текст в отдельной активности
    private void show(String text){
        text = text.trim();
        if (!text.equals("")) {
            user.addWord(text);
            Button button = null;
            for (Button btn : collectionsButtons){
                if (btn.getPaintFlags() == 1) {
                    button = btn;
                }
            }
            lastCollection = button;
            Intent intent = new Intent(SayActivity.this, SayShowActivity.class);
            intent.putExtra("word", text);
            startActivity(intent);
        } else
            Toast.makeText(this, "Введите текст для показа", Toast.LENGTH_LONG).show();
    }

    // воспроизвести речь с текстом
    private void speak(String text){
        text = text.trim();
        if (!text.equals("")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "a");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            user.addWord(text);
        }
        updateOftenWords(user.getWords());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void updateOftenWords(List<String> list){
        Log.d("words_board", "start func");
        if (oftenWordsFlag)
            oftenWordsLayout.removeAllViews();
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        for(String str : list){
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
            b.setId(1001010+i);
            b.setBackground(ContextCompat.getDrawable(this, R.drawable.say_word_btn));
            b.setOnClickListener(v -> speak(b.getText().toString()));
            b.setOnLongClickListener(v -> {
                show(b.getText().toString());
                return true;
            });
            buttons.add(b);
        }
        Object[] arr = buttons.toArray();
        if (arr != null) {
            for (int j = list.size() - 1; j >= 0; j--) {
                oftenWordsLayout.addView((View) arr[j]);
            }
            Log.d("words_board", "array no null");
        } else
            Log.d("words_board", "array is null");
    }

    private void updateCollections(List<String> list){
        Log.d("collections_board", "start func");
        if (collectionsFlag) {
            collectionsLayout.removeViews(0, collectionsLayout.getChildCount());
        }
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        collectionsButtons = new Button[list.size()];
        for(String str : list){
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
            b.setId(110101001+i);
            b.setBackgroundColor(Color.rgb(214, 214, 214));
            b.setOnClickListener(v -> {
                updateCollectionWords(String.valueOf(b.getText()));
                b.setBackgroundColor(Color.rgb(105, 161, 255));
                b.setPaintFlags(1);
                for (Button btn : collectionsButtons){
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
        if (arr != null) {
            for (int j = 0; j < list.size(); j++) {
                collectionsLayout.addView((View) arr[j]);
            }
            Log.d("collections_board", "array no null");
        } else
            Log.d("collections_board", "array is null");
    }

    private void updateCollectionWords(String collectionName) {
        Log.d("collectionWords_board", "start func");
        if (collectionWordsFlag) {
            collectionWordsGrid.removeViews(0, collectionWordsGrid.getChildCount());
        }
        if (Build.VERSION.SDK_INT > 20){
            collectionWordsGrid.setColumnCount(Integer.parseInt(Column_Count.toString()));
        } else {
            collectionWordsGrid.setColumnCount(1);
        }
        List<String> list = user.getCollectionWords(collectionName);
        List<Button> buttons = new LinkedList<>();
        int i = 0;
        if (list != null) {
            for (String str : list) {
                collectionWordsFlag = true;
                Button b = new Button(this);
                str = str.trim();
                b.setText(str);
                GridLayout.LayoutParams params =
                        new GridLayout.LayoutParams(collectionWordsGrid.getLayoutParams());
                if (Build.VERSION.SDK_INT < 21) {
                    params.rowSpec = GridLayout.spec(i, 1);
                    params.columnSpec = GridLayout.spec(0, 1);
                    params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.setGravity(Gravity.FILL);
                } else {
                    params.rowSpec = GridLayout.spec(i/Integer.parseInt(Column_Count.toString()), 1);
                    params.columnSpec = GridLayout.spec(i%Integer.parseInt(Column_Count.toString()), 1f);
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
            if (arr != null) {
                for (int j = 0; j < list.size(); j++) {
                    collectionWordsGrid.addView((View) arr[j]);
                }
                Log.d("collectionWords_board", "array no null");
            } else
                Log.d("collectionWords_board", "array is null");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null){
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
