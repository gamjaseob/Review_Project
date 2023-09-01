package com.example.reviewproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";     // TAG 추가
    private FirebaseAuth mAuth;     // FirebaseAuth 인스턴스 선언
    // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    ArrayList<String> famsaylist = new ArrayList<String>();

    //명언, 디데이
    TextView famsay; // 선언

    int i = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);     // 보여지는 화면

        // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //명언
        famsay = (TextView) findViewById(R.id.FamSay);

        //명언 관련
        FamSayAdd();    //명언 추가
        FamSay();       //명언 실행


        if (user == null) {   // 로그인이 안되어있을 경우 (유저가 없을 경우)
            myStartActivity(LoginActivity.class);      // 로그인 창으로 이동
        } else {      // 로그인이 되어있을경우
            FirebaseFirestore db = FirebaseFirestore.getInstance();     // 데이터베이스 초기화
            DocumentReference docRef = db.collection("users").document(user.getUid());  // 사용자 고유식별자 구해서 불러오기
            //DocumentReference docRef = db.collection("user").document(name)
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                       //String name = (String) document.getData().get("name");
                        if (document.exists()) {        // 사용자에 대한 정보가 존재하면
                            String name = (String) document.getData().get("name");
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData().get("name"));
                            startToast(name + "님 환영합니다.");
                        } else {
                            Log.d(TAG, "No such document");
                            startToast("회원정보를 등록해주세요");
                            //myStartActivity(MemberinitActivity.class);      // 회원정보 등록 페이지(마이페이지)로 이동 ( 예정 )
                        }

                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }

        findViewById(R.id.logoutButton).setOnClickListener(onClickListener);  // 로그아웃 버튼 리스너 생성
        findViewById(R.id.SubjectCategory).setOnClickListener(onClickListener);  // 학습하기(과목 리스트) 버튼 리스너 생성
        findViewById(R.id.ReviewList).setOnClickListener(onClickListener);      // 복습하기 버튼 리스터 생성
        findViewById(R.id.studybtn).setOnClickListener(onClickListener);        //공부 버튼 리스너 생성
        findViewById(R.id.manggagbtn).setOnClickListener(onClickListener);        //망각곡선 버튼 리스너 생성
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {     // 리스너 객체 생성 (클릭했을 때 반응)
        @Override
        public void onClick(View v) {   // 클릭했을때

            switch (v.getId()) {
                case R.id.logoutButton:            // 얻은 아이디가 로그아웃 버튼이면
                    FirebaseAuth.getInstance().signOut();   // 로그아웃하기
                    myStartActivity(LoginActivity.class);   // 로그인 화면 이동
                    break;
                case R.id.SubjectCategory:
                    myStartActivity(SubjectCategory.class);     // 과목리스트로 이동 (학습하기)
                    break;

                case R.id.ReviewList:           // 복습하기 리스트
                    myStartActivity(Review_SubjectCategory.class);
                    break;

                case R.id.studybtn:
                    myStartActivity(Study.class);
                    break;
                case R.id.manggagbtn:
                    myStartActivity(Manggag.class);
                    break;
            }
        }
    }; // 세미콜론 필수

    private void myStartActivity(Class c) {    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }

    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //명언 함수
    private void FamSay() {
        Timer scheduler = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                famsay.setText(famsaylist.get(i));
                if (i < 9) {
                    i = i + 1;
                } else {
                    i = 0;
                }
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 5000);
    }

    //명언 추가 함수
    private void FamSayAdd() {
        famsaylist.add("명언1");
        famsaylist.add("명언2");
        famsaylist.add("명언3");
        famsaylist.add("명언4");
        famsaylist.add("명언5");
        famsaylist.add("명언6");
        famsaylist.add("명언7");
        famsaylist.add("명언8");
        famsaylist.add("명언9");
        famsaylist.add("명언10");
    }
}
