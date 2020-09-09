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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.parus.R;
import com.example.parus.databinding.FragmentChatBinding;
import com.example.parus.services.MyFirebaseMessagingService;
import com.example.parus.viewmodels.ChatViewModel;
import com.example.parus.viewmodels.NetworkViewModel;
import com.example.parus.viewmodels.UserViewModel;
import com.example.parus.viewmodels.data.models.Chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatFragment extends Fragment implements RecognitionListener {

    private static final String LOG_TAG = "Chat";
    private UserViewModel userViewModel;
    private NetworkViewModel networkViewModel;
    private ChatViewModel chatViewModel;
    private MessageAdapter messageAdapter;
    private SpeechRecognizer speech;
    private Intent recognizerIntent;
    private boolean isRecording;
    private static final int REQUEST_RECORD_PERMISSION = 100;
    private FragmentChatBinding binding;

    @Override
    public void onPause() {
        super.onPause();
        MyFirebaseMessagingService.inChat = false;
        stopCheckNetwork();
    }

    @Override
    public void onResume() {
        super.onResume();
        MyFirebaseMessagingService.inChat = true;
        startCheckNetwork();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // не отслеживать интернет соединение
            stopCheckNetwork();
            // не показывать уведомления, если чат активен
            MyFirebaseMessagingService.inChat = false;
            isRecording = false;
            binding.chatProgressBar.setVisibility(View.GONE);
            speech.stopListening();
        } else {
            // отслеживать интернет соединение
            startCheckNetwork();
            // показывать уведомления, если чат не активен
            MyFirebaseMessagingService.inChat = true;
        }
    }

    private void InternetOff() {
        binding.chatInternet.setVisibility(View.VISIBLE);
        binding.chatBottom.setVisibility(View.GONE);
        binding.chatView.setVisibility(View.GONE);
    }

    private void InternetOn() {
        binding.chatInternet.setVisibility(View.GONE);
        binding.chatBottom.setVisibility(View.VISIBLE);
        binding.chatView.setVisibility(View.VISIBLE);
    }

    private void setNetworkActive() {
        requireActivity().runOnUiThread(this::InternetOn);
    }

    private void setNetworkDisable() {
        requireActivity().runOnUiThread(this::InternetOff);
    }

    private void startCheckNetwork() {
        LiveData<Boolean> liveData = networkViewModel.getInternetConnection();
        if (liveData != null)
            liveData.observe(getViewLifecycleOwner(), isInternetConnected -> {
                if (isInternetConnected)
                    setNetworkActive();
                else
                    setNetworkDisable();
            });
    }

    private void stopCheckNetwork() {
        networkViewModel.stopCheckInternetConnection();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        isRecording = false;
        binding = FragmentChatBinding.inflate(inflater, container, false);
        speech = SpeechRecognizer.createSpeechRecognizer(getContext());
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "ru");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        binding.chatText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.chatSend.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_send));
                    binding.chatSend.setText("Отправить сообщение");
                } else {
                    binding.chatSend.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_mic_black_24dp));
                    binding.chatSend.setText("Начать запись голоса");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.chatView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        binding.chatView.setLayoutManager(linearLayoutManager);
        initViewModels();
        initObservers();
        return binding.getRoot();
    }

    private void initViewModels() {
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        networkViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(NetworkViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    private void initObservers() {
        userViewModel.getShortUserData().observe(getViewLifecycleOwner(), pair -> {
            if (pair.first == null)
                return;
            String userId = pair.first.first;
            String linkUserId = pair.first.second;
            Boolean isSupport = pair.second;
            if (userId == null || linkUserId == null || isSupport == null)
                return;
            if (userId.equals(linkUserId)) {
                LiveData<List<Chat>> liveData = chatViewModel.getMessageData();
                if (liveData != null) {
                    liveData.removeObservers(getViewLifecycleOwner());
                    messageAdapter.submitList(new ArrayList<>());
                }
                chatViewModel.setLinkUser(null, false);
                if (isSupport)
                    binding.chatSend.setOnClickListener(c ->
                            Toast.makeText(requireContext(), getString(R.string.no_support_link), Toast.LENGTH_LONG).show());
                else
                    binding.chatSend.setOnClickListener(c ->
                            Toast.makeText(requireContext(), getString(R.string.no_disabled_link), Toast.LENGTH_LONG).show());
            } else {
                messageAdapter = new MessageAdapter(new ChatDiffCallback());
                chatViewModel.setLinkUser(linkUserId, isSupport);
                binding.chatView.setAdapter(messageAdapter);
                chatViewModel.getMessageData().observe(getViewLifecycleOwner(), chats -> {
                    messageAdapter.submitList(new ArrayList<>(chats));
                    if (messageAdapter.getCurrentList().size() > 0)
                        binding.chatView.postDelayed(() -> binding.chatView.smoothScrollToPosition(messageAdapter.getItemCount() - 1),
                                100);
                });
                binding.chatSend.setOnClickListener(c -> {
                    if (!binding.chatText.getText().toString().trim().equals("")) {
                        chatViewModel.sendMessage(binding.chatText.getText().toString(), isSupport);
                        binding.chatText.setText("");
                    } else {
                        // ввод текста голосом
                        if (isRecording) { // завершить ввод голосом
                            speech.stopListening();
                            binding.chatProgressBar.setIndeterminate(false);
                            binding.chatProgressBar.setVisibility(View.INVISIBLE);
                            isRecording = false;
                        } else { // начать ввод голосом
                            binding.chatProgressBar.setVisibility(View.VISIBLE);
                            binding.chatProgressBar.setIndeterminate(true);
                            if (ActivityCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(requireContext(),
                                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                binding.chatProgressBar.setVisibility(View.GONE);
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
            }
        });
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
                binding.chatProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Нет прав на запись речи", Toast.LENGTH_SHORT).show();
            }
        }
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
        super.onDestroy();
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        binding.chatProgressBar.setIndeterminate(false);
        binding.chatProgressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        binding.chatProgressBar.setIndeterminate(true);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        binding.chatProgressBar.setVisibility(View.INVISIBLE);
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
        binding.chatText.setText(matches.get(0));
        binding.chatProgressBar.setIndeterminate(false);
        binding.chatProgressBar.setVisibility(View.INVISIBLE);
        isRecording = false;
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        binding.chatProgressBar.setProgress((int) rmsdB);
        if (binding.chatProgressBar.getVisibility() == View.INVISIBLE)
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