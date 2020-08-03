package com.example.parus.ui.communication.say;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import com.example.parus.R;
import com.example.parus.data.User;

public class DialogSayOptions extends AppCompatDialogFragment {

    private boolean flag;
    private boolean closed;

    static DialogSayOptions newInstance(User user) {
        DialogSayOptions fragment = new DialogSayOptions();
        Bundle bundle = new Bundle();
        bundle.putParcelable("user", user);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        closed = false;
        flag = false;
        User user = null;
        if (getArguments() != null)
            user = getArguments().getParcelable("user");
        String[] data = {"1", "2", "3", "4", "5"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_say_options, null);
        Button testSay = dialogView.findViewById(R.id.testSay);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        Spinner spinner = dialogView.findViewById(R.id.sayColumnSpinner);
        spinner.setAdapter(adapter);
        spinner.setPrompt("Выберите коллекцию");
        assert user != null;
        final Long startColumnCount = (Long) user.getSettings()[2];
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
        final Double startSpeed = (Double) user.getSettings()[0];
        final Double startPitch = (Double) user.getSettings()[1];
        final Double[] speed = {startSpeed};
        final Double[] pitch = {startPitch};
        SeekBar speedSeekBar = dialogView.findViewById(R.id.speedSaySeekBar);
        if (speed[0] == 1.0)
            speedSeekBar.setProgress(0);
        else if (speed[0] == 1.25)
            speedSeekBar.setProgress(1);
        else if (speed[0] == 1.5)
            speedSeekBar.setProgress(2);
        else if (speed[0] == 1.75)
            speedSeekBar.setProgress(3);
        else if (speed[0] == 2)
            speedSeekBar.setProgress(4);
        SeekBar pitchSeekBar = dialogView.findViewById(R.id.pitchSaySeekBar);
        if (pitch[0] == 1.0)
            pitchSeekBar.setProgress(0);
        else if (pitch[0] == 1.25)
            pitchSeekBar.setProgress(1);
        else if (pitch[0] == 1.5)
            pitchSeekBar.setProgress(2);
        else if (pitch[0] == 1.75)
            pitchSeekBar.setProgress(3);
        else if (pitch[0] == 2.0)
            pitchSeekBar.setProgress(4);
        TextToSpeech tts = new TextToSpeech(dialogView.getContext(), status -> {});
        testSay.setOnClickListener(c -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak("Проверка голоса", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak("Проверка голоса", TextToSpeech.QUEUE_FLUSH, null);
            }
        });
        tts.setSpeechRate(Float.parseFloat(speed[0].toString()));
        tts.setPitch(Float.parseFloat(pitch[0].toString()));
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress) {
                    case 0:
                        speed[0] = 1.0;
                        tts.setSpeechRate(Float.parseFloat(speed[0].toString()));
                        break;
                    case 1:
                        speed[0] = 1.25;
                        tts.setSpeechRate(Float.parseFloat(speed[0].toString()));
                        break;
                    case 2:
                        speed[0] = 1.5;
                        tts.setSpeechRate(Float.parseFloat(speed[0].toString()));
                        break;
                    case 3:
                        speed[0] = 1.75;
                        tts.setSpeechRate(Float.parseFloat(speed[0].toString()));
                        break;
                    case 4:
                        speed[0] = 2.0;
                        tts.setSpeechRate(Float.parseFloat(speed[0].toString()));
                        break;
                }
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
                switch (progress) {
                    case 0:
                        pitch[0] = 1.0;
                        tts.setPitch(Float.parseFloat(pitch[0].toString()));
                        break;
                    case 1:
                        pitch[0] = 1.25;
                        tts.setPitch(Float.parseFloat(pitch[0].toString()));
                        break;
                    case 2:
                        pitch[0] = 1.5;
                        tts.setPitch(Float.parseFloat(pitch[0].toString()));
                        break;
                    case 3:
                        pitch[0] = 1.75;
                        tts.setPitch(Float.parseFloat(pitch[0].toString()));
                        break;
                    case 4:
                        pitch[0] = 2.0;
                        tts.setPitch(Float.parseFloat(pitch[0].toString()));
                        break;

                }
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
        User finalUser = user;
        builder.setPositiveButton("Сохранить", (dialog, id) -> {
            if (!(columnCount[0].equals(startColumnCount) && speed[0].equals(startSpeed) && pitch[0].equals(startPitch))) {
                finalUser.setSaySettings(speed[0], pitch[0], columnCount[0]).addOnSuccessListener(t1 -> flag = true);
            } else {
                dialog.dismiss();
                closed = true;
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> {
            closed = true;
            dialog.dismiss();
        });
        return builder.create();
    }

    boolean isClosed() {
        return closed;
    }

    boolean isFlag() {
        return flag;
    }
}
