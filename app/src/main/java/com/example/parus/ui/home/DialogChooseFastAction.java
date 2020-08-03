package com.example.parus.ui.home;

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

public class DialogChooseFastAction extends AppCompatDialogFragment {

    private EditText text;
    private Spinner spinner;
    private String[] spinnerData = {"Отключить быстрое действие", "Распознать текст", "Распознать объект", "Начать слушать", "Сказать/Показать"};
    private User user;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        user = new User();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_choose_fast_action, null);
        text = dialogView.findViewById(R.id.fastDialogText);
        spinner = dialogView.findViewById(R.id.fastDialogSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item3, spinnerData);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item2);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 3)
                    text.setVisibility(View.VISIBLE);
                else
                    text.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, id) -> {
            if (spinner.getSelectedItemPosition() <= 3) {
                user.getDatabase().collection("users").document(user.getUser().getUid()).update("fastAction", String.valueOf(spinner.getSelectedItemPosition()));
                dialog.dismiss();
            } else
                if (text.length() > 0) {
                    user.getDatabase().collection("users").document(user.getUser().getUid()).update("fastAction", text.getText().toString());
                    dialog.dismiss();
                } else {
                    Toast.makeText(dialogView.getContext(), "Вы не ввели фразу", Toast.LENGTH_LONG).show();
                }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}
