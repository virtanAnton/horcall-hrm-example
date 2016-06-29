package jp.teamdecode.horcall_hrm_exp.device.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jp.teamdecode.horcall_hrm_exp.R;

/**
 * Created by Akadem on 27.06.2016.
 */
public class LogRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<String> mList;

    public LogRecyclerAdapter() {
        mList = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_log, parent, false);
        return new LogItem(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LogItem item = (LogItem) holder;
        item.msg.setText(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class LogItem extends RecyclerView.ViewHolder {
        TextView msg;

        public LogItem(View itemView) {
            super(itemView);
            msg = (TextView) itemView.findViewById(R.id.log_text);
        }
    }

    public void addLog(String log) {
        mList.add(log);
        notifyItemInserted(mList.size());
    }

}
