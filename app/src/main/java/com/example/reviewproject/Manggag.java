package com.example.reviewproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//망각곡선 시간 가져오기
import java.util.Calendar;

import java.util.ArrayList;

public class Manggag extends AppCompatActivity {


    private static final String TAG = "Manggag";     // TAG 추가
    // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manggag);

        Drawgraph();
    }

    //망각곡선 그리는 함수
    private void Drawgraph() {
        Intent getInt = getIntent();
        int i = getInt.getIntExtra("count", 0);

        Calendar cal = Calendar.getInstance();
        Calendar c = cal;
        double minute1 = c.get(Calendar.MINUTE) + 5;
        double hour1 = c.get(Calendar.HOUR);
        double day1 = c.get(Calendar.DAY_OF_MONTH);
        double month1 = c.get(Calendar.MONTH) + 1;

        double minute2 = cal.get(Calendar.MINUTE);
        double hour2 = cal.get(Calendar.HOUR);
        double day2 = cal.get(Calendar.DAY_OF_MONTH);
        double month2 = cal.get(Calendar.MONTH) + 1;



        double minute = minute1 - minute2;
        double hour = hour1 - hour2;
        double day = day1 - day2;
        double month = month1 - month2;

        double nowtime = minute + (hour * 60) + (day * 1440) + (month * 44640);

        //double  = 184 / (Math.pow(Math.log(120.0), 1.25) + 1.84);

        LineChart chart = findViewById(R.id.chart);

        ArrayList<Entry> entries = new ArrayList<>();


        for (float t = 0; t <= nowtime; t += 0.1) {
            double mgchart = 184 / (Math.pow(Math.log(t), 1.25) + 1.84);
            float y = (float) mgchart;

            entries.add(new Entry(t, y));
        }


        LineDataSet dataSet = new LineDataSet(entries, "망각진행률");
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

