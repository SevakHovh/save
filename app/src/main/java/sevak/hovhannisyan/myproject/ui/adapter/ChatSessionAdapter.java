package sevak.hovhannisyan.myproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sevak.hovhannisyan.myproject.R;
import sevak.hovhannisyan.myproject.data.model.ChatSession;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    private List<ChatSession> sessions = new ArrayList<>();
    private final OnSessionClickListener clickListener;
    private final OnSessionDeleteListener deleteListener;

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
    }

    public interface OnSessionDeleteListener {
        void onSessionDelete(ChatSession session);
    }

    public ChatSessionAdapter(OnSessionClickListener clickListener, OnSessionDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void setSessions(List<ChatSession> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        holder.tvTitle.setText(session.getTitle());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(session.getLastTimestamp())));

        holder.itemView.setOnClickListener(v -> clickListener.onSessionClick(session));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onSessionDelete(session));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        ImageButton btnDelete;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_session_title);
            tvDate = itemView.findViewById(R.id.tv_session_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_session);
        }
    }
}
