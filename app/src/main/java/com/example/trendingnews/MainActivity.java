package com.example.trendingnews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase articleDB;
    private String versionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER, title VARCHAR, content VARCHAR)");



        DownloadTask task = new DownloadTask();
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),0);
            int currentVersion = info.versionCode;
            this.versionName = info.versionName;
            SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(this);
            int lastVersion = s.getInt("version_code", 0);
            if (currentVersion > lastVersion) {
                s.edit().putInt("version_code", currentVersion).commit();
                task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        ListView listView = findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(i));
                startActivity(intent);
            }
        });

        updateListView();
    }

    public void updateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();

            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }

    }

   public class DownloadTask extends AsyncTask<String,Void,String>{

       @Override
       protected String doInBackground(String... urls) {

           String result = "";
           URL url;
           HttpURLConnection urlConnection = null;
           try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
               InputStream inputStream = urlConnection.getInputStream();
               InputStreamReader reader = new InputStreamReader(inputStream);
               int data = reader.read();

               while (data != -1){
                   char a = (char) data;

                   result +=a;
                   data= reader.read();
               }
                   JSONArray jsonArray = new JSONArray(result);

                    int numberOfIndex =20;
               if(jsonArray.length() < 20){
                   numberOfIndex = jsonArray.length();
               }

               articleDB.execSQL("DELETE FROM articles");

                   for (int i=0;i<numberOfIndex;i++){

                       String articleID = jsonArray.getString(i);
                       url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty");

                       urlConnection = (HttpURLConnection) url.openConnection();
                       inputStream = urlConnection.getInputStream();
                       reader = new InputStreamReader(inputStream);
                       data = reader.read();

                       String articleINfo ="";

                       while (data != -1){
                           char a = (char) data;

                           articleINfo +=a;
                           data= reader.read();
                       }
                       JSONObject jsonObject = new JSONObject(articleINfo);

                       if(! jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                           String articletitle = jsonObject.getString("title");
                           String articleurl = jsonObject.getString("url");

                           String sql = "INSERT INTO articles (articleID,title,content) VALUES (?,?,?)";
                           SQLiteStatement statement = articleDB.compileStatement(sql);
                           statement.bindString(1,articleID);
                           statement.bindString(2,articletitle);
                           statement.bindString(3,articleurl);

                           statement.execute();
                       }

                   }


               Log.i("Url Content",result);
               return  result;

           } catch (Exception e) {
               e.printStackTrace();
           }

           return null;
       }

       @Override
       protected void onPostExecute(String s) {
           super.onPostExecute(s);

           updateListView();
       }
   }
}