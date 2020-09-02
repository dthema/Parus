package com.example.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.parus.R;
import com.example.parus.viewmodels.UserViewModel;


public class DialogResetAccountEmail extends AppCompatDialogFragment {

    private EditText newEmail;
    private EditText password;
    private ProgressBar progressBar;
    private boolean use = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_reset_account_email, null);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        newEmail = dialogView.findViewById(R.id.newEmail);
        password = dialogView.findViewById(R.id.emailPassword);
        progressBar = dialogView.findViewById(R.id.resetEmailProgressBar);
        builder.setView(dialogView);
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Изменить", (dialog, id) -> {});
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> {
            Button positiveButton = ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (!use) {
                    progressBar.setVisibility(View.VISIBLE);
                    use = true;
                    userViewModel.resetEmail(password.getText().toString(), newEmail.getText().toString())
                            .observe(DialogResetAccountEmail.this, string -> {
                                Toast.makeText(dialogView.getContext(), string, Toast.LENGTH_LONG).show();
                                dialog1.dismiss();
                            });
                }
            });
        });
        return dialog;
    }
}
