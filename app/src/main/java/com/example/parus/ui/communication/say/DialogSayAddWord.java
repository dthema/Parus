package com.example.parus.ui.communication.say;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.parus.R;
import com.example.parus.data.User;

public class DialogSayAddWord extends AppCompatDialogFragment {

    static DialogSayAddWord newInstance(String[] msg) {
        DialogSayAddWord fragment = new DialogSayAddWord();
        Bundle bundle = new Bundle();
        bundle.putStringArray("msg", msg);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getArguments() != null;
        String[] data = getArguments().getStringArray("msg");
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        assert data != null;
        if (data.length == 0){
            Toast.makeText(builder.getContext(), "Нет коллекций", Toast.LENGTH_LONG).show();
            dismiss();
        }
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView=inflater.inflate(R.layout.dialog_say_add_word, null);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        Spinner spinner = dialogView.findViewById(R.id.addWordSpinner);
        spinner.setAdapter(adapter);
        spinner.setPrompt("Выберите коллекцию");
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                              @Override
                                              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                              }

                                              @Override
                                              public void onNothingSelected(AdapterView<?> parent) {

                                              }
                                          });
                builder.setView(dialogView);
                builder.setTitle("Выберите коллекцию");
        builder.setPositiveButton("Добавить", (dialog, id) -> {
            EditText txt = dialogView.findViewById(R.id.dialog_say_addWord_text);
            if(!String.valueOf(txt.getText()).trim().equals("")) {
                User user = new User();
                user.updateCollection().addOnSuccessListener(t1-> user.addCollectionWord(spinner.getSelectedItem().toString(), String.valueOf(txt.getText())));
            } else {
                Toast.makeText(builder.getContext(), "Поле не должно быть пустым", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}
