package com.example.parus.viewmodels.repositories;

import android.content.Context;
import android.content.Intent;

import com.example.parus.services.WorkService;
import com.example.parus.viewmodels.data.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class WorkRepository {

    public WorkRepository() {}

    public void startService(Context context) {
        Intent intent = new Intent(context, WorkService.class);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

    public void stopService(Context context){
        Intent intent = new Intent(context, WorkService.class);
        context.stopService(intent);
    }
}
