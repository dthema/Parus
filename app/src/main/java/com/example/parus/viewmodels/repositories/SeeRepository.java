package com.example.parus.viewmodels.repositories;

import android.util.Log;

import androidx.core.util.Pair;

import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SeeRepository {

    private Float textSize;

    public SeeRepository() {
    }

    public LiveData<Pair<String, Float>> detectText(FirebaseVisionImage image) {
        SingleLiveEvent<Pair<String, Float>> liveEvent = new SingleLiveEvent<>();
        textSize = 501f;
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        // распознавание текста с фото
        detector.processImage(image)
                .addOnSuccessListener(firebaseVisionText -> {
                    if (firebaseVisionText.getText().equals(""))
                        liveEvent.setValue(Pair.create("Не удалось распознать текст", 0f));
                    else
                        for (String string : firebaseVisionText.getText().split(" ")) {
                            neededTextSize(string);
                        }
                    liveEvent.setValue(Pair.create(firebaseVisionText.getText(), textSize));
                })
                .addOnFailureListener(e -> liveEvent.setValue(Pair.create("Произошла ошибка", 0f)));
        return liveEvent;
    }

    public LiveData<Pair<String, Float>> detectObject(FirebaseVisionImage image) {
        SingleLiveEvent<Pair<String, Float>> liveEvent = new SingleLiveEvent<>();
        textSize = 501f;
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler();
        // распознавание объекта
        labeler.processImage(image)
                .addOnSuccessListener(labels -> {
                    List<Pair<String, Float>> list = new ArrayList<>();
                    for (FirebaseVisionImageLabel label : labels) {
                        if (label.getConfidence() >= 0.7)
                            list.add(Pair.create(label.getText(), label.getConfidence()));
                    }
                    if (list.isEmpty()) {
                        liveEvent.setValue(Pair.create("Не удалось распознать объект(-ы)", 0f));
                    } else {
                        AtomicReference<String> result = new AtomicReference<>("");
                        for (int i = 0; i < list.size(); i++) {
                            Pair<String, Float> pair = list.get(i);
                            FirebaseTranslatorOptions options =
                                    new FirebaseTranslatorOptions.Builder()
                                            .setSourceLanguage(FirebaseTranslateLanguage.EN)
                                            .setTargetLanguage(FirebaseTranslateLanguage.RU)
                                            .build();
                            final FirebaseTranslator translator =
                                    FirebaseNaturalLanguage.getInstance().getTranslator(options);
                            String text = pair.first;
                            if (text != null && pair.second != null) {
                                // перевод результата
                                int finalI = i;
                                translator.downloadModelIfNeeded()
                                        .addOnSuccessListener(v ->
                                                translator.translate(text).addOnSuccessListener(
                                                        translatedText -> {
                                                            // показать, если шанс "угадывания" объекта более 70%
                                                            float confidence = pair.second;
                                                            Log.d("ABCd", text);
                                                            switch (text) {
                                                                case "Nail":
                                                                    result.set(result.get() +
                                                                            "Ноготь - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                                case "Skin":
                                                                    result.set(result.get() +
                                                                            "Кожа - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                                case "Flash":
                                                                    result.set(result.get() +
                                                                            "Вспышка - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                                case "Pattern":
                                                                    result.set(result.get() +
                                                                            "Узор - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                                case "Room":
                                                                    result.set(result.get() +
                                                                            "Комната - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                                case "Ear":
                                                                    result.set(result.get() +
                                                                            "Ухо - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                                default:
                                                                    result.set(result.get() + translatedText +
                                                                            " - " + Math.round(confidence * 100) + "%" + "\n");
                                                                    break;
                                                            }
                                                            neededTextSize(translatedText);
                                                            if (finalI == list.size() - 1)
                                                                liveEvent.setValue(Pair.create(result.get(), textSize));
                                                        })
                                                        .addOnFailureListener(
                                                                e -> liveEvent.setValue(Pair.create("Не удалось загрузить языковой пакет", 0f))));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> liveEvent.setValue(Pair.create("Произошла ошибка", 0f)));
        return liveEvent;
    }

    private void neededTextSize(String text) {
        // выбор оптимального размера шрифта
        float min;
        switch (text.length()) {
            case 1:
                min = 300;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 2:
                min = 180;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 3:
                min = 120;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 4:
                min = 90;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 5:
                min = 70;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 6:
                min = 60;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 7:
                min = 50;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 8:
                min = 48;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            case 9:
                min = 40;
                if (textSize < min * 2.75)
                    min = 500;
                break;
            default:
                min = 30;
                break;
        }
        if (min != 500)
            textSize = min;
    }
}
