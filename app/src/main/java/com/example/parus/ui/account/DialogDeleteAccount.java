package com.example.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.example.parus.FirstRunActivity;
import com.example.parus.LoginActivity;
import com.example.parus.R;
import com.example.parus.services.OnlineService;
import com.example.parus.ui.communication.say.SayActivity;
import com.example.parus.viewmodels.ServiceViewModel;
import com.example.parus.viewmodels.UserViewModel;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class DialogDeleteAccount extends AppCompatDialogFragment {

    private EditText email;
    private EditText password;
    private ProgressBar progressBar;
    private boolean use = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_delete_account, null);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        ServiceViewModel serviceViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(ServiceViewModel.class);
        email = dialogView.findViewById(R.id.delaccDialogEmail);
        password = dialogView.findViewById(R.id.delaccDialogPassword);
        progressBar = dialogView.findViewById(R.id.deleteProgressBar);
        builder.setView(dialogView);
        builder
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Удалить", (dialog, id) -> {});
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> {
            Button positiveButton = ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (!use) {
                    Log.d("TAGAA", "S:");
                    progressBar.setVisibility(View.VISIBLE);
                    use = true;
                    userViewModel.delete(email.getText().toString(), password.getText().toString())
                            .observe(DialogDeleteAccount.this, string -> {
                                if ("1".equals(string)){
                                    serviceViewModel.stopAllServices();
                                    requireActivity().finish();
                                } else
                                    Toast.makeText(dialogView.getContext(), string, Toast.LENGTH_LONG).show();
                                dialog1.dismiss();
                            });
                }
            });
        });
        return dialog;
    }
}
