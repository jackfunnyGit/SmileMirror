package com.asus.jacktsai.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.asus.zenheart.smilemirror.FaceTrackerActivity;
import com.asus.zenheart.smilemirror.Util.PermissionUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startButton = (Button) findViewById(R.id.button);
        //to simulate zenHeart,permission is requested here first
        if (!PermissionUtil.hasPermissions(this, PermissionUtil.VIDEO_PERMISSIONS)) {
            PermissionUtil.requestPermission(this, PermissionUtil.VIDEO_PERMISSIONS, 3);
        }
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), FaceTrackerActivity.class));
            }
        });
    }
}
