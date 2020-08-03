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

import com.example.parus.R;
import com.example.parus.data.User;

import java.util.LinkedList;
import java.util.List;


public class DialogSayDeleteWord extends AppCompatDialogFragment {

    static DialogSayDeleteWord newInstance(String[] msg, User user) {
        DialogSayDeleteWord fragment = new DialogSayDeleteWord();
        Bundle bundle = new Bundle();
        bundle.putStringArray("msg", msg);
        bundle.putParcelable("user", user);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CheckBox[][] checkBoxes = {null};
        final boolean[][] checkedItemsArray = {null};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        assert getArguments() != null;
        String[] data = getArguments().getStringArray("msg");
        assert data != null;
        if (data.length == 0){
            Toast.makeText(builder.getContext(), "Нет коллекций", Toast.LENGTH_LONG).show();
            dismiss();
        }
        getArguments().getStringArray("msg");
        User user = getArguments().getParcelable("user");
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView=inflater.inflate(R.layout.dialog_say_delete_word, null);
        LinearLayout dialogLayout = dialogView.findViewById(R.id.dialog_linear);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        Spinner spinner = dialogView.findViewById(R.id.deleteWordTopSpinner);
        spinner.setAdapter(adapter);
        spinner.setPrompt("Выберите коллекцию");
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    assert user != null;
                    List<String> wordsList = user.getCollectionWords(spinner.getSelectedItem().toString());
                    if (wordsList != null) {
                        Log.d("ABCD", "+");
                        String[] wordsNames = new String[wordsList.size()];
                        checkedItemsArray[0] = new boolean[wordsList.size()];
                        checkBoxes[0] = new CheckBox[wordsList.size()];
                        for (int i = 0; i < wordsList.size(); i++) {
                            checkedItemsArray[0][i] = false;
                            wordsNames[i] = wordsList.get(i);
                            dialogLayout.removeAllViews();
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
                        }
                        for (int i = 0; i < wordsList.size(); i++) {
                            dialogLayout.addView(checkBoxes[0][i], i);
                        }
                    } else {
                        Log.d("ABCD", "-");
                        dialogLayout.removeAllViews();
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
            assert user != null;
            user.updateCollection().addOnSuccessListener(t1->{
                        if (user.getCollectionWords(spinner.getSelectedItem().toString()).size()==0){
                            Toast.makeText(builder.getContext(), "В коллекции нет фраз", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            boolean hasChecked = false;
                            List<String> list = new LinkedList<>();
                            if (checkedItemsArray[0] != null)
                                for (int i = 0; i < checkedItemsArray[0].length; i++) {
                                    if (checkedItemsArray[0][i]){
                                        hasChecked = true;
                                        list.add(checkBoxes[0][i].getText().toString());
                                    }
                                }
                            if (hasChecked){
                                String[] words = new String[list.size()];
                                for (int i = 0; i < list.size(); i++) {
                                    words[i] = list.get(i);
                                    Log.d("ABCD", words[i]);
                                }
                                user.deleteCollectionWord(spinner.getSelectedItem().toString(), words);
                            } else {
                                Toast.makeText(builder.getContext(), "Фразы не выбраны", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        }
                });
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.show();
    }
}
