package sevak.hovhannisyan.myproject.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

import sevak.hovhannisyan.myproject.data.converter.DateConverter;

/**
 * Transaction entity representing a financial transaction in the database.
 * Supports both income and expense types and is associated with a specific user.
 */
@Entity(tableName = "transactions")
@TypeConverters(DateConverter.class)
public class Transaction {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String userId; // Added to associate transaction with a user
    
    private double amount;
    
    private String category;
    
    private Date date;
    
    @TransactionType
    private String type; // "INCOME" or "EXPENSE"
    
    private String description;
    
    public Transaction() {
    }
    
    public Transaction(String userId, double amount, String category, Date date, @TransactionType String type, String description) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.type = type;
        this.description = description;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    @TransactionType
    public String getType() {
        return type;
    }
    
    public void setType(@TransactionType String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
