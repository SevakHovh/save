package sevak.hovhannisyan.myproject.data.model;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        TransactionType.INCOME,
        TransactionType.EXPENSE
})
public @interface TransactionType {
    String INCOME = "INCOME";
    String EXPENSE = "EXPENSE";
}
