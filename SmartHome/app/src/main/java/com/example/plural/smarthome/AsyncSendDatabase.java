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

/**
 * Created by Andrii on 22-Mar-16.
 */

//extended class of asyncTask to send data to database
    //uses php scripts on server
    //passes the value through post method
public class AsyncSendDatabase extends AsyncTask<String,Void,String>

    {
        public AsyncSendDatabase() {

    }

    protected void onPreExecute(){

    }

    @Override
    protected String doInBackground(String... arg0) {

            try{
                //Getting arguments passed at .execute()
                String temp = (String)arg0[0];
                Log.v("DB value", temp);
                //Setting Values
                String link="http://asa.fawlty.nl/2014andrii/cavetempsend.php";
                String data  = URLEncoder.encode("temp", "UTF-8") + "=" + URLEncoder.encode(temp, "UTF-8");


                URL url = new URL(link);
                URLConnection conn = url.openConnection();

                //Sets the output as enabled
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write( data );
                wr.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

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

    @Override
    protected void onPostExecute(String result){
        if(result.equals("success"))
        {
            Log.v("DB result", "success");
        }
        else
        {
            Log.v("DB result", result);
        }

    }
}
