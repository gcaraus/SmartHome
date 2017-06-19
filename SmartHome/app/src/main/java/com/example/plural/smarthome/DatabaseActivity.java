package com.example.plural.smarthome;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//
public class DatabaseActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, onTaskCompletion, DatePickerFragment.updateDateDialogListener{
    ListView mainListView;
    onTaskCompletion mCompletion;
    List<ListViewItem> mainList= new ArrayList<ListViewItem>();
    Activity context;
    CustomListViewAdapter adapter;
    Calendar currentDate;
    final Handler handler = new Handler();
    TextView dateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view_activity);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        //temps = (TextView)findViewById(R.id.temp_text);
//        setSupportActionBar(toolbar);

        context=this;
        mainListView = (ListView) findViewById(R.id.listView);
        mCompletion=this;

        currentDate=Calendar.getInstance();
        Log.v("Current Calendar", String.valueOf(currentDate.getTime()));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(currentDate.getTime());
        Log.v("Formatted Calendar", formattedDate);

        new AsyncReceiveDatabase(this,1,mCompletion).execute(formattedDate + "%");

        dateView = (TextView) findViewById(R.id.dateView);
        dateView.setText(formattedDate);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab!=null)
        {
            //Set listener for floating action button
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getFragmentManager(), "datePicker");
            }
        });
        }


        adapter=new CustomListViewAdapter(this,mainList);
        mainListView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //Click is not implemented
    }

    //Called when the list with the temperatures was loaded
    public void onTaskCompleted(List<ListViewItem> mainList) {
        if(mainList!=null)
        {
            this.mainList=mainList;
            handler.post(notifyAdapter);
        }
    }



    private final Runnable notifyAdapter = new Runnable(){
        public void run(){
            adapter=new CustomListViewAdapter(context,mainList);
            mainListView.setAdapter(adapter);
            //adapter.notifyDataSetInvalidated();
            //adapter.notifyDataSetChanged();
            mainListView.invalidateViews();

        }
    };

    //Called when the user finished editing the dialog and returns the chosen value in integers
    @Override
    public void onFinishEditDialog(int year, int month, int day) {
        String monthEdited;
        String dayEdited;
        monthEdited=String.valueOf(month+1);
        dayEdited=String.valueOf(day);
        monthEdited=monthEdited.length()<2?"0"+monthEdited:monthEdited;
        dayEdited=dayEdited.length()<2?"0"+dayEdited:dayEdited;
        new AsyncReceiveDatabase(this,1,mCompletion).execute(year+"-"+monthEdited+"-"+dayEdited + "%");
        dateView.setText(year + "-" + monthEdited + "-" + dayEdited);
        Log.v("Picker result", year+"-"+month+"-"+day);
    }

    static class ListViewItem
    {
        public String Title;
        public String Subtitle;

        public ListViewItem(String Title, String Subtitle)
        {
            this.Title=Title;
            this.Subtitle=Subtitle;
        }
    }


}
