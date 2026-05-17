package sevak.hovhannisyan.myproject.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

import sevak.hovhannisyan.myproject.data.converter.DateConverter;

@Entity(tableName = "recurring_transactions")
@TypeConverters(DateConverter.class)
public class RecurringTransaction {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userId;
    private double amount;
    private String category;
    private String description;
    private String type; // "INCOME" or "EXPENSE"
    private int periodDays; // Interval in days
    private Date lastProcessedDate;
    private boolean active;

    public RecurringTransaction() {
        this.active = true;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPeriodDays() { return periodDays; }
    public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }

    public Date getLastProcessedDate() { return lastProcessedDate; }
    public void setLastProcessedDate(Date lastProcessedDate) { this.lastProcessedDate = lastProcessedDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
