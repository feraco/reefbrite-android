package com.nordicsemi.nrfUARTv2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChartActivity extends Activity {

    private Button btnBack, btnSimu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart_controller);

        btnBack = (Button) findViewById(R.id.go_back_btn_1);
        btnSimu = (Button) findViewById(R.id.btn_simulation);


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChartActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        btnSimu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }
}
