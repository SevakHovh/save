package sevak.hovhannisyan.myproject.ui;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import sevak.hovhannisyan.myproject.di.AppModule;

@Singleton
public class GoalManager {

    private static final String KEY_GOAL_AMOUNT = "goal_amount";
    private static final String KEY_SALARY = "user_salary";
    private static final String KEY_FIXED_EXPENSES = "fixed_expenses";
    
    private final SharedPreferences prefs;

    @Inject
    public GoalManager(@Named(AppModule.GOAL_PREFS) SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void saveGoalAmount(double amount) {
        prefs.edit().putFloat(KEY_GOAL_AMOUNT, (float) amount).apply();
    }

    public double getGoalAmount() {
        return prefs.getFloat(KEY_GOAL_AMOUNT, 0f);
    }

    public void saveSalary(double salary) {
        prefs.edit().putFloat(KEY_SALARY, (float) salary).apply();
    }

    public double getSalary() {
        return prefs.getFloat(KEY_SALARY, 0f);
    }

    public void saveFixedExpenses(double expenses) {
        prefs.edit().putFloat(KEY_FIXED_EXPENSES, (float) expenses).apply();
    }

    public double getFixedExpenses() {
        return prefs.getFloat(KEY_FIXED_EXPENSES, 0f);
    }
}
