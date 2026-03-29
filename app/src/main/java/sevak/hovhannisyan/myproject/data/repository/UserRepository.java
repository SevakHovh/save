package sevak.hovhannisyan.myproject.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Repository for user-specific settings like salary, fixed expenses, and goals.
 * Uses Firebase Firestore for persistence.
 */
@Singleton
public class UserRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private static final String COLLECTION_USERS = "users";
    private static final String FIELD_SALARY = "salary";
    private static final String FIELD_FIXED_EXPENSES = "fixedExpenses";
    private static final String FIELD_GOAL_AMOUNT = "goalAmount";
    private static final String FIELD_GOAL_START_BALANCE = "goalStartBalance";
    private static final String FIELD_GOAL_START_TIME = "goalStartTime";
    private static final String FIELD_GOAL_END_TIME = "goalEndTime";
    private static final String FIELD_EXCLUDED_DATES = "excludedDates";
    private static final String FIELD_COMPLETED_DATES = "completedDates";

    @Inject
    public UserRepository(FirebaseFirestore firestore, FirebaseAuth auth) {
        this.firestore = firestore;
        this.auth = auth;
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void saveUserFinancialData(double salary, double fixedExpenses) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_SALARY, salary);
        data.put(FIELD_FIXED_EXPENSES, fixedExpenses);

        firestore.collection(COLLECTION_USERS).document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    public void saveGoalAmount(double goalAmount, double currentBalance, long endTime) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_GOAL_AMOUNT, goalAmount);
        data.put(FIELD_GOAL_START_BALANCE, currentBalance);
        data.put(FIELD_GOAL_START_TIME, System.currentTimeMillis());
        data.put(FIELD_GOAL_END_TIME, endTime);
        // Reset progress when setting a new goal
        data.put(FIELD_EXCLUDED_DATES, new java.util.ArrayList<Long>());
        data.put(FIELD_COMPLETED_DATES, new java.util.ArrayList<Long>());

        firestore.collection(COLLECTION_USERS).document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    public void updateExcludedDates(List<Long> dates) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_EXCLUDED_DATES, dates);

        firestore.collection(COLLECTION_USERS).document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    public void markDateAsCompleted(long dateInMillis) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        firestore.collection(COLLECTION_USERS).document(userId)
                .update(FIELD_COMPLETED_DATES, FieldValue.arrayUnion(dateInMillis));
    }

    public LiveData<Map<String, Object>> getUserData() {
        MutableLiveData<Map<String, Object>> userData = new MutableLiveData<>();
        String userId = getCurrentUserId();
        if (userId == null) return userData;

        firestore.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && value.exists()) {
                        userData.setValue(value.getData());
                    } else {
                        userData.setValue(new HashMap<>());
                    }
                });
        return userData;
    }
}
