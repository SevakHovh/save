package sevak.hovhannisyan.myproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.tvMessage.setText(message.getText());

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.cardMessage.getLayoutParams();
        if (message.isUser()) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            holder.cardMessage.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            holder.tvMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.on_primary));
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            holder.cardMessage.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_container));
            holder.tvMessage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.on_secondary_container));
        }
        holder.cardMessage.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        MaterialCardView cardMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            cardMessage = itemView.findViewById(R.id.card_message);
        }
    }
}
