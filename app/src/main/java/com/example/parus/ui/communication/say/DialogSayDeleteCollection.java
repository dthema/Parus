package com.example.parus.ui.communication.say;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.parus.data.User;

import java.util.LinkedList;
import java.util.List;


public class DialogSayDeleteCollection extends AppCompatDialogFragment {

    static DialogSayDeleteCollection newInstance(String[] msg) {
        DialogSayDeleteCollection fragment = new DialogSayDeleteCollection();
        Bundle bundle = new Bundle();
        bundle.putStringArray("msg", msg);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        User user = new User();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        assert getArguments() != null;
        final String[] collectionsNames = getArguments().getStringArray("msg");
        assert collectionsNames != null;
        if (collectionsNames.length == 0){
            Toast.makeText(builder.getContext(), "Нет коллекций", Toast.LENGTH_LONG).show();
            dismiss();
        }
        boolean[] checkedItemsArray = new boolean[collectionsNames.length];
        for (int i = 0; i < collectionsNames.length; i++) {
            checkedItemsArray[i] = false;
        }
        builder.setTitle("Выберите коллекции")
                .setMultiChoiceItems(collectionsNames, checkedItemsArray,
                        (dialog, which, isChecked) -> checkedItemsArray[which] = isChecked)
                .setPositiveButton("Удалить",
                        (dialog, id) -> {
                            List<String> list = new LinkedList<>();
                            for (int i = 0; i < collectionsNames.length; i++) {
                                if (checkedItemsArray[i]) {
                                    list.add(collectionsNames[i]);
                                }
                            }
                            String[] deletedCollections = new String[list.size()];
                            for (int i = 0; i < list.size(); i++) {
                                deletedCollections[i] = list.get(i);
                            }
                            user.deleteCollections(deletedCollections);
                        })
                .setNegativeButton("Отмена",
                        (dialog, id) -> dialog.dismiss());
        return builder.create();
    }
}
