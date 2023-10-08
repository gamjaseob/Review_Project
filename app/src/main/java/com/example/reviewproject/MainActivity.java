package com.example.reviewproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private boolean Review;     // 복습하기 (집중모드) 여부 확인 변수
    private boolean GoToManggag;    // 망각곡선 바로가기를 위한 변수

    int i = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);     // 보여지는 화면

        // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Review = getIntent().getBooleanExtra("Review",Review);      // 초기값은 False
        Log.d(TAG, "Review: 받아온 복습하기 리스트 (집중모드) 여부 : " + Review);

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

        ImageView logoutButton = findViewById(R.id.logoutButton);  // 로그아웃 버튼
        LinearLayout SubjectCategory_Button = findViewById(R.id.SubjectCategory);  // 학습하기(과목 리스트) 버튼
        LinearLayout ReviewList_Button = findViewById(R.id.ReviewList);      // 복습하기 버튼 리
        LinearLayout manggag_Button = findViewById(R.id.manggagbtn);        //망각곡선 버튼
        TextView Study_button = findViewById(R.id.studybtn);       // 집중모드 리스너 생성

        // '집중모드' 버튼 텍스트 변경
        if(Review) {    // 집중모드가 이미 실행 중이면
            Study_button.setText("집중모드 종료");
        } else {
            Study_button.setText("집중모드 시작");
        }

        // 로그아웃
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();   // 로그아웃하기
                myStartActivity(LoginActivity.class);   // 로그인 화면 이동
            }
        });

        // 학습하기
        SubjectCategory_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 집중모드 여부 전달
                Intent intent = new Intent(MainActivity.this, SubjectCategory.class);
                intent.putExtra("Review", Review);      // 집중모드 여부 전달
                startActivity(intent);
            }
        });

        //복습하기
        ReviewList_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { myStartActivity(Review_SubjectCategory.class); }
        });

        // 집중모드
        Study_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { Study_Dialog((Button) Study_button); }
        });

        // 망각곡선
        manggag_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoToManggag = true;
                // 망각곡선 바로가기 : 과목 카데고리로 이동
                Intent intent = new Intent(MainActivity.this, SubjectCategory.class);
                intent.putExtra("GoToManggag", GoToManggag);      // 바로가기 여부 전달
                startActivity(intent);
            }
        });
    }

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
        famsaylist.add("미래는 꿈의 아름다움을 믿는 자의 것입니다");
        famsaylist.add("끝날 때까지 항상 불가능해 보인다");
        famsaylist.add("노력을 대신할 수 있는 것은 없습니다");
        famsaylist.add("열심히 하면 할수록 행운도 더 많이 옵니다");
        famsaylist.add("탁월함은 기술이 아니라 태도입니다");
        famsaylist.add("성적이나 결과는 행동이 아니라 습관입니다");
        famsaylist.add("핑계를 댈 때가 아니다");
        famsaylist.add("걱정은 작은 것에 큰 그림자를 준다");
        famsaylist.add("의심은 실패보다 더 많은 꿈을 죽인다");
        famsaylist.add("공부가 뭐 대수냐 후회가 무서운거지");
    }
    // '집중모드를 시작&종료 하시겠습니까?' 문구를 띄우는 Dialog
    private void Study_Dialog(Button Study_button) {

        String dynamicText1;    // 텍스트 설정
        //String dynamicText2;

        // Dialog Builder 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // Dialog 레이아웃 설정
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_yes_or_back, null);
        builder.setView(view);

        // Dialog 의 TextvView, Button 추가
        TextView Text1 = view.findViewById(R.id.Check_Text1);
        //TextView Text2 = view.findViewById(R.id.Check_Text2);

        if(Review) {    // 현재 집중모드일 경우
            dynamicText1 = "<집중모드>를 종료하시겠습니까?";

        } else   // 현재 집중모드가 아닐 경우
        {
            dynamicText1 = "<집중모드>를 시작하시겠습니까?";
        }

        Text1.setText(dynamicText1);    // 텍스트 설정

        Button OKButton = view.findViewById(R.id.Check_Ok_Button);            // 확인 버튼
        Button BackButton = view.findViewById(R.id.Check_Back_Button);        // 돌아가기 버튼

        // Dialog 생성
        AlertDialog alertDialog = builder.create();     // 객체 생성
        alertDialog.show();         // 사용자에게 보여주기

        // 확인 버튼 클릭
        OKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Review) {    // 현재 집중모드일 경우 -> 집중모드 종료하기
                    startToast("집중모드 Off");
                    Study_button.setText("집중모드 시작");
                    Review = false;

                } else   // 현재 집중모드가 아닐 경우 -> 집중모드 시작하기
                {
                    startToast("집중모드 ON");
                    Study_button.setText("집중모드 종료");
                    Review = true;

                    // 집중모드 실행로직

                }

                alertDialog.dismiss();      // Dialog창 닫기

            }
        });

        // 돌아가기 버튼 클릭 : 이전 화면으로 되돌아가기
        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();      // Dialog창 닫기
            }
        });

    }
}
