package com.yg.amit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class StudentAdapter extends ArrayAdapter<Student> {

    private Context context;
    private List<Student> objects;

    public StudentAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<Student> objects) {
        super(context, resource, textViewResourceId, objects);

        this.context = context;
        this.objects = objects;
    }

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.student_layout, parent, false);

        TextView tvName = (TextView) view.findViewById(R.id.tvName);
        TextView tvMeetingC = (TextView) view.findViewById(R.id.tvMCount);

        Student temp = objects.get(position);

        tvName.setText(temp.getName());
        tvMeetingC.setText(temp.getMeetingCount() + "/2");

        if (temp.getMeetingCount() >= 2) {
            tvName.setTextColor(Color.GRAY);
            tvMeetingC.setTextColor(Color.GRAY);
        }

        return view;
    }
}
