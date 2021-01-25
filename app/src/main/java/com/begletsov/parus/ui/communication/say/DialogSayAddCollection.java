package com.begletsov.parus.ui.communication.say;

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
import androidx.lifecycle.ViewModelProvider;

import com.begletsov.parus.R;
import com.begletsov.parus.viewmodels.SayViewModel;


public class DialogSayAddCollection extends AppCompatDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView=inflater.inflate(R.layout.dialog_say_add_collection, null);
        SayViewModel sayViewModel = new ViewModelProvider(this).get(SayViewModel.class);
        builder.setTitle("Введите название коллекции");
        builder.setView(dialogView);
        builder.setPositiveButton("Добавить", (dialog, id) -> {
            EditText txt = dialogView.findViewById(R.id.dialog_say_text);
            if(!String.valueOf(txt.getText()).trim().equals("")) {
                sayViewModel.addCollection(String.valueOf(txt.getText()));
            } else {
                Toast.makeText(builder.getContext(), "Поле не должно быть пустым", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}
