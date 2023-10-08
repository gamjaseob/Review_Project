package com.example.reviewproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

//망각곡선 시간 가져오기
import java.sql.Time;
import java.util.Calendar;

import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;


public class Manggag extends AppCompatActivity {
    private static final String TAG = "Manggag";     // TAG 추가
    public String subjectDocId;

    public String fileName;
    public String Subject;
    public Timestamp studyTime;        // 시간 변수 ( Timestamp 형식 저장, Data형으로 변환 필요할 듯? )

    // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manggag);

        // Intent에서 선택한 과목 이름 & 파일 이름 받아오기
        Subject = getIntent().getStringExtra("Subject");
        fileName = getIntent().getStringExtra("fileName");

        // 받아온 값을 통해 Subject Category Document ID 추출 -> 이지만 문제 발생으로 이 안에서 구현해야함.
        ReturnSubjectDocRef(Subject);

        // 타이틀 설정
        //setTitle(fileName + " 망각 진행률");

        //값 전달 test
        Log.d(TAG, "받아온 과목 이름 : " + Subject);
        Log.d(TAG, "추출한 Subject Collection Document Id : " + subjectDocId );
        Log.d(TAG, "받아온 파일 이름 : " + fileName);

        // 공부 종료 시간 ( StudyEnd )을 FireStore로부터 얻어오는 메서드 : 주석 풀어보고 Test 해보기
        //StudyTimeLoad(fileName, subjectDocId);

        // + if ( 망각진행률 == 60% ) 이면 AddReviewList ( 리뷰리스트에 추가 ) 메서드 실행
        AddReviewList(Subject);     // test 전용, 조건추가해야함.

    }

    //망각곡선 그리는 함수
    private void Drawgraph(Calendar cal) {
        Calendar c = Calendar.getInstance();

        //현재시간 더블형 변환
        double minute1 = c.get(Calendar.MINUTE) + 1;
        double hour1 = c.get(Calendar.HOUR);
        double day1 = c.get(Calendar.DAY_OF_MONTH);
        double month1 = c.get(Calendar.MONTH) + 1;

        //공부종료시간 더블형 변환
        double minute2 = cal.get(Calendar.MINUTE);
        double hour2 = cal.get(Calendar.HOUR);
        double day2 = cal.get(Calendar.DAY_OF_MONTH);
        double month2 = cal.get(Calendar.MONTH) + 1;

        //현재시간 - 공부 종료 시간
        double minute = minute1 - minute2;
        double hour = hour1 - hour2;
        double day = day1 - day2;
        double month = month1 - month2;

        //시간 더하기
        double nowtime = minute + (hour * 60) + (day * 1440) + (month * 44640);

        //double  = 184 / (Math.pow(Math.log(120.0), 1.25) + 1.84);

        LineChart lineChart = findViewById(R.id.chart);

        ArrayList<Entry> entries = new ArrayList<>();


        for (float t = 0; t <= nowtime; t += 0.1) {
            double mgchart = 184 / (Math.pow(Math.log(t), 1.25) + 1.84);
            float y = (float) mgchart;

            entries.add(new Entry(t, y));
        }


        LineDataSet dataSet = new LineDataSet(entries, "망각진행률");
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);


        lineChart.invalidate();
    }

    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 공부 종료 시간 ( StudyEnd )을 FireStore로부터 얻어오는 메서드
    private void StudyTimeLoad (String fileName, String subjectDocId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference FileInfoDocRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("SubjectCategory")
                .document(subjectDocId)
                .collection("FileInfo")
                .document(fileName);

        FileInfoDocRef.get().addOnCompleteListener(task -> {        // 비동기 접근
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // 공부 종료 시간 불러오기
                    studyTime = document.getTimestamp("StudyEnd");      // studyTime : 전역 변수
                    Log.d(TAG,"Firestore : " + "StudyTime (공부 종료 시간)  : " + studyTime);
                } else {
                    Log.d(TAG,"Firestore : " + "No such document");
                }
            } else {
                Log.d(TAG,"Firestore : "+ "Error getting document: ", task.getException());
            }

            //그래프 그리기 위한 데이터 변환
            Date date = studyTime.toDate();

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            cal.add(Calendar.HOUR, -3);

            // 본격적으로 그래프 그리기
            Drawgraph(cal);
        });
    }

    // 과목 카테고리 해당 문서의 ID 반환 메서드
    private void ReturnSubjectDocRef(String subject) {
        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        DocumentReference userDocRef = userRef.document(user.getUid()); // 문서 참조 변수 : 현재 사용자 정보

        userDocRef.collection("SubjectCategory")
                .whereEqualTo("subject", subject)   // 받아온 값과 일치하는 문서 찾기
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                subjectDocId = document.getId();


                                //종료시간 불러오는 함수

                                StudyTimeLoad(fileName, subjectDocId);

                                // 이 부분에 망각 곡선 작성 :onCreate() 함수부터 Test 바람

                                Log.d(TAG,"ReturnDocRef 메서드 Return 값 : " + subjectDocId);
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

}


