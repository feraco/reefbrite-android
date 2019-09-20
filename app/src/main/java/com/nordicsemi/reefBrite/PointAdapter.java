package com.nordicsemi.reefBrite;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

public class PointAdapter extends ArrayAdapter<PointModel> implements View.OnClickListener, Serializable{

    private ArrayList<PointModel> point;
    Context mContext;
    ViewHolder viewHolder; // view lookup cache stored in tag

    // View lookup cache
    private static class ViewHolder {
        TextView timeV;
        TextView bluePercent;
        TextView whitePercent;
        Button editBtn;
        Button deleteBtn;
    }

    public PointAdapter(ArrayList<PointModel> data, Context context) {
        super(context, R.layout.point_detail, data);
        this.point = data;
        this.mContext=context;
    }

    @Override
    public void onClick(View v) {

    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final PointModel pModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null) {
            viewHolder = new ViewHolder();
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.point_detail, parent, false);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();

        }

        viewHolder.timeV = (TextView) convertView.findViewById(R.id.time);
        viewHolder.bluePercent = (TextView) convertView.findViewById(R.id.blue_value);
        viewHolder.whitePercent = (TextView) convertView.findViewById(R.id.white_value);
        viewHolder.editBtn = (Button) convertView.findViewById(R.id.edit);
        viewHolder.deleteBtn = (Button) convertView.findViewById(R.id.delete);
        if(position==0){//the first point cannot be delete
            viewHolder.deleteBtn.setEnabled(false);
            viewHolder.deleteBtn.setTextColor(Color.GRAY);
        }else{
            viewHolder.deleteBtn.setEnabled(true);
            viewHolder.deleteBtn.setTextColor(mContext.getResources().getColor(R.color.btn_color_connected));
        }

        viewHolder.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.indexEdit = point.indexOf(pModel);
                Intent intent = new Intent(v.getContext(),PointActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intent);
            }
        });

        viewHolder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                point.remove(pModel);
                notifyDataSetChanged();
                MainActivity.sendEvent();
            }
        });

        viewHolder.timeV.setText(pModel.getTime());
        viewHolder.bluePercent.setText(pModel.getBluePercent());
        viewHolder.whitePercent.setText(pModel.getWhitePercent());
        return convertView;
    }

    public ArrayList<PointModel> clone(){
        return (ArrayList<PointModel>)point.clone();
    }
}

