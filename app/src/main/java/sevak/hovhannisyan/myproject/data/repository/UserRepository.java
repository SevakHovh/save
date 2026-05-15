package sevak.hovhannisyan.myproject.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Repository for user-specific settings.
 * Optimized for immediate local feedback and background cloud sync.
 */
@Singleton
public class UserRepository {

    private static final String TAG = "UserRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final SharedPreferences prefs;
    private final Gson gson;

    private static final String COLLECTION_USERS = "users";
    private static final String PREFS_NAME = "user_data_v9"; // Bumped version for clean state
    
    public static final String FIELD_SALARY = "salary";
    public static final String FIELD_FIXED_EXPENSES = "fixedExpenses";
    public static final String FIELD_GOAL_AMOUNT = "goalAmount";
    public static final String FIELD_GOAL_START_BALANCE = "goalStartBalance";
    public static final String FIELD_GOAL_START_TIME = "goalStartTime";
    public static final String FIELD_GOAL_END_TIME = "goalEndTime";
    public static final String FIELD_EXCLUDED_DATES = "excludedDates";
    public static final String FIELD_COMPLETED_DATES = "completedDates";

    private final MutableLiveData<Map<String, Object>> userDataLiveData = new MutableLiveData<>(new HashMap<>());
    private ListenerRegistration firestoreListener;

    @Inject
    public UserRepository(FirebaseFirestore firestore, FirebaseAuth auth, @ApplicationContext Context context) {
        this.firestore = firestore;
        this.auth = auth;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        // Load whatever we have in local storage into the LiveData immediately
        Map<String, Object> initial = getAllLocalData();
        Log.d(TAG, "Initialized with local data: " + initial);
        userDataLiveData.setValue(initial);
        
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                attachFirestoreListener(user.getUid());
            } else {
                detachFirestoreListener();
                userDataLiveData.postValue(new HashMap<>());
            }
        });
    }

    private void attachFirestoreListener(String userId) {
        detachFirestoreListener();
        firestoreListener = firestore.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Firestore sync offline: " + error.getMessage());
                        return;
                    }
                    if (value != null && value.exists()) {
                        Map<String, Object> data = value.getData();
                        if (data != null) {
                            Log.d(TAG, "Received cloud update: " + data);
                            saveAllLocally(data);
                        }
                    }
                });
    }

    private void detachFirestoreListener() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private synchronized void saveAllLocally(Map<String, Object> data) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                editor.remove(key);
            } else if (value instanceof List) {
                editor.putString(key, gson.toJson(value));
            } else {
                // Save everything else as string to avoid ClassCastException on retrieval
                editor.putString(key, String.valueOf(value));
            }
        }
        editor.apply();
        
        // Notify observers on next loop
        userDataLiveData.postValue(getAllLocalData());
    }

    private synchronized Map<String, Object> getAllLocalData() {
        Map<String, Object> map = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (key.equals(FIELD_EXCLUDED_DATES) || key.equals(FIELD_COMPLETED_DATES)) {
                try {
                    Type type = new TypeToken<List<Long>>(){}.getType();
                    List<Long> list = gson.fromJson((String) val, type);
                    map.put(key, list != null ? list : new ArrayList<Long>());
                } catch (Exception e) {
                    map.put(key, new ArrayList<Long>());
                }
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    public void saveUserFinancialData(double salary, double fixedExpenses) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_SALARY, salary);
        update.put(FIELD_FIXED_EXPENSES, fixedExpenses);

        saveAllLocally(update);
        firestore.collection(COLLECTION_USERS).document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge());
    }

    public void saveGoalAmount(double goalAmount, double currentBalance, long endTime) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_GOAL_AMOUNT, goalAmount);
        update.put(FIELD_GOAL_START_BALANCE, currentBalance);
        update.put(FIELD_GOAL_START_TIME, System.currentTimeMillis());
        update.put(FIELD_GOAL_END_TIME, endTime);
        update.put(FIELD_EXCLUDED_DATES, new ArrayList<Long>());
        update.put(FIELD_COMPLETED_DATES, new ArrayList<Long>());

        Log.d(TAG, "Saving Goal: Amount=" + goalAmount + ", EndTime=" + endTime);
        saveAllLocally(update);
        
        firestore.collection(COLLECTION_USERS).document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge());
    }

    public void updateExcludedDates(List<Long> dates) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_EXCLUDED_DATES, dates);
        saveAllLocally(update);

        firestore.collection(COLLECTION_USERS).document(uid)
                .set(update, com.google.firebase.firestore.SetOptions.merge());
    }

    public void markDateAsCompleted(long dateInMillis) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        firestore.collection(COLLECTION_USERS).document(uid)
                .update(FIELD_COMPLETED_DATES, FieldValue.arrayUnion(dateInMillis));
        
        // Immediate local feedback
        Map<String, Object> current = getAllLocalData();
        List<Long> completed = (List<Long>) current.get(FIELD_COMPLETED_DATES);
        if (completed == null) completed = new ArrayList<>();
        else completed = new ArrayList<>(completed);
        
        if (!completed.contains(dateInMillis)) {
            completed.add(dateInMillis);
            Map<String, Object> update = new HashMap<>();
            update.put(FIELD_COMPLETED_DATES, completed);
            saveAllLocally(update);
        }
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public LiveData<Map<String, Object>> getUserData() {
        return userDataLiveData;
    }
}
