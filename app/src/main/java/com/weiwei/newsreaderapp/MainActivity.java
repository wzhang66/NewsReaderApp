package com.weiwei.newsreaderapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> newsTitlesList = new ArrayList<>();
    ArrayList<String> newsUrlsList = new ArrayList<>();
    SQLiteDatabase myDatabase;
    ListView listView;
    ArrayAdapter arrayAdapter;

    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            StringBuilder result =new StringBuilder();
            URL url;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(in);
                    BufferedReader bufferedReader = new BufferedReader(reader);

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                } finally {
                    urlConnection.disconnect();
                }
                return result.toString();
            } catch (MalformedURLException e) {
                result.append("Error: MalformedURLException");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result.toString();
        }
    }

    public void downloadLinks(){
        DownloadTask downloadTask = new DownloadTask();
        String dataLink = "https://hacker-news.firebaseio.com/v0/topstories.json";
        String data = null;
        try {
            data = downloadTask.execute(dataLink).get();
            if(data!=""){
                try {
                    JSONArray array = new JSONArray(data);
                    for (int i = 0; i < array.length(); i++){
//                        Log.i("index", array.getString(i));

                        downloadData(array.getString(i));
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void downloadData(String index){
        DownloadTask downloadTask = new DownloadTask();
        String dataLink = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json", index);
        String data = null;
        try {
            data = downloadTask.execute(dataLink).get();
            if(data!=""){
                try {
                    JSONObject object = new JSONObject(data);

                    if(!object.getString("title").equals("") && !object.getString("url").equals("")){
                        // Load data into database
                        String titleData = object.getString("title");
                        titleData.replace("\'","\"" );
                        String urlData = object.getString("url");
                        String command =String.format("INSERT INTO newsTable (title, url) VALUES ('%s', '%s')", titleData, urlData);
                        myDatabase.execSQL(command);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        newsTitlesList = new ArrayList<>();
        myDatabase = this.openOrCreateDatabase("NewsList", Context.MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS newsTable (title VARCHAR, url VARCHAR, id INTEGER PRIMARY KEY)");
//        downloadLinks();

        Cursor c = myDatabase.rawQuery("SELECT * FROM newsTable",null);
        int titleIndex = c.getColumnIndex("title");
        int urlIndex = c.getColumnIndex("url");
        c.moveToFirst();
        if(c.isAfterLast()){
            downloadLinks();
        }

        while(!c.isAfterLast()){
            newsTitlesList.add(c.getString(titleIndex));
            newsUrlsList.add(c.getString(urlIndex));
            c.moveToNext();
        }

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, newsTitlesList);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), NewsActivity.class);
                intent.putExtra("url", newsUrlsList.get(i));
                startActivity(intent);
            }
        });



    }
}