package com.example.parus.ui.account;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parus.LoginActivity;
import com.example.parus.R;
import com.example.parus.data.User;
import com.example.parus.services.HeartRateService;
import com.example.parus.services.GeoLocationService;
import com.example.parus.services.OnlineService;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;

public class AccountFragment extends Fragment {

    private TextView name;
    private TextView linkLabel;
    private TextView linkName;
    private Button link;
    private User user;
    private ListenerRegistration listenerRegistration;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_account, container, false);
        user = new User();
        name = root.findViewById(R.id.infoName);
        TextView id = root.findViewById(R.id.infoId);
        id.setText(user.getUser().getUid());
        // копирование ID при долгом нажатии
        id.setOnLongClickListener(l -> {
            ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ID пользователя", user.getUser().getUid());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getActivity(), "ID скопирован в буфер обмена", Toast.LENGTH_LONG).show();
            return true;
        });
        linkLabel = root.findViewById(R.id.infoLinkNameLabel);
        linkName = root.findViewById(R.id.infoLinkName);
        link = root.findViewById(R.id.accountLink);
        ImageButton exitAcc = root.findViewById(R.id.btnExitAccount);
        exitAcc.setOnClickListener(e -> {
            // отключение уведомлений(token) и всех служб перед выходом из аккаунта
            user.update("token", "");
            requireActivity().stopService(new Intent(requireActivity(), HeartRateService.class));
            requireActivity().stopService(new Intent(requireActivity(), GeoLocationService.class));
            requireActivity().stopService(new Intent(requireActivity(), OnlineService.class));
            user.singOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.putExtra("del", true);
            startActivity(intent);
            requireActivity().finish();
        });
        ImageButton settings = root.findViewById(R.id.accountSettings);
        settings.setOnClickListener(l -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // real-time обновление данных
        listenerRegistration = user.getDatabase().collection("users").document(user.getUser().getUid()).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.d("Account", e.getMessage());
                return;
            }
            if (documentSnapshot == null)
                return;
            String uName = documentSnapshot.getString("name");
            name.setText(uName);
            String linkId = documentSnapshot.getString("linkUserId");
            Boolean isSupport = documentSnapshot.getBoolean("isSupport");
            if (linkId != null && isSupport != null) {
                if (user.getUser().getUid().equals(linkId)) {
                    // привязывание другого пользователя
                    link.setOnClickListener(l -> {
                        DialogLinkUser dialogLinkUser = new DialogLinkUser();
                        dialogLinkUser.show(requireActivity().getSupportFragmentManager(), "DialogLinkUser");
                    });
                    if (isSupport) {
                        linkLabel.setText("Имя подопечного:");
                        linkName.setText("Нет связи с подопечным");
                        link.setText("Связать аккаунт с подопечным");
                    } else {
                        linkLabel.setText("Имя помощника:");
                        linkName.setText("Нет связи с помощником");
                        link.setText("Связать аккаунт с помощником");
                    }
                } else {
                    if (isSupport) {
                        linkLabel.setText("Имя подопечного:");
                    } else {
                        linkLabel.setText("Имя помощника:");
                    }
                    link.setText("Разорвать связь");
                    // отвязывание от другого пользователя
                    link.setOnClickListener(l -> {
                        requireActivity().stopService(new Intent(requireActivity(), OnlineService.class));
                        user.getDatabase().collection("users").document(linkId).update("linkUserId", linkId);
                        user.update("linkUserId", user.getUser().getUid());
                        if (isSupport)
                            WorkManager.getInstance(requireContext()).cancelAllWork();
                    });
                    user.getDatabase().collection("users").document(linkId).get()
                            .addOnSuccessListener(s -> linkName.setText(s.getString("name")));
                }
            }
        });
    }

    @Override
    public void onPause() {
        if (listenerRegistration != null)
            listenerRegistration.remove();
        super.onPause();
    }
}
