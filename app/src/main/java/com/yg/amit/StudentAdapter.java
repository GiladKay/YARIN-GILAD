package com.yg.amit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private List<Student> originalList;
    private List<Student> tempList;
    private CustomFilter cf = new CustomFilter();

    public StudentAdapter(@NonNull Context context, @NonNull List<Student> objects) {
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

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.student_layout, parent, false);

        TextView tvName = (TextView) view.findViewById(R.id.tvName);
        TextView tvMeetingC = (TextView) view.findViewById(R.id.tvMCount);

        Student temp = originalList.get(position);

        tvName.setText(temp.getName());
        tvMeetingC.setText(temp.getMeetingCount() + "/2");

        if (temp.getMeetingCount() >= 2) {
            tvName.setTextColor(Color.GRAY);
            tvMeetingC.setTextColor(Color.GRAY);
        }



        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {

        if(cf==null){
            cf=new CustomFilter();
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
                ArrayList<Student> filters = new ArrayList<>();

                for (int i = 0 ; i<tempList.size();i++) {
                    if (tempList.get(i).getName().toLowerCase().contains(filterString)) {
                        Student student1 = new Student(tempList.get(i).getName(), tempList.get(i).getMeetingCount());
                        filters.add(student1);
                    }
                }

                results.count = filters.size();
                results.values = filters;
            }

            else {
                results.count = tempList.size();
                results.values = tempList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            originalList=(ArrayList<Student>)filterResults.values;
            notifyDataSetChanged();
        }

    }
}
