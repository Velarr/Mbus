package com.example.mbus.utils;

import android.util.Log;

import com.example.mbus.listeners.ServerTimeCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ServerTimeFetcher {

    public static void fetchServerTime(ServerTimeCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("serverTime").document("now");

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());

        ref.set(data)
                .addOnSuccessListener(unused -> {
                    ref.get().addOnSuccessListener(snapshot -> {
                        Timestamp ts = snapshot.getTimestamp("timestamp");
                        if (ts != null) {
                            callback.onTimeReceived(ts.toDate());
                            Log.d("ServerTimeFetcher", "Server time received: " + ts.toDate());
                        } else {
                            callback.onError(new Exception("Timestamp is null"));
                        }
                    }).addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
}
