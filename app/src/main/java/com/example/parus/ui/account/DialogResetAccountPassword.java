package com.example.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.parus.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;


public class DialogResetAccountPassword extends AppCompatDialogFragment {

    private EditText oldPassword;
    private EditText newPassword;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_reset_account_password, null);
        oldPassword = dialogView.findViewById(R.id.oldPassword);
        newPassword = dialogView.findViewById(R.id.newPassword);
        builder.setView(dialogView);
        builder
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Сменить", (dialog, id) -> {
                    if (oldPassword.getText().toString().isEmpty() || newPassword.getText().toString().isEmpty()) {
                        Toast.makeText(dialogView.getContext(), "Поля не заполнены", Toast.LENGTH_LONG).show();
                    } else {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        assert user != null;
                        AuthCredential credential = EmailAuthProvider
                                .getCredential(Objects.requireNonNull(user.getEmail()), oldPassword.getText().toString());
                        user.reauthenticate(credential)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        user.updatePassword(newPassword.getText().toString())
                                                .addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        Toast.makeText(dialogView.getContext(), "Пароль изменён", Toast.LENGTH_LONG).show();
                                                    } else
                                                        Toast.makeText(dialogView.getContext(), "Ошибка", Toast.LENGTH_LONG).show();
                                                });
                                    } else {
                                        Toast.makeText(dialogView.getContext(), "Данные введены неверно", Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                    }
                                });

                    }
                });

        return builder.create();
    }

}
