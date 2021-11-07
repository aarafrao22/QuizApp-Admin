package com.nanosoft22.mcquizadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;


public class GridAdapter extends BaseAdapter {

    public int sets = 0;
    private String category;
    private GridListener listener;

    public GridAdapter(int sets, String category,GridListener listener) {
        this.sets = sets;
        this.listener = listener;
        this.category = category;
    }

    @Override
    public int getCount() {
        return sets+1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_item, parent, false);
            } else {
            view = convertView;
        }
        if (position == 0){
            ((TextView)view.findViewById(R.id.text_view)).setText("+");
        }else {
            ((TextView)view.findViewById(R.id.text_view)).setText(String.valueOf(position));
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position == 0){
                    listener.addSet();
                }else {
                Intent  questionIntent = new Intent(parent.getContext(),QuestionsActivity.class);
                                questionIntent.putExtra("category",category);
                                  questionIntent.putExtra("setNo",position);
                                parent.getContext().startActivity(questionIntent);
                }
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (position != 0) {

                    listener.onLongClick(position);
                }
                return false;
            }
        });

        return view;
    }
    public interface GridListener{
        public void addSet();

        void onLongClick(int setNo);
    }
}
