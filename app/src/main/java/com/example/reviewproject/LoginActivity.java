package com.example.reviewproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";     // TAG 추가
    private FirebaseAuth mAuth;     // FirebaseAuth 인스턴스 선언

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.checkbutton).setOnClickListener(onClickListener);  // 로그인 버튼 리스너 생성
        findViewById(R.id.signupbutton).setOnClickListener(onClickListener);  // 회원가입 버튼 리스너 생성
        findViewById(R.id.gotoPasswordResetButton).setOnClickListener(onClickListener);  // 비밀번호 재설정 버튼 리스너 생성

    }

    @Override
    public void onBackPressed() {      // 뒤로가기 눌렀을 때 로직 -> 프로세스 종료시킴
        // 로그인 후에 뒤로가기 눌렀을 때, 로그인 창이 나타나지않도록함.
        super.onBackPressed();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {     // 리스너 객체 생성 (클릭했을 때 반응)
        @Override
        public void onClick(View v) {   // 클릭했을때
            switch(v.getId()) {
                case R.id.checkbutton:            // 얻은 아이디가 loginbutton (회원가입 버튼)이면
                   Login();        // 로그인 함수 실행
                    break;
                case R.id.gotoPasswordResetButton:            // 얻은 아이디가 loginbutton (회원가입 버튼)이면
                    myStartActivity(PasswordResetActivity.class); // 비밀번호 재설정 화면 이동
                    break;
                case R.id.signupbutton:
                myStartActivity(SignUpActivity.class);
                break;
            }

        }
    };      // 세미콜론 필수

    private void Login() {     // 사용자 정보 업데이트
        String email = ((EditText)findViewById(R.id.emailEditText)).getText().toString();
        // getText()는 일반 View는 사용못하기때문에 EditText으로 형변환을 해준다.
        String password = ((EditText)findViewById(R.id.passwordEditText)).getText().toString();
        // 이메일, 패스워드를 입력받아서 String 형태로 변경한다.

        if(email.length() > 0 && password.length() > 0) {    // 사용자가 0자 이상 입력안했을 경우엔 경고창 출력
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {      // 로그인 성공했을 때 실행 로직
                                // Sign in success, update UI with the signed-in user's information
                                //Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                startToast("로그인 성공");
                                myStartActivity(MainActivity.class); // 메인 화면 이동
                                //updateUI(user);
                            } else {
                                // 로그인 실패시 메시지 출력
                                //Log.w(TAG, "createUserWithEmail:failure", task.getException());

                                if (task.getException() != null) {  // null 처리
                                    startToast("이메일 혹은 비밀번호를 확인해주세요"); // 오류내용 Toast로 출력
                                    Log.d(TAG, task.getException().toString()); // 오류내용 출력
                                }
                                // 로그인 실패했을 때 UI
                                //updateUI(null);
                            }
                        }
                    });
        }
        else{  // 이메일, 비밀번호를 입력하지 않았을 때
            startToast("이메일 또는 비밀번호를 입력해주세요");
        }
    }

    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }

    private void myStartActivity(Class c) {    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
        Intent intent = new Intent(this, c);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    // 스택에 있던 이전 작업들 삭제
        startActivity(intent);
    }
}




