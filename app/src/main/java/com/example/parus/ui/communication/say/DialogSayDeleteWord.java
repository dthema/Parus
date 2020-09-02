package com.example.parus.ui.communication.say;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.parus.R;
import com.example.parus.viewmodels.SayViewModel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class DialogSayDeleteWord extends AppCompatDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CheckBox[][] checkBoxes = {null};
        final boolean[][] checkedItemsArray = {null};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        SayViewModel sayViewModel = new ViewModelProvider(this).get(SayViewModel.class);
        String[] data = sayViewModel.getCollectionsString();
        assert data != null;
        if (data.length == 0) {
            Toast.makeText(builder.getContext(), "Нет коллекций", Toast.LENGTH_LONG).show();
            dismiss();
        }
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_say_delete_word, null);
        LinearLayout dialogLayout = dialogView.findViewById(R.id.dialog_linear);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        Spinner spinner = dialogView.findViewById(R.id.deleteWordTopSpinner);
        spinner.setAdapter(adapter);
        spinner.setPrompt("Выберите коллекцию");
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dialogLayout.removeAllViews();
                List<String> wordsList = sayViewModel.getCollectionWords(spinner.getSelectedItem().toString());
                if (wordsList != null) {
                    String[] wordsNames = new String[wordsList.size()];
                    checkedItemsArray[0] = new boolean[wordsList.size()];
                    checkBoxes[0] = new CheckBox[wordsList.size()];
                    for (int i = 0; i < wordsList.size(); i++) {
                        checkedItemsArray[0][i] = false;
                        wordsNames[i] = wordsList.get(i);
                        CheckBox checkBox = new CheckBox(getContext());
                        checkBox.setText(wordsNames[i]);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        checkBox.getRootView().setPaddingRelative(50, 30, 0, 30);
                        checkBox.setTextSize(20f);
                        checkBox.setLayoutParams(params);
                        int finalI = i;
                        checkBox.setOnClickListener(l -> checkedItemsArray[0][finalI] = !checkedItemsArray[0][finalI]);
                        checkBox.setAllCaps(false);
                        checkBox.setId(111001010 + i);
                        checkBoxes[0][i] = checkBox;
                        Log.d("TAGAA", "+");
                        dialogLayout.addView(checkBoxes[0][i], i);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setSelection(0, true);
        builder.setView(dialogView);
        builder.setTitle("Выберите коллекцию");
        builder.setPositiveButton("Удалить", (dialog, id) -> {
            if (sayViewModel.getCollectionWords(spinner.getSelectedItem().toString()).size() == 0) {
                Toast.makeText(builder.getContext(), "В коллекции нет фраз", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                boolean hasChecked = false;
                List<String> list = new ArrayList<>();
                if (checkedItemsArray[0] != null)
                    for (int i = 0; i < checkedItemsArray[0].length; i++) {
                        if (checkedItemsArray[0][i]) {
                            hasChecked = true;
                            list.add(checkBoxes[0][i].getText().toString());
                        }
                    }
                if (hasChecked) {
                    String[] words = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        words[i] = list.get(i);
                        Log.d("ABCD", words[i]);
                    }
                    sayViewModel.deleteCollectionWord(spinner.getSelectedItem().toString(), words);
                } else {
                    Toast.makeText(builder.getContext(), "Фразы не выбраны", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.show();
    }
}
