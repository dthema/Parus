package com.example.parus.ui.communication.say;

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
import com.example.parus.data.User;


public class DialogSayAddCollection extends AppCompatDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView=inflater.inflate(R.layout.dialog_say_add_collection, null);
        builder.setTitle("Введите название коллекции");
        builder.setView(dialogView);
        builder.setPositiveButton("Добавить", (dialog, id) -> {
            EditText txt = dialogView.findViewById(R.id.dialog_say_text);
            if(!String.valueOf(txt.getText()).trim().equals("")) {
                User user = new User();
                user.updateCollection().addOnSuccessListener(t1 -> user.addCollection(String.valueOf(txt.getText())));
            } else {
                Toast.makeText(builder.getContext(), "Поле не должно быть пустым", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}
