package com.example.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.parus.R;
import com.example.parus.data.User;
import com.example.parus.services.OnlineService;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class DialogDeleteAccount extends AppCompatDialogFragment {

    private EditText email;
    private EditText password;
    private boolean flag;
    private boolean closed;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        flag = false;
        closed = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_delete_account, null);
        email = dialogView.findViewById(R.id.delaccDialogEmail);
        password = dialogView.findViewById(R.id.delaccDialogPassword);
        builder.setView(dialogView);
        builder
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Удалить", (dialog, id) -> {
                    if (email.getText().toString().isEmpty() || password.getText().toString().isEmpty()) {
                        Toast.makeText(dialogView.getContext(), "Поля не заполнены", Toast.LENGTH_LONG).show();
                        closed = true;
                    } else {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        AuthCredential credential = EmailAuthProvider
                                .getCredential(email.getText().toString(), password.getText().toString());

                        assert user != null;
                        user.reauthenticate(credential)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("TAGAA", "User re-authenticated.");
                                            User mUser = new User();
                                        mUser.updateLinkUser().addOnSuccessListener(s ->{
                                            if (!mUser.getLinkUserId().equals(user.getUid()))
                                                // отвязывание связанного пользователя, если он есть
                                                mUser.getDatabase().collection("users").document(mUser.getLinkUserId()).update("linkUserId", mUser.getLinkUserId())
                                                        .addOnSuccessListener(l-> mUser.getDatabase().collection("users").document(user.getUid()).delete()
                                                        .addOnSuccessListener(t-> deleteUser(user, mUser, dialogView, dialog)));
                                            else
                                                deleteUser(user, mUser, dialogView, dialog);

                                        });
                                    } else {
                                        Toast.makeText(dialogView.getContext(), "Данные введены неверно", Toast.LENGTH_LONG).show();
                                        closed = true;
                                        dialog.dismiss();
                                    }
                                });

                    }
                });

        return builder.create();
    }

    private void deleteUser(FirebaseUser user, User mUser, View dialogView, DialogInterface dialog){
        mUser.getDatabase().collection("users").document(user.getUid()).delete()
                .addOnSuccessListener(t-> user.delete()
                        .addOnCompleteListener(task12 -> {
                            if (task12.isSuccessful()) {
                                FirebaseAuth.getInstance().signOut();
                                flag = true;
                                dialogView.getContext().stopService(new Intent(dialogView.getContext(), OnlineService.class));
                            } else {
                                closed = true;
                                Toast.makeText(dialogView.getContext(), "Ошибка", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        }));
    }

    boolean isFlag() {
        return flag;
    }

    boolean isClosed() {
        return closed;
    }
}
