package com.example.reviewproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Review_FileList extends AppCompatActivity {
    private static final String TAG = "Review_FileList";     // TAG 추가

    // 현재 사용자 불러오기
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public ListView fileListView;
    private FloatingActionButton MenuButton;        // 메뉴 선택 버튼
    private FloatingActionButton Menu_XButton;      // 메뉴 선택 취소 버튼
    private FloatingActionButton FileAddButton;     // 파일 추가 버튼
    private FloatingActionButton FileDeleteButton;      // 파일 삭제 버튼
    private FloatingActionButton FileDeleteButton_Back;   // 파일 삭제 "취소" 버튼
    private FloatingActionButton FileDeleteButton_Ok;   // 파일 삭제 "완료" 버튼
    private FloatingActionButton ManggagViewButton;     // 망각곡선 확인 버튼

    private String subjectDocId;     // SubjectCategory 문서 ID
    private String fileName;         // 파일 이름
    private String Subject;          // 해당 과목

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        // 복습하기 리스트에서는 '집중모드' 자동실행 ( * 집중모드가 실행되는 곳은 PDF 뷰어 )

        // Intent에서 선택한 과목 이름 받아오기
        Subject = getIntent().getStringExtra("Subject");
        //값 전달 test
        Log.d(TAG, "받아온 과목 이름 : " + Subject);

        // 과목 이름을 타이틀로 설정
        setTitle(Subject);

        // 파일 리스트 출력을 위한 ListView 초기화
        fileListView = (ListView) findViewById(R.id.FileList);

        // inflate된 레이아웃에서 버튼 찾아 초기화
        FileAddButton = findViewById(R.id.FileAddButton);           // 파일 추가 버튼
        FileDeleteButton = findViewById(R.id.FileDeleteButton);     // 파일 삭제 버튼
        MenuButton = findViewById(R.id.File_MenuButton);         // 메뉴 선택 버튼
        Menu_XButton = findViewById(R.id.File_Menu_XButton);     // 메뉴 선택 취소 버튼
        FileDeleteButton_Back = findViewById(R.id.FileDeleteButton_Back);   // 파일 삭제 "취소" 버튼
        FileDeleteButton_Ok = findViewById(R.id.FileDeleteButton_OK);   // 파일 삭제 "완료" 버튼
        ManggagViewButton = findViewById(R.id.ManggagViewButton);   // 망각 곡선 버튼

        MenuRemove();    // 메뉴 지우기

        // 받아온 값을 통해 Subject Category Document ID 추출 -> 파일 리스트 불러오기
        ReturnSubjectDocRef(Subject);

    }
    // Toast 출력
    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 복습이 필요한 파일 리스트를 가져오는 메서드
    private void getFileList(String Subject, String subjectDocId) {

        Log.d(TAG, "getFileList() : Review_Subject Collection DocumentID : " + subjectDocId);
        // Firestore 초기화
        CollectionReference Review_FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("Review_SubjectCategory")
                .document(subjectDocId)
                .collection("Review_FileInfo");

        Review_FileInfoRef.get()  // 컬렉션 전체 데이터 가져오기
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<StorageReference> Review_FileList = new ArrayList<>(); // StorageReference 리스트를 담을 리스트

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String file_name = (String) document.get("FileName");
                                // StorageReference를 생성하여 리스트에 추가
                                if (file_name != null) {     // null 처리
                                    Review_FileList.add(FirebaseStorage.getInstance().getReference().child(file_name));
                                } else {  startToast("복습파일 불러오기 실패" + "\n 다시 시도해 주세요.");}
                            }

                            // 어댑터를 생성하고 리스트뷰에 설정
                            Review_FileList.
                                    Review_FileAdapter adapter =
                                    new Review_FileList.Review_FileAdapter(Review_FileList.this, Review_FileList);
                            fileListView.setAdapter(adapter);

                            // 어댑터에 데이터가 변경되었음을 알려줌
                            adapter.notifyDataSetChanged();
                            //startToast("복습파일 불러오기 성공");
                            Log.d(TAG,"복습파일 불러오기 성공");
                        } else {
                            Log.d(TAG, "복습파일 불러오기 실패 : Error getting documents: ", task.getException());
                            startToast("복습파일 불러오기 실패" + "\n 다시 시도해 주세요.");
                        }
                    }
                });

    }

    // 파일리스트를 출력하기위한 커스텀 어댑터
    public class Review_FileAdapter extends ArrayAdapter<StorageReference> {
        private static final String TAG = "Review_FileAdapter";
        private final Context context;
        private final List<StorageReference> files;

        public Review_FileAdapter(Context context, List<StorageReference> files) {     // 생성자, 멤버변수 초기화
            super(context, R.layout.list_item, files);
            this.context = context;
            this.files = files;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Item의 현재 위치 구하기
            StorageReference fileRef = files.get(position);

            // 각 아이템 뷰에 해당하는 XML 파일을 inflate ( XML 파일 -> 실제 뷰 객체 )
            if (convertView == null) {
                // 일반 아이템 사용
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            // 텍스트뷰만 사용
            TextView filenameView = (TextView) convertView.findViewById(R.id.item_name);

            // 파일 이름 가져와 나타내기
            filenameView.setText(fileRef.getName());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {        // 파일리스트 아이템 클릭 이벤트

                        fileName = fileRef.getName();  // 전역변수 : fileName 구하기
                        TimeStore(fileName,Subject);           // 공부 시작 시간 저장

                        // 파일 경로 값을 전달하는 부분
                        String DirectoryPath = "users/" + user.getUid() + "/Subject/" + Subject;

                        Log.d(TAG, "Review_FileAdapter : 전달된 파일 경로 : " + DirectoryPath);

                        // Dialog : 학습을 시작하시겠습니까? '집중모드'가 실행됩니다.
                        Check_Dialog(Subject, fileName, DirectoryPath);
                }
            });

            return convertView;
        }
    }

    // 아이템(파일) 클릭 시간(공부 시작시간)을 Firestore에 저장하는 함수
    // 시간은 복습리스트 컬렉션이 아닌, 학습리스트(기본) 컬렉션에 저장한다.
    private void TimeStore(String fileName, String subject) {
        // Firestore 초기화 ( 학습(기본)리스트 )
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference SubjectRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("SubjectCategory");


       SubjectRef.whereEqualTo("subject", subject)   // 받아온 값과 일치하는 문서 찾기
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subjectDocId = document.getId();    // 아이디 구하기

                                // (Test) 값 확인
                                Log.d(TAG, "Timestore : Subject Collection Document ID: " + subjectDocId);
                                Log.d(TAG, "Timestore : FileInfo Collection Document ID: " + fileName);

                                // 현재 시간 가져오기
                                Date currentTime = Calendar.getInstance().getTime();

                                // 파일 컬렉션 가져오기
                                CollectionReference FileInfoRef = SubjectRef
                                        .document(subjectDocId)
                                        .collection("FileInfo");

                                FileInfoRef.document(fileName)
                                        .update("StudyStart", currentTime)  // 현재 시간을 'StudyStart' 필드에 업데이트
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // 업데이트 성공
                                                startToast("공부 시작 시간 저장");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // 업데이트 실패
                                                startToast("공부 시작 시간 기록 실패");
                                                Log.e(TAG, "Error saving time: " + e.getMessage());
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "ReturnDocRef 메서드 : Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    // 메뉴 지우기
    private void MenuRemove() {
        MenuButton.setVisibility(View.GONE);
        Menu_XButton.setVisibility(View.GONE);
        FileAddButton.setVisibility(View.GONE);
        FileDeleteButton.setVisibility(View.GONE);
        ManggagViewButton.setVisibility(View.GONE);
        FileDeleteButton_Back.setVisibility(View.GONE);
        FileDeleteButton_Ok.setVisibility(View.GONE);
    }

    //  Dialog : 학습을 시작하시겠습니까? '집중모드'가 실행됩니다.
    private void Check_Dialog(String Subject, String fileName, String DirectoryPath) {

        // Dialog Builder 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(Review_FileList.this);

        // Dialog 레이아웃 설정
        View Dialog_view = LayoutInflater.from(Review_FileList.this).inflate(R.layout.dialog_yes_or_back, null);
        builder.setView(Dialog_view);

        // Dialog 의 TextvView, Button 추가
        TextView Text1 = Dialog_view.findViewById(R.id.Check_Text1);
        TextView Text2 = Dialog_view.findViewById(R.id.Check_Text2);

        String dynamicText1 = "학습을 시작하시겠습니까?";      // TextView에 세팅하기위한 Text
        String dynamicText2 = "<집중모드>가 실행됩니다.";
        Text1.setText(dynamicText1);    // 텍스트 설정
        Text2.setText(dynamicText2);

        Button OKButton = Dialog_view.findViewById(R.id.Check_Ok_Button);            // 확인 버튼
        Button BackButton = Dialog_view.findViewById(R.id.Check_Back_Button);        // 돌아가기 버튼

        // Dialog 생성
        AlertDialog alertDialog = builder.create();     // 객체 생성
        alertDialog.show();         // 사용자에게 보여주기

        // 확인 버튼 클릭 : 집중모드 실행 + PDF 뷰어 연결
        OKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent를 생성하고 파일 경로 값을 설정하여 PdfViewerActivity로 전달 ( 뷰어 이동 )
                Intent intent = new Intent(Review_FileList.this, PDFViewerActivity.class);
                intent.putExtra("DirectoryPath", DirectoryPath);
                intent.putExtra("fileName", fileName);
                intent.putExtra("Subject", Subject);    // 과목이름 전달

                // 복습하기 리스트에서는 '집중모드' 자동실행 ( * 집중모드가 실행되는 곳은 PDF 뷰어 )
                intent.putExtra("Review", true);    // 집중모드 여부 전달

                startActivity(intent);

                alertDialog.dismiss();      // Dialog창 닫기
            }
        });

        // 돌아가기 버튼 클릭
        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }   // Dialog창 닫기
        });
    }
    // 과목 카테고리 해당 문서의 ID 반환 메서드
    private void ReturnSubjectDocRef(String subject) {

        Log.d(TAG,"ReturnDocRef 메서드 실행 ");
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
                                getFileList(subject, subjectDocId);     //  파일 리스트 불러오기
                            }
                        } else {
                            Log.d(TAG, "ReturnDocRef 메서드 : Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}