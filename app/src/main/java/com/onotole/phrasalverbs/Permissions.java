package com.onotole.phrasalverbs;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

class Permissions {

    private Context context;

    Permissions(Context context) {
        this.context = context;
    }

    boolean AskPermission (String[] Permission){
        boolean Result = true;
        for (String sPermission : Permission)
        {
            if (ContextCompat.checkSelfPermission(context,sPermission) != PackageManager.PERMISSION_GRANTED){
                Result = false;
                ActivityCompat.requestPermissions((Activity) context, Permission, C._requestCode);
            }
        }
        return Result;
    }
}
