package com.example.reviewproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Study extends AppCompatActivity {

    private static final String TAG = "Study";     // TAG 추가
    // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);

        findViewById(R.id.endbtn).setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {     // 리스너 객체 생성 (클릭했을 때 반응)
        @Override
        public void onClick(View v) {   // 클릭했을때

            switch (v.getId()) {
                case R.id.endbtn:
                    Calendar c = Calendar.getInstance();   //현재 시간가져오기

                    TimeSave(c);

                    c.add(Calendar.SECOND, 10);     //시간 계산
                    startAlarm(c);                          //알람설정

                    myStartActivity(MainActivity.class);
                    break;
            }

        }

    };

    //알람 설정
    private void startAlarm(Calendar c){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    private void myStartActivity(Class c) {    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }

    private void TimeSave(Calendar c) {    //ArrayList<String> List
        FirebaseFirestore db = FirebaseFirestore.getInstance();     // FireStore 인스턴스 가져오기
        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        //DocumentReference userDocRef = userRef.document(userRef.getId());   // 문서 참조 변수 : 현재 사용자 정보
        DocumentReference userDocRef = userRef.document(user.getUid());

        Map<String, Object> subjectMap = new HashMap<>();      // 데이터를 저장할 Map 객체 생성
        subjectMap.put("time", c);                             // subject 필드에 List 배열 값을 추가
        // subject 필드에 과목이름 추가

        // FireStore에 데이터 추가
        userDocRef.collection("time")   // 현재 사용자의 SubjectCategory 서브컬렉션 접근
                .add(subjectMap)            // 데이터 추가
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {        // 성공적으로 추가되었을 때
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        //startToast("과목 추가 완료");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }
}