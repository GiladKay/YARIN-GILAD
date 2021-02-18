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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
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

public class MeetingAdapter extends BaseAdapter implements Filterable {

    private Context context;

    private List<Meeting> originalList;
    private List<Meeting> tempList;
    private MeetingAdapter.CustomFilter cf = new MeetingAdapter.CustomFilter();

    private SharedPreferences sharedPreferences;
    private String type;

   public MeetingAdapter(@NonNull Context context,@NonNull List<Meeting> objects){
       super();
       this.context = context;
       this.originalList = objects;
       this.tempList = objects;

   }


    @Override
    public int getCount() {
        return originalList.size();
    }

    @Override
    public Object getItem(int i) {
        return originalList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.meeting_layout, parent, false);


        TextView tvPerson = (TextView) view.findViewById(R.id.tvPerson);



        Meeting temp = originalList.get(position);

        sharedPreferences = context.getSharedPreferences(Utils.AMIT_SP, MODE_PRIVATE);
        type = sharedPreferences.getString(Utils.TYPE_KEY, "student");

        if (type.equals("student"))
            tvPerson.setText(temp.getTeacher());
        else if (type.equals("teacher"))
            tvPerson.setText(temp.getStudent());
        else
            tvPerson.setText(temp.getStudent() + " - " + temp.getTeacher());


        return view;
    }


    @NonNull
    @Override
    public Filter getFilter() {

        if (cf == null) {
            cf = new MeetingAdapter.CustomFilter();
        }
        return cf;
    }


    class CustomFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            String filterString;
            FilterResults results = new FilterResults();


            if (charSequence != null && charSequence.length() > 0) {
                filterString = charSequence.toString().toLowerCase();
                ArrayList<Meeting> filters = new ArrayList<>();

                for (int i = 0; i < tempList.size(); i++) {
                    if ((tempList.get(i).getStudent()+" - "+tempList.get(i).getTeacher()).toLowerCase().contains(filterString)) {
                        Meeting meeting1 = new Meeting(tempList.get(i).getStudent(), tempList.get(i).getTeacher(), "", "");
                        filters.add(meeting1);
                    }
                }

                results.count = filters.size();
                results.values = filters;
            } else {
                results.count = tempList.size();
                results.values = tempList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            originalList = (ArrayList<Meeting>) filterResults.values;
            notifyDataSetChanged();
        }
    }
}
