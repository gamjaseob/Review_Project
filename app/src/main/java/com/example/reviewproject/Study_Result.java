package com.example.reviewproject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Study_Result extends AppCompatActivity {
    private static final String TAG = "Study_Result";     // TAG 추가

    // 현재 사용자 불러오기
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String subjectDocId;     // SubjectCategory 문서 ID
    private String fileName;         // 파일 이름
    private String Subject;          // 해당 과목
    private String DirectoryPath;   // 파일 경로
    private Button Study_ResultOK_Button;  // 학습 태도 분석 결과 '확인'버튼
    private boolean IsStudyList;   // 학습하기 or 복습하기 리스트인지 구별하기 위한 변수

    private boolean result;

    TextView test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_result);

        test = (TextView) findViewById(R.id.test);


        // Intent를 통해 데이터값 받아오기
        DirectoryPath = getIntent().getStringExtra("DirectoryPath");
        fileName = getIntent().getStringExtra("fileName");
        Subject = getIntent().getStringExtra("Subject");
        IsStudyList = getIntent().getBooleanExtra("IsStudyList", IsStudyList);
        result = getIntent().getBooleanExtra("result", result);
        //ReturnSubjectDocRef(Subject);


        //값 전달 test
        Log.d(TAG, "DirectoryPath: 받아온 파일 경로 : " + DirectoryPath);
        Log.d(TAG, "fileName: 받아온 파일 이름 : " + fileName);
        Log.d(TAG, "Subject: 받아온 과목 이름 : " + Subject);
        Log.d(TAG, "추출한 Subject Collection DocumentID : " + subjectDocId);  // 여기가 왜 null인지 모르겠다.
        Log.d(TAG, "StudyList: 받아온 학습하기 & 복습하기 여부 : " + IsStudyList);

        Study_ResultOK_Button = findViewById(R.id.StudyResult_Ok);   // 확인 버튼

        // 확인 버튼 이벤트 처리 :
        Study_ResultOK_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startToast("복습하기를 종료합니다.");
                startToast("집중모드 Off");
                // 메인화면으로 이동
                // Intent를 생성하고 파일 경로 값을 설정하여 Study_Result로 전달
                Intent intent = new Intent(Study_Result.this, MainActivity.class);
                intent.putExtra("Review", false);      // 복습하기 리스트 해제 ( 집중모드 해제 )

                // 액티비티 스택을 지우고 새로운 작업으로 시작하기 위한 플래그 설정
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
            }
        });


        // 학습 태도 결과 출력 메서드

        // 학습 태도 결과 안좋을 때
        // if ( IsStudyList && Study_Result <= 60% ) 조건 추가하기!
        //if (IsStudyList) { }          // 학습하기 리스트인 경우 : 복습하기 리스트에 추가

        if(result == false) {
            test.setText("GOOD");
        }
        else{
            test.setText("BAD");
            AddReviewList(Subject);
            Calendar c = Calendar.getInstance();   //현재 시간가져오기

            TimeSave(c);

            c.add(Calendar.SECOND, 10);     //시간 계산
            startAlarm(c);                          //알람설정
        }

        // 복습하기 리스트인 경우 : 그대로 유지

        // 받아온 값을 통해 Subject Category Document ID 추출 + 학습 태도 결과가 좋다면 복습 리스트에서 파일 삭제 ( Delete_FireStore 메서드 실행 )
        ReturnSubjectDocRef(Subject);
    }
    // Toast 출력
    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    // FireStore에서 Review_FileInfo 정보 삭제 : 복습 완료 했을 때
    // ex) 학습 성실도가 60% 이상이면, 자동으로 삭제되도록 한다.
    private void Delete_FireStore(String FileToDelete, String subjectDocId) {

        // Test
        Log.d(TAG,"Delete_FireStore ( 파일 삭제할지 판단하는 ) 메서드 실행 ");
        Log.d(TAG,"Delete_FireStore 메서드 : 받아온 subjectDocID : " + subjectDocId );
        Log.d(TAG,"Delete_FireStore 메서드 : 받아온 fileName : " + FileToDelete );

        // if  학습 성실도가 60% 이상이면 삭제 한다. ( 아래에 기술된 로직 수행 )
        // else 그렇지않으면 삭제를 수행하지 않는다.

        CollectionReference FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("Review_SubjectCategory")
                .document(subjectDocId)
                .collection("Review_FileInfo");

        // 'FileInfo' 컬렉션에서 'fileName' 필드 값이 지정한 값과 일치하는 문서를 검색
        Query query = FileInfoRef.whereEqualTo("FileName", FileToDelete);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 일치하는 문서를 찾았을 때 삭제
                for (QueryDocumentSnapshot document : task.getResult()) {

                    // 해당 파일 문서 ID 추출
                    String documentId = document.getId();

                    // 파일 삭제 (문서 삭제)
                    FileInfoRef.document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // 삭제 성공
                                startToast("복습 완료");
                                Log.d(TAG, FileToDelete + " : FireStore 파일 삭제 성공");

                            }).addOnFailureListener(e-> {
                                // 삭제 실패
                                Log.d(TAG,FileToDelete + " : FireStore 파일 삭제 실패 : " + e.getMessage());
                            });
                }
            } else {
                // 파일리스트 검색 실패 시 처리 ( Query )
                System.out.println("복습 리스트 쿼리 실패: " + task.getException().getMessage());
            }
        });
    }

    // 과목 카테고리 해당 문서의 ID 반환 메서드
    private void ReturnSubjectDocRef(String subject) {

        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        DocumentReference userDocRef = userRef.document(user.getUid()); // 문서 참조 변수 : 현재 사용자 정보

        userDocRef.collection("Review_SubjectCategory")
                .whereEqualTo("subject", subject)   // 받아온 값과 일치하는 문서 찾기
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                subjectDocId = document.getId();    // 아이디 구하기
                                Log.d(TAG,"ReturnDocRef 메서드 Return 값 : " + subjectDocId);

                                // 학습 태도 결과가 좋다면 복습 리스트에서 파일 삭제
                                Delete_FireStore(fileName,subjectDocId);
                            }
                        } else {
                            Log.d(TAG, "ReturnDocRef 메서드 : Error getting documents: ", task.getException());
                        }
                    }
                });
    }
    // 복습리스트 FireStore에 파일 정보를 추가 : 과목 추가 및 검색
    private void AddReviewList(String subject) {
        CollectionReference userRef = db.collection("users");
        DocumentReference userDocRef = userRef.document(user.getUid());

        userDocRef.collection("Review_SubjectCategory")
                .whereEqualTo("subject", subject)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {       // Review_SubjectCategory 컬렉션이 없는 경우 ( 문서가 없는 경우 )
                            Map<String, Object> newCategory = new HashMap<>();      // 새로운 데이터(과목) 추가하기
                            newCategory.put("subject", subject);

                            userDocRef.collection("Review_SubjectCategory")
                                    .add(newCategory)
                                    .addOnSuccessListener(documentReference -> {
                                        // 새로운 과목 카테고리 문서 생성 성공
                                        String newSubjectId = documentReference.getId();
                                        Add_Review_FileInfo(userDocRef, newSubjectId, subject);    // 파일 정보 추가하기
                                    })
                                    .addOnFailureListener(e -> {
                                        // 새로운 과목 카테고리 문서 생성 실패
                                        Log.w(TAG, "AddReviewList : 과목 카테고리 문서 생성 실패", e);
                                    });
                        } else {
                            // Review_SubjectCategory 컬렉션이 존재하는 경우
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String SubjectId = document.getId();
                                Add_Review_FileInfo(userDocRef, SubjectId, subject);       // 파일 정보 추가하기
                            }
                        }
                    } else {
                        Log.d(TAG, "AddReviewList : 과목 카테고리 문서 찾기 실패: ", task.getException());
                    }
                });
    }

    // 해당하는 과목 복습리스트 FireStore에 파일 정보를 추가
    private void Add_Review_FileInfo(DocumentReference userDocRef, String SubjectId, String subject) {
        // Review_FileInfo 컬렉션 가져오기
        CollectionReference FileInfoRef = userDocRef
                .collection("Review_SubjectCategory")
                .document(SubjectId)
                .collection("Review_FileInfo");

        String DirectoryPath = "users/" + user.getUid() + "/Subject/" + subject;

        // FileInfoMap에 데이터 세팅
        Map<String, Object> FileInfoMap = new HashMap<>();
        FileInfoMap.put("SubjectCategoryId", subject);      // 해당 파일의 과목 카테고리(필드) 추가
        FileInfoMap.put("FileName", fileName);              // 식별자로 사용
        FileInfoMap.put("DirectoryPath", DirectoryPath);         // 디렉토리 경로

        // FireStore : Review_FileInfoRef Collection에 데이터 추가
        FileInfoRef.document(fileName)
                .set(FileInfoMap)
                .addOnSuccessListener(aVoid -> {
                    // 데이터(문서) 추가 성공
                    Log.d(TAG, "Review_FileInfo Collection 데이터(문서)추가 성공: " + fileName);
                    startToast("복습리스트에 파일 추가" + "\n" + fileName);
                })
                .addOnFailureListener(e -> {
                    // 데이터(문서) 추가 실패
                    Log.w(TAG, "Review_FileInfo Collection 데이터(문서)추가 실패", e);
                    startToast("복습리스트에 파일 추가 실패" + "\n" + fileName);
                });
    }

    private void startAlarm(Calendar c){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    private void TimeSave(Calendar c) {    //ArrayList<String> List
        FirebaseFirestore db = FirebaseFirestore.getInstance();     // FireStore 인스턴스 가져오기
        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        DocumentReference userDocRef = userRef.document(user.getUid());

        Map<String, Object> timeMap = new HashMap<>();      // 데이터를 저장할 Map 객체 생성
        timeMap.put("time", c);                             // subject 필드에 List 배열 값을 추가
    }
}