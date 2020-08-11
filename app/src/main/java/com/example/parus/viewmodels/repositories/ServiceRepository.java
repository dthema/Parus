package com.example.parus.viewmodels.repositories;

import android.content.Context;
import android.content.Intent;

import com.example.parus.services.OnlineService;
import com.example.parus.services.WorkService;
import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ServiceRepository {

    private static ServiceRepository repository;

    private ServiceRepository() {}

    public synchronized static ServiceRepository getInstance(){
        if (repository == null) repository = new ServiceRepository();
        return repository;
    }

    public void startWorkService(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Intent intent = new Intent(context, WorkService.class);
        intent.putExtra("uid", userId);
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s->{
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    intent.putExtra("linkUid", user.getLinkUserId());
                    intent.putExtra("isSupport", user.isSupport());
                    context.startService(intent);
                });
    }

    public void stopWorkService(Context context){
        Intent intent = new Intent(context, WorkService.class);
        context.stopService(intent);
    }

    public void startOnlineService(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    if (s != null) {
                        User user = s.toObject(User.class);
                        if (user == null)
                            return;
                        if (!user.getUserId().equals(user.getLinkUserId()) && !OnlineService.isServiceRunning) {
                            Intent intent = new Intent(context, OnlineService.class).setAction("action");
                            intent.putExtra("uid", userId);
                            context.startService(intent);

                        }
                    }
                });
    }

    public void stopOnlineService(Context context){
        Intent intent = new Intent(context, OnlineService.class);
        context.stopService(intent);
    }

}
