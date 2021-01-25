package com.begletsov.parus.ui.communication.say;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.begletsov.parus.viewmodels.SayViewModel;

import java.util.LinkedList;
import java.util.List;


public class DialogSayDeleteCollection extends AppCompatDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        SayViewModel sayViewModel = new ViewModelProvider(this).get(SayViewModel.class);
        final String[] collectionsNames = sayViewModel.getCollectionsString();
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
                            sayViewModel.deleteCollections(deletedCollections);
                        })
                .setNegativeButton("Отмена",
                        (dialog, id) -> dialog.dismiss());
        return builder.create();
    }
}
