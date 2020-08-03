package com.example.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.parus.R;
import com.example.parus.data.User;
import com.example.parus.services.OnlineService;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Objects;


public class DialogLinkUser extends AppCompatDialogFragment {

    private TextView text;
    private EditText uId;
    private User user;

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_link_user, null);
        user = new User();
        uId = dialogView.findViewById(R.id.linkId);
        text = dialogView.findViewById(R.id.linkText);
        user.updateIsSupport().addOnSuccessListener(s -> {
            if (user.isSupport()) {
                text.setText("Введите ID подопечного");
                uId.setHint("ID подопечного");
            } else {
                text.setText("Введите ID помощника");
                uId.setHint("ID помощника");
            }
        });
        builder.setView(dialogView);
        builder
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Готово", (dialog, id) -> {
                    if (uId.getText().toString().isEmpty())
                        Toast.makeText(dialogView.getContext(), "Вы не ввели ID", Toast.LENGTH_LONG).show();
                    else {
                        user.getUsers().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                QueryDocumentSnapshot thisUser = null;
                                QueryDocumentSnapshot linkedUser = null;
                                for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                    if (document.getId().equals(String.valueOf(uId.getText()))) {
                                        linkedUser = document;
                                        Log.d("linkingUsers", linkedUser.getId());
                                    }
                                    if (document.getId().equals(user.getUser().getUid())) {
                                        thisUser = document;
                                        Log.d("linkingUsers", thisUser.getId());
                                    }
                                }
                                if (linkedUser == null) {
                                    Toast.makeText(dialogView.getContext(), "Пользователь с таким ID не найден", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                assert thisUser != null;
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
                                                dialogView.getContext().startService(new Intent(dialogView.getContext(), OnlineService.class).setAction("action"));
                                            })
                                                    .addOnFailureListener(f -> Toast.makeText(dialogView.getContext(), "Ошибка", Toast.LENGTH_LONG).show());
                                        });
                                    }
                                    else
                                        Toast.makeText(dialogView.getContext(), "Ошибка: У пользователя уже есть связь с другим пользователем", Toast.LENGTH_LONG).show();
                                } else
                                    Toast.makeText(dialogView.getContext(), "Ошибка: Ваша роль индентична роли пользователя", Toast.LENGTH_LONG).show();
                            } else {
                                Log.d("linkingUsers", "Error getting documents: ", task.getException());
                                Toast.makeText(dialogView.getContext(), "Ошибка", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });

        return builder.create();
    }
}
