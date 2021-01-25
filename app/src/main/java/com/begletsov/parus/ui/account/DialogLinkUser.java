package com.begletsov.parus.ui.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.begletsov.parus.R;
import com.begletsov.parus.viewmodels.ServiceViewModel;
import com.begletsov.parus.viewmodels.UserViewModel;


public class DialogLinkUser extends AppCompatDialogFragment {

    private EditText uId;

    static DialogLinkUser newInstance(boolean isSupport) {
        Bundle args = new Bundle();
        args.putBoolean("support", isSupport);
        DialogLinkUser fragment = new DialogLinkUser();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_link_user, null);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        ServiceViewModel serviceViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(ServiceViewModel.class);
        uId = dialogView.findViewById(R.id.linkId);
        TextView text = dialogView.findViewById(R.id.linkText);
        assert getArguments() != null;
        boolean isSupport = getArguments().getBoolean("support");
        if (isSupport) {
            text.setText("Введите ID подопечного");
            uId.setHint("ID подопечного");
        } else {
            text.setText("Введите ID помощника");
            uId.setHint("ID помощника");
        }
        builder.setView(dialogView);
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Готово", (dialog, id) -> {});
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialog1 -> {
            Button positiveButton = ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> userViewModel.setLinkUser(uId.getText().toString(), isSupport)
                    .observe(DialogLinkUser.this, string -> {
                        if ("1".equals(string)) {
                            serviceViewModel.startOnlineService();
                        } else {
                            Toast.makeText(dialogView.getContext(), string, Toast.LENGTH_LONG).show();
                        }
                        dialog1.dismiss();
                    }));
        });
        return dialog;
    }
}
