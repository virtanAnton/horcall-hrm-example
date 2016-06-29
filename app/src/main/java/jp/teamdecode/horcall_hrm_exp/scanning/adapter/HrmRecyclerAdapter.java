package jp.teamdecode.horcall_hrm_exp.scanning.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jp.teamdecode.horcall_hrm_exp.R;
import jp.teamdecode.horcall_hrm_exp.scanning.model.PolarHrmModel;

/**
 * Created by Akadem on 27.06.2016.
 */
public class HrmRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private final String TAG = getClass().getSimpleName();

    public interface HrmClickListener {
        void onHrmClick(PolarHrmModel hrm);
    }

    private HrmClickListener mClickListener;

    public void setClickListener(HrmClickListener clickListener) {
        mClickListener = clickListener;
    }

    private final List<PolarHrmModel> mList;

    public HrmRecyclerAdapter() {
        mList = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_hrm, parent, false);
        return new HrmItem(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        HrmItem item = (HrmItem) holder;
        item.name.setText(mList.get(position).getDevice().getName());
        item.address.setText(mList.get(position).getDevice().getAddress());
        if (mList.get(position).isBounded()) {
            item.isBounded.setVisibility(View.VISIBLE);
        } else {
            item.isBounded.setVisibility(View.INVISIBLE);
        }
        item.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) {
                    mClickListener.onHrmClick(mList.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public void addHrm(PolarHrmModel hrm) {
        if (!mList.contains(hrm)) {
            Log.d(TAG, "onAddHrm: HRM founded: Name: " + hrm.getDevice().getName() + "; Address: " + hrm.getDevice().getAddress());
            mList.add(hrm);
            notifyItemInserted(mList.size());
        }
    }


    public class HrmItem extends RecyclerView.ViewHolder {
        LinearLayout item;
        TextView name;
        TextView address;
        TextView isBounded;

        public HrmItem(View itemView) {
            super(itemView);
            item = (LinearLayout) itemView.findViewById(R.id.recycler_item);
            name = (TextView) itemView.findViewById(R.id.name);
            address = (TextView) itemView.findViewById(R.id.address);
            isBounded = (TextView) itemView.findViewById(R.id.bounded);
        }
    }

}
