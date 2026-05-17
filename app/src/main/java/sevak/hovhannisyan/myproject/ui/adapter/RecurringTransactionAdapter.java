package sevak.hovhannisyan.myproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;

public class RecurringTransactionAdapter extends RecyclerView.Adapter<RecurringTransactionAdapter.ViewHolder> {

    private List<RecurringTransaction> list = new ArrayList<>();
    private final OnDeleteListener deleteListener;
    private final NumberFormat moneyFmt = NumberFormat.getCurrencyInstance(Locale.US);

    public interface OnDeleteListener {
        void onDelete(RecurringTransaction rt);
    }

    public RecurringTransactionAdapter(OnDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setList(List<RecurringTransaction> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recurring_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecurringTransaction rt = list.get(position);
        holder.tvDesc.setText(rt.getDescription());
        
        // Fixed: Use getPeriodDays() and the localized period_format string resource
        String periodText = holder.itemView.getContext().getString(R.string.period_format, rt.getPeriodDays());
        holder.tvDetails.setText(String.format("%s • %s", periodText, rt.getCategory()));
        
        double amt = rt.getAmount();
        boolean isExpense = TransactionType.EXPENSE.equals(rt.getType());
        holder.tvAmount.setText(isExpense ? "-" + moneyFmt.format(amt) : "+" + moneyFmt.format(amt));
        holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                isExpense ? R.color.expense_red : R.color.income_green));

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(rt));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvDetails, tvAmount;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tv_recurring_desc);
            tvDetails = itemView.findViewById(R.id.tv_recurring_details);
            tvAmount = itemView.findViewById(R.id.tv_recurring_amount);
            btnDelete = itemView.findViewById(R.id.btn_delete_recurring);
        }
    }
}
