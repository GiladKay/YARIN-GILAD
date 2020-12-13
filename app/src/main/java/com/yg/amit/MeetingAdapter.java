package com.yg.amit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class MeetingAdapter extends ArrayAdapter<Meeting> {

    Context context;
    List<Meeting> objects;

    private SharedPreferences sharedPreferences;
    private String type;

    public MeetingAdapter(Context context, int resource, int textViewResourceId, List<Meeting> objects) {
        super(context, resource, textViewResourceId, objects);

        this.context = context;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.meeting_layout, parent, true);

        sharedPreferences = context.getSharedPreferences(Menu.AMIT_SP, MODE_PRIVATE);

        TextView tvPerson = (TextView) view.findViewById(R.id.tvPerson);
        TextView tvDate = (TextView) view.findViewById(R.id.tvDate);
        TextView tvTime = (TextView) view.findViewById(R.id.tvTime);
        Button btnMore = (Button) view.findViewById(R.id.btnMore);

        Meeting temp = objects.get(position);

        tvPerson.setText(temp.getPerson());
        tvDate.setText(temp.getDate());
        tvTime.setText(temp.getTime());

        type = sharedPreferences.getString(Menu.TYPE_KEY, "student");

        if (type.equals("student"))
            btnMore.setVisibility(View.GONE);
        else {
            // TODO move to a meeting activity
            //context.startActivity(new Intent(context, <ACTIVITY>));
        }

        return view;
    }
}
