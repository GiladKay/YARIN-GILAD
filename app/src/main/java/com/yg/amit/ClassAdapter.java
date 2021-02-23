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
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ClassAdapter extends ArrayAdapter<Class> {

    private Context context;
    private List<Class> objects;



    public ClassAdapter(Context context, int resource, int textViewResourceId, List<Class> objects) {
        super(context, resource, textViewResourceId,objects);
        this.objects=objects;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.class_layout, parent, false);

        TextView tvClass = (TextView) view.findViewById(R.id.tvClass);

        String temp = objects.get(position).getClassName();

        tvClass.setText(temp);

        return view;
    }
}
