package com.example.reviewproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class PDFViewerActivity extends AppCompatActivity {
    private static final String TAG = "PDFViewerActivity";     // TAG 추가

    // 현재 사용자 불러오기
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public String subjectDocId;     // SubjectCategory 문서 ID
    public String Subject;          // 해당 과목
    public String fileName;         // 파일 이름
    private PDFView pdfView;    // PDF Viewer 컨테이너 객체
    private String DirectoryPath;     // 파일이 존재하는 디렉토리 경로
    private File viewFile;  // 뷰어에 띄울 임시파일 변수
    private boolean Review;     // 복습하기 리스트(집중모드 O)인지 구별하기 위한 변수

    @Override
    public void onBackPressed() {       // 뒤로가기 눌렀을 때
        // '학습을 종료하시겠습니까?' Dialog 띄우기 -> 학습 종료 시간 기록
        TimeCheck_Dialog();
    }

    @Override
    protected void onDestroy() {    // 액티비티가 종료될 때
        super.onDestroy();
        deleteTempFile();   // PDF Viewer용 임시파일 삭제
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        // Intent를 통해 데이터값 받아오기
        DirectoryPath = getIntent().getStringExtra("DirectoryPath");
        fileName = getIntent().getStringExtra("fileName");
        Subject = getIntent().getStringExtra("Subject");
        Review = getIntent().getBooleanExtra("Review", Review);         // 복습하기 리스트 여부

        // 받아온 값(Subject)을 통해 Subject Category Document ID 추출
        ReturnSubjectDocRef(Subject);

        //값 전달 test
        Log.d(TAG, "DirectoryPath: 받아온 파일 경로 : " + DirectoryPath);
        Log.d(TAG, "fileName: 받아온 파일 이름 : " + fileName);
        Log.d(TAG, "Subject: 받아온 과목 이름 : " + Subject);
        Log.d(TAG, "Review: 받아온 복습하기 리스트 (집중모드) 여부 : " + Review);
        Log.d(TAG, "추출한 Subject Collection DocumentID : " + subjectDocId);  // 여기가 왜 null인지 모르겠다.

        if(Review){     // 집중모드일 경우



            // 집중모드 실행 메서드


        }


        // PDF Viewer 객체
        pdfView = findViewById(R.id.pdfView);

        // Firebase Storage에서 PDF 파일 찾아 다운로드
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pdfFileRef = storageRef.child(DirectoryPath)
                .child(fileName);

        // * 중요 * : PDF 뷰어 : PDF 파일만 지원 가능..
        try {
            // PDF Viewer에 출력하기 위한 임시 파일 생성
            viewFile = File.createTempFile("tempFile", ".pdf", getCacheDir());
            pdfFileRef.getFile(viewFile).addOnSuccessListener(taskSnapshot -> {

                // 다운로드 완료 후 PDF 표시
                displayPdfFromFile(viewFile);

            }).addOnFailureListener(exception -> {
                // 다운로드 실패 시 처리 코드
                startToast("파일 열기 실패");
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // PDF Viewer 출력 메서드
    private void displayPdfFromFile(File file) {
        pdfView.fromFile(file)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        // PDF가 로드 완료되었을 때 실행
                        startToast("파일 열기 성공");
                    }
                })
                .scrollHandle(new DefaultScrollHandle(PDFViewerActivity.this))
                .spacing(10) // 페이지 간격 설정
                .load();
    }

    // 임시 파일을 로컬 저장소에서 삭제
    private void deleteTempFile() {
        if (this.viewFile != null && this.viewFile.exists()) {
            boolean isDeleted = this.viewFile.delete();
            if (isDeleted) {
                // 임시 파일 삭제 성공
                Log.d(TAG, "임시 파일 삭제 성공");
            } else {
                // 임시 파일 삭제 실패
                startToast("임시 파일 삭제 실패" + "\n로컬 저장소에서 직접 삭제 바람.");
            }
        }
    }

    // '학습을 종료하시겠습니까?' 문구를 띄우는 Dialog : 학습 종료 시간 기록
    private void TimeCheck_Dialog() {

        String dynamicText2;    // 텍스트 설정

        // Dialog Builder 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(PDFViewerActivity.this);

        // Dialog 레이아웃 설정
        View view = LayoutInflater.from(PDFViewerActivity.this).inflate(R.layout.dialog_yes_or_back, null);
        builder.setView(view);

        // Dialog 의 TextvView, Button 추가
        TextView Text1 = view.findViewById(R.id.Check_Text1);          // 학습을 종료하시겠습니까?
        TextView Text2 = view.findViewById(R.id.Check_Text2);           // <확인>버튼을 누르시면 학습 종료 시간이 기록됩니다.

        String dynamicText1 = "학습을 종료하시겠습니까?";      // TextView에 세팅하기위한 Text

        if(Review) {    // '복습하기' 리스트 OR 집중모드에서 실행한 PDF 뷰어라면
            dynamicText2 = "<확인>버튼을 클릭하면 " +"\n 학습태도 분석 결과가 출력됩니다.";

        } else   // '학습하기' 리스트 OR 집중모드 X에서 실행한 PDF 뷰어라면
        {
            dynamicText2 = "<확인>버튼을 클릭하면 학습 종료 시간이 기록됩니다.";
        }

        Text1.setText(dynamicText1);    // 텍스트 설정
        Text2.setText(dynamicText2);

        Button OKButton = view.findViewById(R.id.Check_Ok_Button);            // 확인 버튼
        Button BackButton = view.findViewById(R.id.Check_Back_Button);        // 돌아가기 버튼

        // Dialog 생성
        AlertDialog alertDialog = builder.create();     // 객체 생성
        alertDialog.show();         // 사용자에게 보여주기

        // 확인 버튼 클릭 : 학습 종료 시간 기록하기 OR 학습태도 분석 결과 출력
        OKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimeStore(fileName);        // FireStore에 공부시간 기록 메서드
                alertDialog.dismiss();      // Dialog창 닫기

                if(Review) {     // '복습하기' 리스트(집중모드 O)에서 실행한 PDF 뷰어라면

                    // 학습태도 분석 로직 넣기


                    // 학습태도 분석 결과로 이동
                    // Intent를 생성하고 파일 경로 값을 설정하여 Study_Result로 전달
                    Intent intent = new Intent(PDFViewerActivity.this, Study_Result.class);
                    intent.putExtra("DirectoryPath", DirectoryPath);
                    intent.putExtra("fileName", fileName);
                    intent.putExtra("Subject", Subject);    // 과목이름 전달
                    intent.putExtra("Review", Review);      // 복습하기 리스트 여부 전달
                    startActivity(intent);

                }else {     // '학습하기' 리스트(집중모드 X)에서 실행한 PDF 뷰어라면
                    finish();           // 이전 액티비티로 돌아가기
                }

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
    // '확인'버튼 클릭 시, 공부 종료시간을 Firestore에 저장하는 함수
    public void TimeStore(String fileName) {
        // Firestore 초기화
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("SubjectCategory")
                .document(subjectDocId)
                .collection("FileInfo");

        // (Test) 값 확인
        Log.d(TAG, "Timestore : Subject Collection Document ID: " + subjectDocId);
        Log.d(TAG, "Timestore : FileInfo Collection Document ID: " + fileName);

        // 현재 시간 가져오기
        Date currentTime = Calendar.getInstance().getTime();

        // fileName을 식별자로 사용
        FileInfoRef.document(fileName)
                .update("StudyEnd", currentTime)  // 현재 시간을 'StudyEnd' 필드에 업데이트
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // 업데이트 성공
                        startToast("학습 종료 시간이 기록되었습니다.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // 업데이트 실패
                        startToast("학습 종료 시간 기록 실패" +"\n 다시 시도해 주세요.");
                        Log.e(TAG, "Error saving time: " + e.getMessage());
                    }
                });
    }

    // 과목 카테고리 해당 문서의 ID 반환 메서드
    public void ReturnSubjectDocRef(String subject) {

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
                                subjectDocId = document.getId();    // 아이디 구하기
                                Log.d(TAG,"ReturnDocRef 메서드 Return 값 : " + subjectDocId);
                            }
                        } else {
                            Log.d(TAG, "ReturnDocRef 메서드 : Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    // Toast 출력
    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
