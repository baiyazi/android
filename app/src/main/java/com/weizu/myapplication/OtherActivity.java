package com.weizu.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.weizu.annotation.ARouter;
import com.weizu.router_manager2.ARouterManager;

@ARouter(path = "233")
public class OtherActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);

        TextView viewById = findViewById(R.id.text);
        viewById.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        try {
            ARouterManager.jump(this, "123");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}