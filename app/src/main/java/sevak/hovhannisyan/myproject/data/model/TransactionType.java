package sevak.hovhannisyan.myproject.data.model;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define valid transaction types.
 */
@StringDef({TransactionType.INCOME, TransactionType.EXPENSE})
@Retention(RetentionPolicy.SOURCE)
public @interface TransactionType {
    String INCOME = "INCOME";
    String EXPENSE = "EXPENSE";
}
