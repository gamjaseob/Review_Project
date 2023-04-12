package com.example.reviewproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;       // 뷰클래스 import
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";     // TAG 추가
    private FirebaseAuth mAuth;     // FirevaseAuth 인스턴스 선언

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Firebase Auth 인스턴스 초기화
        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.signUpbutton).setOnClickListener(onClickListener);  // 회원가입 버튼 리스너 생성
        findViewById(R.id.gotoLoginButton).setOnClickListener(onClickListener); // 로그인 화면 이동 버튼 리스너 생성

    }

    @Override
    public void onBackPressed() {      // 뒤로가기 눌렀을 때 로직 -> 프로세스 종료시킴
        // 로그인 후에 뒤로가기 눌렀을 때, 회원가입 창이 나타나지않도록함.
        super.onBackPressed();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
/*
    @Override
    public void onStart() {     // 활동을 초기화할 때 사용자가 현재 로그인되어 있는지 확인
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();


        if(currentUser != null){
            currentUser.reload();
        }

    }
*/
    View.OnClickListener onClickListener = new View.OnClickListener() {     // 리스너 객체 생성 (클릭했을 때 반응)
        @Override
        public void onClick(View v) {   // 클릭했을때
            switch(v.getId()) {
                case R.id.signUpbutton:            // 얻은 아이디가 signup_button (회원가입 버튼)이면
                   signUp();        // 회원가입 함수 실행
                    break;
                case R.id.gotoLoginButton:
                    myStartActivity(LoginActivity.class);       // 로그인 화면 이동 함수
                    break;
            }

        }
    };      // 세미콜론 필수

    private void signUp() {     // 신규 사용자 가입
        String email = ((EditText)findViewById(R.id.emailEditText)).getText().toString();
        // getText()는 일반 View는 사용못하기때문에 EditText으로 형변환을 해준다.
        String password = ((EditText)findViewById(R.id.passwordEditText)).getText().toString();
        // 이메일, 패스워드를 입력받아서 String 형태로 변경한다.
        String passwordCheck = ((EditText)findViewById(R.id.password_check)).getText().toString();
        // 비밀번호 확인
        String name = ((EditText)findViewById(R.id.nameEditText)).getText().toString();

        if(name.length() > 0 && email.length() > 0 && password.length() > 0 && passwordCheck.length() > 0 ) {    // 사용자가 0자 이상 입력안했을 경우엔 경고창 출력
            if (password.equals(passwordCheck)) {        // 입력한 비밀번호 동일여부 확인 (-> 같을 경우)
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {      // 사용자 계정 생성 성공했을 경우
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();      // 클라우드 파이어베이스 초기화

                                    MemberInfo memberInfo = new MemberInfo(name, email);

                                    if(user != null) {  // 사용자 정보가 존재하면 FireStore DB에 넣기
                                        //  Cloud FireStore DB 연동하기
                                        db.collection("users").document(user.getUid()).set(memberInfo)
                                                // 현재 사용자의 정보를 FireBase Firestore DB에 저장하기
                                                // 'user' 컬렉션 참조 -> 사용자의 고유 식별자를 사용하여 사용자에 대한 문서를 참조 -> memberInfo 객체를 사용하여 데이터 저장
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        startToast("회원가입 성공");
                                                        finish();       // 화면 종료
                                                        myStartActivity(MainActivity.class);   // Main 화면 이동
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        startToast("회원가입 실패");
                                                        Log.w(TAG, "Error writing document", e);
                                                    }
                                                });
                                    }

                                } else {
                                    // 실패시 메시지 출력
                                    //Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    if (task.getException() != null) {  // null 처리
                                        startToast(task.getException().toString()); // 오류내용 Toast로 출력
                                    }
                                }
                            }
                        });
            } else {        // 비밀번호가 같지않으면 Toast창 띄우기
                startToast("비밀번호가 일치하지 않습니다.");
            }
        }else{  // 입력하지않은 창이 있을 경우
                startToast("회원정보를 입력해주세요");
        }
    }

    private void startToast(String msg) {     // Toast 출력 (알림창) 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void myStartActivity(Class c) {    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
        Intent intent = new Intent(this, c);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    // 스택에 있던 이전 작업들 삭제
        startActivity(intent);
    }

}

