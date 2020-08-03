package com.example.parus.ui.chat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parus.R;
import com.example.parus.RequestTime;
import com.example.parus.data.User;
import com.example.parus.services.MyFirebaseMessagingService;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.apache.commons.net.ntp.TimeStamp;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatFragment extends Fragment implements
        RecognitionListener {

    private static final String LOG_TAG = "Chat";
    private User user;
    private MessageAdapter messageAdapter;
    private List<Chat> chats;
    private RecyclerView recyclerView;
    private RelativeLayout relativeLayout;
    private TextView textView;
    private EditText sendText;
    private Button send;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private boolean isRecording;
    private static final int REQUEST_RECORD_PERMISSION = 100;

    private boolean isNetworkAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listenerRegistration != null)
            listenerRegistration.remove();
        listenerRegistration = null;
        MyFirebaseMessagingService.inChat = false;
        stopCheckNetwork();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            MyFirebaseMessagingService.inChat = true;
            startCheckNetwork();
            addListener();
        }
    }

    private void InternetOff() {
        textView.setVisibility(View.VISIBLE);
        relativeLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void InternetOn() {
        textView.setVisibility(View.GONE);
        relativeLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private boolean isCheckNetworkThreadActive = false;
    final private static int NETWORK_CHECK_INTERVAL = 1000;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // не показывать уведомления, если чат активен
            MyFirebaseMessagingService.inChat = false;
            // не отслеживать интернет соединение
            stopCheckNetwork();
            isRecording = false;
            progressBar.setVisibility(View.GONE);
            if (speech != null) {
                speech.destroy();
            }
            if (listenerRegistration != null)
                listenerRegistration.remove();
            listenerRegistration = null;
        } else {
            // отслеживать интернет соединение
            startCheckNetwork();
            // показывать уведомления, если чат не активен
            MyFirebaseMessagingService.inChat = true;
            // обновление данных о пользователе
            user.updateIsSupport().addOnSuccessListener(s -> user.updateLinkUser().addOnSuccessListener(l -> user.getUsers().addOnCompleteListener(task -> {
                speech = SpeechRecognizer.createSpeechRecognizer(getContext());
                speech.setRecognitionListener(this);
                recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                        "ru");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                send.setOnClickListener(c -> {
                    if (!sendText.getText().toString().trim().equals("")) {
                        if (user.isSupport())
                            sendMessage(user.getUser().getUid(), user.getLinkUserId(), sendText.getText().toString(), true);
                        else {
                            sendMessage(user.getUser().getUid(), user.getLinkUserId(), sendText.getText().toString(), false);
                        }
                        sendText.setText("");
                    } else {
                        // ввод текста голосом
                        if (isRecording) { // завершить ввод голосом
                            speech.stopListening();
                            progressBar.setIndeterminate(false);
                            progressBar.setVisibility(View.INVISIBLE);
                            isRecording = false;
                            Log.d(LOG_TAG, "-");
                        } else { // начать ввод голосом
                            Log.d(LOG_TAG, "+");
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setIndeterminate(true);
                            if (ActivityCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(requireContext(),
                                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                progressBar.setVisibility(View.GONE);
                                ActivityCompat.requestPermissions(requireActivity(),
                                        new String[]{Manifest.permission.RECORD_AUDIO,
                                                Manifest.permission.RECORD_AUDIO},
                                        REQUEST_RECORD_PERMISSION);
                            } else {
                                speech.startListening(recognizerIntent);
                                isRecording = true;
                                Log.e(LOG_TAG, "PERMISSION GRANTED");
                            }
                        }
                    }
                });
                addListener();
                CollectionReference ref;
                if (user.isSupport()) {
                    ref = user.getDatabase().collection("chats").document(user.getLinkUserId() + user.getUser().getUid()).collection("chat");
                } else {
                    ref = user.getDatabase().collection("chats").document(user.getUser().getUid() + user.getLinkUserId()).collection("chat");
                }
                // отслеживание новых сообщений
                if (messageListener == null)
                    messageListener = ref.addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            return;
                        }
                        assert queryDocumentSnapshots != null;
                        for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Chat chat = dc.getDocument().toObject(Chat.class);
                                    for (Chat c : chats) {
                                        if (c.getMessage().equals(chat.getMessage()) && c.getDate().compareTo(chat.getDate()) == 0) {
                                            return;
                                        }
                                    }
                                    if (chat.getReceiver().equals(user.getLinkUserId()) && chat.getSender().equals(user.getUser().getUid()) || chat.getReceiver().equals(user.getUser().getUid()) && chat.getSender().equals(user.getLinkUserId())) {
                                        chats.add(chat);
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot queryDocumentSnapshot : Objects.requireNonNull(task.getResult())) {
                                                if (queryDocumentSnapshot.getId().equals(user.getLinkUserId())) {
                                                    Collections.sort(chats, (c1, c2) -> c1.getDate().compareTo(c2.getDate()));
                                                    messageAdapter = new MessageAdapter(getContext(), chats, Objects.requireNonNull(queryDocumentSnapshot.get("name")).toString());
                                                    recyclerView.setAdapter(messageAdapter);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case MODIFIED:
                                    break;
                                case REMOVED:
                                    chats.clear();
                                    messageAdapter = new MessageAdapter(getContext(), chats, "");
                                    recyclerView.setAdapter(messageAdapter);
                                    break;
                            }
                        }
                    });
            })));
        }
    }

    private void setNetworkActive() {
        if (isCheckNetworkThreadActive) {
            requireActivity().runOnUiThread(this::InternetOn);
        }
    }

    private void setNetworkDisable() {
        if (isCheckNetworkThreadActive) {
            requireActivity().runOnUiThread(this::InternetOff);
        }
    }

    private void startCheckNetwork() {
        if (!isCheckNetworkThreadActive) {
            isCheckNetworkThreadActive = true;
            Thread checkNetworkThread = new Thread(() -> {
                while (isCheckNetworkThreadActive) {
                    if (isVisible()) {
                        // удаление уведомлений о новых сообщениях
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
                        notificationManager.cancel(1);
                        if (isNetworkAvailable()) {
                            setNetworkActive();
                        } else {
                            setNetworkDisable();
                        }
                        try {
                            Thread.sleep(NETWORK_CHECK_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            checkNetworkThread.setName("Check Network Thread");
            checkNetworkThread.start();
        }
    }

    private void stopCheckNetwork() {
        isCheckNetworkThreadActive = false;
    }

    private ListenerRegistration listenerRegistration;
    private ListenerRegistration messageListener;

    private void addListener() {
        listenerRegistration = user.getDatabase().collection("users").document(user.getUser().getUid()).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.d(LOG_TAG, e.getMessage());
                return;
            }
            if (documentSnapshot != null)
                if (documentSnapshot.getString("linkUserId") != null)
                    if (documentSnapshot.getString("linkUserId").equals(user.getUser().getUid())) {
                        // удаление сообщений при разрыве связи с пользователем
                        if (messageListener != null)
                            messageListener.remove();
                        messageListener = null;
                        chats.clear();
                        messageAdapter = new MessageAdapter(getContext(), chats, "");
                        recyclerView.setAdapter(messageAdapter);
                        send.setOnClickListener(c -> Toast.makeText(getContext(), "Нет связи с другим пользователем", Toast.LENGTH_LONG).show());
                    }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        isRecording = false;
        user = new User();
        View root = inflater.inflate(R.layout.fragment_chat, container, false);
        send = root.findViewById(R.id.chatSend);
        textView = root.findViewById(R.id.chatInternet);
        progressBar = root.findViewById(R.id.chatProgressBar);
        progressBar.setVisibility(View.INVISIBLE);
        sendText = root.findViewById(R.id.chatText);
        sendText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    send.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_send));
                    send.setText("Отправить сообщение");
                } else {
                    send.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_mic_black_24dp));
                    send.setText("Начать запись голоса");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        relativeLayout = root.findViewById(R.id.chatBottom);
        recyclerView = root.findViewById(R.id.chatView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        chats = new ArrayList<>();
        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                speech.startListening(recognizerIntent);
                isRecording = true;
            } else {
                isRecording = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Нет прав на запись речи", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String sender, String receiver, String message, boolean fromSupport) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("fromSupport", fromSupport);
        hashMap.put("date", Calendar.getInstance().getTime());
        AtomicBoolean flag = new AtomicBoolean(true);
        new Thread(() -> {
            Log.d(LOG_TAG, "send start");
            try {
                TimeStamp date = new RequestTime().execute().get();
                if (date != null) {
                    hashMap.remove("date");
                    hashMap.put("date", date.getDate());
                    Log.d(LOG_TAG, "send date change");
                }
            } catch (Throwable e) {
                Log.d(LOG_TAG, "send err");
                e.printStackTrace();
            } finally {
                Log.d(LOG_TAG, "send done");
                user.addMessage(hashMap, fromSupport).addOnSuccessListener(s -> flag.set(false));
            }
            Log.d("TAGAA", hashMap.get("date").toString());
        }).start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (flag.get())
                    user.addMessage(hashMap, fromSupport).addOnSuccessListener(s -> flag.set(false));
            }
        }, 2000);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (speech != null) {
            speech.destroy();
            Log.i(LOG_TAG, "destroy");
        }
    }

    @Override
    public void onDestroy() {
        if (messageListener != null)
            messageListener.remove();
        super.onDestroy();
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        progressBar.setIndeterminate(true);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        progressBar.setVisibility(View.INVISIBLE);
        isRecording = false;
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        assert matches != null;
        sendText.setText(matches.get(0));
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
        isRecording = false;
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        progressBar.setProgress((int) rmsdB);
        if (progressBar.getVisibility() == View.INVISIBLE)
            speech.stopListening();
    }

    private static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
}