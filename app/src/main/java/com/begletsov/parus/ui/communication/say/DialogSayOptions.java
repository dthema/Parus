package com.begletsov.parus.ui.communication.say;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.begletsov.parus.R;
import com.begletsov.parus.viewmodels.SayViewModel;
import com.begletsov.parus.viewmodels.TTSViewModel;

public class DialogSayOptions extends AppCompatDialogFragment {

    private boolean change = false;
    private TTSViewModel TTS;
    private Double startSpeed = 1.;
    private Double startPitch = 1.;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] data = {"1", "2", "3", "4", "5"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_say_options, null);
        SayViewModel sayViewModel = new ViewModelProvider(this).get(SayViewModel.class);
        TTS = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(TTSViewModel.class);
        Button testSay = dialogView.findViewById(R.id.testSay);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        Spinner spinner = dialogView.findViewById(R.id.sayColumnSpinner);
        spinner.setAdapter(adapter);
        spinner.setPrompt("Выберите коллекцию");
        Object[] settings = sayViewModel.getSettings();
        Long startColumnCount = 1L;
        if (settings != null)
            startColumnCount = (Long) settings[2];
        final Long[] columnCount = {startColumnCount};
        spinner.setSelection(Integer.parseInt(startColumnCount.toString()) - 1);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                columnCount[0] = (long) position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (settings != null) {
            startSpeed = (Double) settings[0];
            startPitch = (Double) settings[1];
        }
        final Double[] speed = {startSpeed};
        final Double[] pitch = {startPitch};
        SeekBar speedSeekBar = dialogView.findViewById(R.id.speedSaySeekBar);
        speedSeekBar.setProgress(Integer.parseInt(String.valueOf(startSpeed * 4 - 4).substring(0, 1)));
        SeekBar pitchSeekBar = dialogView.findViewById(R.id.pitchSaySeekBar);
        pitchSeekBar.setProgress(Integer.parseInt(String.valueOf(startPitch * 4 - 4).substring(0, 1)));
        testSay.setOnClickListener(c -> TTS.speak("Проверка голоса"));
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed[0] = 0.25 * progress + 1;
                TTS.setSpeed(speed[0]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pitch[0] = 0.25 * progress + 1;
                TTS.setPitch(pitch[0]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        builder.setView(dialogView);
        builder.setTitle("Настройки");
        Long finalStartColumnCount = startColumnCount;
        builder.setPositiveButton("Сохранить", (dialog, id) -> {
            if (!(columnCount[0].equals(finalStartColumnCount) && speed[0].equals(startSpeed) && pitch[0].equals(startPitch))) {
                sayViewModel.setSaySettings(speed[0], pitch[0], columnCount[0]);
                change = true;
            } else
                dialog.dismiss();
        })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!change) {
            TTS.setSpeed(startSpeed);
            TTS.setPitch(startPitch);
        }
    }
}
