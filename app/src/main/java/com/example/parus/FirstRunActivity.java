package com.example.parus;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.parus.data.User;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Objects;

public class FirstRunActivity extends AppCompatActivity {

    private User user;
    private int cnt = 0;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_run);
        Button btn = findViewById(R.id.firstRunBtn);
        TextView text = findViewById(R.id.firstText);
        EditText editText = findViewById(R.id.firstEditText);
        Button support = findViewById(R.id.firstSupport);
        Button disabled = findViewById(R.id.firstDisabled);
        LinearLayout linearLayout = findViewById(R.id.firstLinear);
        disabled.setOnClickListener(l -> {
            editText.setVisibility(View.VISIBLE);
            btn.setVisibility(View.VISIBLE);
            editText.setHint("ID помощника");
            text.setText("Если ваш помощник уже зарегистрирован, введите его ID.\nИначе оставьте это поле пустым");
            linearLayout.setVisibility(View.GONE);
        });
        support.setOnClickListener(l -> user.update("support", true)
                .addOnSuccessListener(c -> {
                    editText.setVisibility(View.VISIBLE);
                    btn.setVisibility(View.VISIBLE);
                    editText.setHint("ID человека с ОВЗ");
                    text.setText("Если ваш подопечный уже зарегистрирован, введите его ID.\nИначе оставьте это поле пустым");
                    linearLayout.setVisibility(View.GONE);
                })
                .addOnFailureListener(f -> Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_LONG).show()));
        user = new User();
        btn.setOnClickListener(e -> {
            if (cnt == 0) {
                if (editText.getText().toString().length() > 0) {
                    user.update("name", editText.getText().toString()).addOnSuccessListener(t2 -> {
                        cnt++;
                        editText.setVisibility(View.GONE);
                        linearLayout.setVisibility(View.VISIBLE);
                        btn.setVisibility(View.GONE);
                        text.setText("Выберите роль");
                        editText.setText("");
                    })
                            .addOnFailureListener(f -> Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_LONG).show());
                } else
                    Toast.makeText(this, "Имя не введено", Toast.LENGTH_LONG).show();
            } else if (cnt == 1) {
                if (String.valueOf(editText.getText()).equals("")) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    if (!String.valueOf(editText.getText()).equals(user.getUser().getUid())) {
                        user.getUsers().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                QueryDocumentSnapshot thisUser = null;
                                QueryDocumentSnapshot linkedUser = null;
                                for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                    if (document.getId().equals(String.valueOf(editText.getText()))) {
                                        linkedUser = document;
                                        Log.d("linkingUsers", linkedUser.getId());
                                    }
                                    if (document.getId().equals(user.getUser().getUid())) {
                                        thisUser = document;
                                        Log.d("linkingUsers", thisUser.getId());
                                    }
                                }
                                if (linkedUser == null) {
                                    Toast.makeText(this, "Пользователь с таким ID не найден", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                if (thisUser != null) {
                                    Boolean thisBool = (Boolean) thisUser.get("isSupport");
                                    Boolean linkBool = (Boolean) linkedUser.get("isSupport");
                                    String linkId = linkedUser.getId();
                                    String linkLinkUser = (String) linkedUser.get("linkUserId");
                                    if (thisBool != linkBool) {
                                        if (linkId.equals(linkLinkUser)) {
                                            Log.d("linkingUsers", "+");
                                            user.update("linkUserId", linkId).addOnSuccessListener(t -> {
                                                Log.d("linkingUsers", "++");
                                                user.update(linkId, "linkUserId", user.getUser().getUid()).addOnSuccessListener(t2 -> {
                                                    Log.d("linkingUsers", "link user update");
                                                    startActivity(new Intent(FirstRunActivity.this, MainActivity.class));
                                                    finish();
                                                })
                                                        .addOnFailureListener(f -> Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show());
                                            });
                                        } else
                                            Toast.makeText(this, "Ошибка: У пользователя уже есть связь с другим пользователем", Toast.LENGTH_LONG).show();
                                    } else
                                        Toast.makeText(this, "Ошибка: Ваша роль индентична роли пользователя", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.d("linkingUsers", "Error getting documents: ", task.getException());
                                Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

    }
}
