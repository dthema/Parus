package com.begletsov.parus.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.begletsov.parus.R;
import com.begletsov.parus.databinding.FragmentHomeBinding;
import com.begletsov.parus.ui.communication.listen.ListenActivity;
import com.begletsov.parus.ui.communication.say.SayShowActivity;
import com.begletsov.parus.ui.communication.see.SeeActivity;
import com.begletsov.parus.ui.home.map.MapActivity;
import com.begletsov.parus.ui.home.reminder.RemindersActivity;
import com.begletsov.parus.viewmodels.HealthViewModel;
import com.begletsov.parus.viewmodels.HomeViewModel;
import com.begletsov.parus.viewmodels.NetworkViewModel;
import com.begletsov.parus.viewmodels.ReminderViewModel;
import com.begletsov.parus.viewmodels.TTSViewModel;
import com.begletsov.parus.viewmodels.UserViewModel;
import com.begletsov.parus.viewmodels.data.binding.HomeData;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int NO_PERMISSION = 0;
    private static final int NO_GOOGLE_ACCOUNT = 1;
    private static final int SAMSUNG_NO_CONNECT = 3;
    private UserViewModel userViewModel;
    private ReminderViewModel reminderViewModel;
    private NetworkViewModel networkViewModel;
    private HealthViewModel healthViewModel;
    private HomeViewModel homeViewModel;
    private HomeData homeData;
    private FragmentHomeBinding binding;
    private TTSViewModel TTS;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            TTS.stopSpeech();
            stopCheckInternetConnection();
            stopCheckReminders();
        } else {
            startCheckInternetConnection();
            startCheckReminders();
        }
    }

    private void InternetOff() {
        homeData.setLinkUserOnline(getString(R.string.not_internet));
    }

    @SuppressLint("SetTextI18n")
    private void InternetOn() {
        userViewModel.getSingleLinkUserData().observe(getViewLifecycleOwner(), linkUser -> {
            if (linkUser != null) {
                homeViewModel.setLastOnline(linkUser);
            }
        });
    }

    private void InternetOnUI() {
        requireActivity().runOnUiThread(this::InternetOn);
    }

    private void InternetOffUI() {
        requireActivity().runOnUiThread(this::InternetOff);
    }

    @Override
    public void onResume() {
        super.onResume();
        startCheckInternetConnection();
        startCheckReminders();
    }

    private void initModels() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        networkViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(NetworkViewModel.class);
        healthViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HealthViewModel.class);
        TTS = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(TTSViewModel.class);
    }

    private void initObservers() {
        userViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            String userId = user.getUserId();
            String linkUserId = user.getLinkUserId();
            String fastAction = user.getFastAction();
            boolean isSupport = user.isSupport();
            HashMap<String, Object> SaySettings = user.getSaySettings();
            if (linkUserId == null || userId == null)
                return;
            if ((!isSupport || !userId.equals(linkUserId)) && reminderViewModel.getReminderData(false) == null)
                observeReminders(userId, linkUserId, isSupport);
            if (SaySettings.get("TTS_Speed") != null)
                TTS.setSpeed((Double) SaySettings.get("TTS_Speed"));
            if (SaySettings.get("TTS_Pitch") != null)
                TTS.setPitch((Double) SaySettings.get("TTS_Pitch"));
            // быстрое действие
            if (fastAction != null)
                setFastAction(fastAction);
            if (!isSupport) {
                if (!userId.equals(linkUserId)) {
                    binding.homeCallSupport.setVisibility(View.VISIBLE);
                    binding.homeCallSupport.setOnClickListener(l -> userViewModel.callSupport().observe(getViewLifecycleOwner(),
                            send -> Toast.makeText(getContext(), R.string.notification_send, Toast.LENGTH_LONG).show()));
                } else {
                    binding.homeCallSupport.setVisibility(View.GONE);
                }
                // отображение пульса
                healthViewModel.get().observe(getViewLifecycleOwner(), result -> {
                    switch (result) {
                        case NO_PERMISSION:
                            binding.homePulse.setClickable(true);
                            homeData.setHeartRate(getString(R.string.no_pulse_rights));
                            break;
                        case NO_GOOGLE_ACCOUNT:
                            binding.homePulse.setClickable(true);
                            homeData.setHeartRate(getString(R.string.google_account_not_connected));
                            break;
                        case SAMSUNG_NO_CONNECT:
                            binding.homePulse.setClickable(true);
                            homeData.setHeartRate(getString(R.string.samsung_not_connected));
                            break;
                        default:
                            binding.homePulse.setClickable(false);
                            if (user.isCheckHeartBPM()) {
                                homeData.setHeartRate(getString(R.string.no_pulse_data));
                                Long BPM = user.getPulse();
                                if (BPM != null)
                                    if (BPM != 0)
                                        homeData.setHeartRate("Пульс: " + BPM + " у/м");
                            } else
                                homeData.setHeartRate(getString(R.string.no_pulse_checked));
                            break;
                    }
                });
            } else {
                binding.homeCallSupport.setVisibility(View.GONE);
                binding.homePulse.setClickable(false);
                if (linkUserId.equals(userId)) {
                    stopCheckReminders();
                    homeData.setCurrentReminder(getString(R.string.no_reminders));
                    homeData.setHeartRate(getString(R.string.no_support_link));
                    binding.reminderButton.setClickable(false);
                    if (reminderViewModel.getReminderData(false) != null) {
                        reminderViewModel.removeObserver(getViewLifecycleOwner());
                        reminderViewModel.setReminders(null);
                    }
                    if (userViewModel.getOtherUserData() != null)
                        userViewModel.removeLinkObserver(getViewLifecycleOwner());
                } else {
                    startCheckReminders();
                    binding.reminderButton.setClickable(true);
                    if (userViewModel.getOtherUserData() == null)
                        observeLinkUserPulse(linkUserId);
                }
            }
        });
        reminderViewModel.setReminders(new ArrayList<>());
    }

    private void startCheckInternetConnection() {
        LiveData<Boolean> liveData = networkViewModel.getInternetConnection();
        if (liveData != null)
            liveData.observe(getViewLifecycleOwner(), isInternetConnected -> {
                if (isInternetConnected != null) {
                    if (isInternetConnected)
                        InternetOnUI();
                    else
                        InternetOffUI();
                }
            });
    }

    private void stopCheckInternetConnection() {
        networkViewModel.stopCheckInternetConnection();
    }

    private void startCheckReminders() {
        LiveData<String> liveData = reminderViewModel.startCheckReminders();
        if (liveData != null) {
            userViewModel.getSingleLinkUserData().observe(getViewLifecycleOwner(), user -> {
                if (!user.isSupport() || !user.getLinkUserId().equals(user.getUserId())) {
                    liveData.observe(getViewLifecycleOwner(), s -> homeData.setCurrentReminder(s));
                }
            });
        }
    }

    private void stopCheckReminders() {
        reminderViewModel.stopCheckReminders();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
        homeData = new HomeData();
        binding.setData(homeData);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setFragment(this);
        binding.reminderButton.setOnClickListener(view ->
                userViewModel.getSingleShortUserData().observe(getViewLifecycleOwner(), pair -> {
                    if (pair.first == null)
                        return;
                    String userId = pair.first.first;
                    String linkUserId = pair.first.second;
                    Boolean isSupport = pair.second;
                    if (userId == null || linkUserId == null || isSupport == null)
                        return;
                    if (!userId.equals(linkUserId) || !isSupport) {
                        requireActivity().startActivity(new Intent(requireContext(), RemindersActivity.class));
                    }
                }));
        binding.homeMap.setOnClickListener(c -> startActivity(new Intent(getActivity(), MapActivity.class)));
        binding.homeLastOnline.setClickable(false);
        initModels();
        binding.setViewmodel(homeViewModel);
        homeViewModel.setData(homeData);
        initObservers();
        return binding.getRoot();
    }


    @SuppressLint("SetTextI18n")
    private void observeLinkUserPulse(String linkUserId) {
        userViewModel.getOtherUserData(linkUserId, true).observe(getViewLifecycleOwner(), user -> {
            if (user == null)
                return;
            Long BPM = user.getPulse();
            boolean check = user.isCheckHeartBPM();
            if (check) {
                homeData.setHeartRate(getString(R.string.no_pulse_data));
                if (BPM != null)
                    if (BPM != 0)
                        homeData.setHeartRate("Пульс:\n" + BPM + " у/м");
            } else
                homeData.setHeartRate(getString(R.string.no_link_pulse_checked));
            if (binding.homePulse.getHeight() > binding.reminderButton.getHeight())
                binding.reminderButton.setMinHeight(binding.homePulse.getHeight());
            else
                binding.homePulse.setMinHeight(binding.reminderButton.getHeight());
        });
    }

    private void observeReminders(String userId, String linkUserId, boolean isSupport) {
        if (reminderViewModel.getReminderData(false) != null || isSupport && userId.equals(linkUserId))
            return;
        reminderViewModel.getReminderData(userId, linkUserId, isSupport).observe(getViewLifecycleOwner(), reminders -> {
            reminderViewModel.setReminders(reminders);
            homeViewModel.showCurrentReminder(reminders);
        });
    }

    @SuppressLint("SetTextI18n")
    private void setFastAction(String action) {
        switch (action) {
            case "0":
                homeData.setFastAction(getString(R.string.fast_action_no_choose));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "1":
                homeData.setFastAction(getString(R.string.detect_text));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "2":
                homeData.setFastAction(getString(R.string.detect_object));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "3":
                homeData.setFastAction(getString(R.string.start_listen));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            default:
                homeData.setFastAction(getString(R.string.say_and_show) + " " + action);
                binding.homeFast.setOnLongClickListener(l -> {
                    Intent intent = new Intent(getActivity(), SayShowActivity.class);
                    intent.putExtra("word", action);
                    startActivity(intent);
                    return true;
                });
                break;
        }
    }

    @Override
    public void onPause() {
        TTS.stopSpeech();
        stopCheckInternetConnection();
        stopCheckReminders();
        super.onPause();
    }

    public void onHealthClick() {
        healthViewModel.get().observe(getViewLifecycleOwner(), result -> {
            switch (result) {
                case 0:
                    requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 100);
                    break;
                case 1:
                    googleHealthPermissionsAction();
                    FitnessOptions fitnessOptions = FitnessOptions.builder()
                            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                            .build();
                    GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions);
                    GoogleSignIn.requestPermissions(
                            this,
                            1000,
                            account,
                            fitnessOptions);
                    break;
            }
        });
    }

    public void onFastActionClick() {
        if (homeData.getFastAction().equals(getString(R.string.fast_action_no_choose))) {
            DialogChooseFastAction dialogChooseFastAction = new DialogChooseFastAction();
            dialogChooseFastAction.show(requireActivity().getSupportFragmentManager(), "DialogFastAction");
        } else if (homeData.getFastAction().equals(getString(R.string.detect_text))) {
            recognizeTextFastAction();
            Intent intent1 = new Intent(getActivity(), SeeActivity.class);
            intent1.putExtra("fastAction", 1);
            startActivity(intent1);
        } else if (homeData.getFastAction().equals(getString(R.string.detect_object))) {
            recognizeObjectFastAction();
            Intent intent2 = new Intent(getActivity(), SeeActivity.class);
            intent2.putExtra("fastAction", 2);
            startActivity(intent2);
        } else if (homeData.getFastAction().equals(getString(R.string.start_listen))) {
            listenFastAction();
            Intent intent3 = new Intent(getActivity(), ListenActivity.class);
            intent3.putExtra("fastAction", true);
            startActivity(intent3);
        } else {
            sayFastAction();
            TTS.speak(homeData.getFastAction().substring(16));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        healthViewModel.onRequestPermissionsResult(requestCode, permissions, grantResults).observe(getViewLifecycleOwner(), result -> {
            switch (result) {
                case NO_PERMISSION:
                    homeData.setHeartRate(getString(R.string.no_pulse_rights));
                    binding.homePulse.setClickable(true);
                    break;
                case NO_GOOGLE_ACCOUNT:
                    googleHealthPermissionsActionFail();
                    homeData.setHeartRate(getString(R.string.google_account_not_connected));
                    binding.homePulse.setClickable(true);
                    break;
                default:
                    homeData.setHeartRate(getString(R.string.no_pulse_data));
                    binding.homePulse.setClickable(false);
                    break;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        healthViewModel.onActivityResult(requestCode, resultCode, data);
    }

    private void googleHealthPermissionsAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "google-health-permission");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Granted");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void googleHealthPermissionsActionFail() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "google-health-permission-fail");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Not granted");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void recognizeTextFastAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fast-action-text");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Recognize Text");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void recognizeObjectFastAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fast-action-object");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Recognize Object");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void listenFastAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fast-action-listen");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Listen");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void sayFastAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fast-action-say");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Say");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }
}