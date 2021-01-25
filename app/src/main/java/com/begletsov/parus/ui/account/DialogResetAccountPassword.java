package com.begletsov.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
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

import com.begletsov.parus.R;
import com.begletsov.parus.viewmodels.UserViewModel;


public class DialogResetAccountPassword extends AppCompatDialogFragment {

    private EditText oldPassword;
    private EditText newPassword;
    private ProgressBar progressBar;
    private boolean use = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_reset_account_password, null);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        oldPassword = dialogView.findViewById(R.id.oldPassword);
        newPassword = dialogView.findViewById(R.id.newPassword);
        progressBar = dialogView.findViewById(R.id.resetPasswordProgressBar);
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
                    userViewModel.resetPassword(oldPassword.getText().toString(), newPassword.getText().toString())
                            .observe(DialogResetAccountPassword.this, string -> {
                                Toast.makeText(dialogView.getContext(), string, Toast.LENGTH_LONG).show();
                                dialog1.dismiss();
                            });
                }
            });
        });
        return dialog;
    }

}
