package com.example.reviewproject;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordResetActivity extends AppCompatActivity {
    private static final String TAG = "PasswordResetActivity";     // TAG 추가
    private FirebaseAuth mAuth;     // FirebaseAuth 인스턴스 선언

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        // Firebase Auth 인스턴스 초기화
        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.sendButton).setOnClickListener(onClickListener);  // 버튼 리스너 생성

    }

    View.OnClickListener onClickListener = new View.OnClickListener() {     // 리스너 객체 생성 (클릭했을 때 반응)
        @Override
        public void onClick(View v) {   // 클릭했을때
            switch(v.getId()) {
                case R.id.sendButton:
                    send();
                    break;
            }

        }
    };      // 세미콜론 필수

    private void send() {     // 비밀번호 재설정 Logic
        String email = ((EditText)findViewById(R.id.emailEditText)).getText().toString();
        // getText()는 일반 View는 사용못하기때문에 EditText으로 형변환을 해준다.


        if(email.length() > 0) {    // 사용자가 0자 이상 입력했을 때
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                startToast("이메일을 보냈습니다.");
                                Log.d(TAG, "Email sent.");
                            }
                        }
                    });
        }
        else{  // 이메일을 입력하지 않았을 때
            startToast("이메일을 입력해주세요");
        }
    }

    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}





