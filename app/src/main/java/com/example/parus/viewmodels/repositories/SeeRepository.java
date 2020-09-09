package com.example.parus.viewmodels.repositories;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.util.Pair;

import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.concurrent.atomic.AtomicReference;

public class SeeRepository {

    private Float textSize;

    public SeeRepository() {
    }

    public LiveData<Pair<String, Float>> detectText(Bitmap bitmap) {
        SingleLiveEvent<Pair<String, Float>> liveEvent = new SingleLiveEvent<>();
        textSize = 501f;
        if (bitmap == null) {
            liveEvent.setValue(Pair.create("Не удалось распознать текст", 0f));
            return liveEvent;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        // распознавание текста с фото
        recognizer.process(image)
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

    public LiveData<Pair<String, Float>> detectObject(Bitmap bitmap) {
        SingleLiveEvent<Pair<String, Float>> liveEvent = new SingleLiveEvent<>();
        textSize = 501f;
        if (bitmap == null) {
            liveEvent.setValue(Pair.create("Не удалось обнаружить объект(-ы)", 0f));
            return liveEvent;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        ImageLabelerOptions options =
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.7f)
                        .build();
        ImageLabeler labeler = ImageLabeling.getClient(options);
        // распознавание объекта
        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    if (labels.isEmpty()) {
                        liveEvent.setValue(Pair.create("Не удалось обнаружить объект(-ы)", 0f));
                        return;
                    }
                    AtomicReference<String> result = new AtomicReference<>("");
                    for (ImageLabel label : labels) {
                        String text = label.getText();
                        TranslatorOptions translatorOptions =
                                new TranslatorOptions.Builder()
                                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                                        .setTargetLanguage(TranslateLanguage.RUSSIAN)
                                        .build();
                        final Translator translator =
                                Translation.getClient(translatorOptions);
                        // перевод результата
                        translator.downloadModelIfNeeded()
                                .addOnSuccessListener(v ->
                                        translator.translate(text).addOnSuccessListener(
                                                translatedText -> {
                                                    // показать, если шанс "угадывания" объекта более 70%
                                                    switch (text) {
                                                        case "Nail":
                                                            result.set(result.get() +
                                                                    "Ноготь - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                        case "Skin":
                                                            result.set(result.get() +
                                                                    "Кожа - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                        case "Flash":
                                                            result.set(result.get() +
                                                                    "Вспышка - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                        case "Pattern":
                                                            result.set(result.get() +
                                                                    "Узор - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                        case "Room":
                                                            result.set(result.get() +
                                                                    "Комната - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                        case "Ear":
                                                            result.set(result.get() +
                                                                    "Уши - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                        default:
                                                            result.set(result.get() + translatedText +
                                                                    " - " + Math.round(label.getConfidence() * 100) + "%" + "\n");
                                                            break;
                                                    }
                                                    Log.d("TAGAA",result.get());
                                                    neededTextSize(translatedText);
                                                    if (labels.indexOf(label) == labels.size()-1)
                                                        liveEvent.setValue(Pair.create(result.get(), textSize));
                                                })
                                                .addOnFailureListener(
                                                        e -> liveEvent.setValue(Pair.create("Не удалось загрузить языковой пакет", 0f))));
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
