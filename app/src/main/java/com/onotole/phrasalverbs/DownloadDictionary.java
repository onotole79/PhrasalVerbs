package com.onotole.phrasalverbs;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.ref.WeakReference;

import java.net.URL;
import java.net.URLConnection;


class DownloadDictionary extends AsyncTask<String, String, Void> {

    private String newFileName, currFileName;
    private WeakReference<Context> weakContext;

    //создаём инетерфейс, чтобы вызвать метод из MainActivity
    private WeakReference<MainActivityMethods> weakMethods;
    public interface MainActivityMethods {
        void start();
    }


    DownloadDictionary(Activity methods, Context context){
        //context в background thread может дать утечку памяти,
        //поэтому делаем через WeakReference
        weakContext = new WeakReference<>(context);
        weakMethods = new WeakReference<>((MainActivityMethods)methods);
    }



    @Override
    protected Void doInBackground(String... f_url) {
        int count;
        try {
            newFileName = weakContext.get().getFilesDir() + C.newDictFileName;
            currFileName = weakContext.get().getFilesDir() + C.dictFileName;

            //запрашиваем ссылку на файл
            URL url = new URL(C.urlDictionary);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(3000);
            connection.connect();

            InputStream input = new BufferedInputStream(url.openStream());

            // Output stream to write file
            OutputStream output = new FileOutputStream(newFileName);
            byte[] data = new byte[1024];
            while ((count = input.read(data)) != -1) {
                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(Void nothing) {
        File newFile = new File(newFileName);
        File currFile = new File(currFileName);

        long newLen = newFile.length();

        if (newLen > 0 && newLen != currFile.length()){
            Toast.makeText(weakContext.get(), R.string.dict_updated, Toast.LENGTH_SHORT).show();
        }

        if (!newFile.renameTo(currFile)){
            Toast.makeText(weakContext.get(), R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
        weakMethods.get().start();
    }

}