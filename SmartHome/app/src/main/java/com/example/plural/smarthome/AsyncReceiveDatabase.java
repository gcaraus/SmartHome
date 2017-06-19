package com.example.plural.smarthome;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import com.example.plural.smarthome.DatabaseActivity.ListViewItem;

/**
 * Created by Andrii on 21-Mar-16.
 */
public class AsyncReceiveDatabase extends AsyncTask<String,Void,String> {
    private Context context;
    private int byGetOrPost = 0;
    onTaskCompletion completion;
    List<ListViewItem> mainList= new ArrayList<ListViewItem>();

    //flag 0 means get and 1 means post.(By default it is get.)
    public AsyncReceiveDatabase(Context context, int flag, onTaskCompletion completion) {
        this.context = context;
        byGetOrPost = flag;
        this.completion=completion;
    }

    protected void onPreExecute(){

    }

    @Override
    protected String doInBackground(String... arg0) {
        if(byGetOrPost == 0){ //means by Get Method

            try{
                String date = (String)arg0[0];
                String link = "http://asa.fawlty.nl/2014andrii/cavetemp.php?date="+date;

                URL url = new URL(link);/
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                request.setURI(new URI(link));
                HttpResponse response = client.execute(request);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                StringBuffer sb = new StringBuffer("");
                String line="";

                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }
                in.close();
                return sb.toString();
            }

            catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }
        else{//Post method
            try{
                String date = (String)arg0[0];

                String link="http://asa.fawlty.nl/2014andrii/cavetemp.php";
                String data  = URLEncoder.encode("date", "UTF-8") + "=" + URLEncoder.encode(date, "UTF-8");


                URL url = new URL(link);
                URLConnection conn = url.openConnection();

                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write( data );
                wr.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line = null;

                // Read Server Response
                while((line = reader.readLine()) != null)
                {
                    sb.append(line);
                    break;
                }
                return sb.toString();
            }
            catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPostExecute(String result){
        String[] data=result.split("/");
        String[] holder;
        String time;
        if(!data[0].equals("")){
        for (int i = 0; i < data.length; i++)
            {
                holder=data[i].split("_");
                time=holder[1].substring(11, holder[1].length());
                mainList.add(new ListViewItem(holder[0]+"°C", time));
                Log.v("DB result", holder[0]+":"+time);
            }
        }
        else
        {
            mainList=null;
        }
        //Interface is called to send list to main activity
        completion.onTaskCompleted(mainList);
        //this.statusField.setText(String.valueOf(data.length));

    }
}