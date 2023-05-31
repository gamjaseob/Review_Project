package com.example.reviewproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class FileList_Manggag_view extends AppCompatActivity {
    private static final String TAG = "FileList_Manggag_view";     // TAG 추가

    // 현재 사용자 불러오기
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    // Firestore 초기화
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public ListView fileListView;
    private ArrayAdapter<String> adapter;   // 어댑터 ( ListView와 데이터 배열의 다리 역할 );

    public String subjectDocId;     // SubjectCategory 문서 ID
    public String fileName;         // 파일 이름 ( FileInfo 문서 ID )
    public String Subject;          // 해당 과목
    public MutableLiveData<Uri> selectedFileUri;       // File Uri

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manggag_view);

        // Intent에서 선택한 과목 이름 받아오기
        Subject = getIntent().getStringExtra("Subject");

        // 받아온 값을 통해 Subject Category Document ID 추출
        ReturnSubjectDocRef(Subject);

        // 타이틀 설정
        setTitle("망각 진행률 확인");

        //값 전달 test
        Log.d(TAG, "받아온 과목 이름 : " + Subject);
        Log.d(TAG, "추출한 Subject Collection DocumentID : " + subjectDocId);  // 여기가 왜 null인지 모르겠다.


        // 파일 리스트 출력을 위한 ListView 초기화
        fileListView = (ListView) findViewById(R.id.FileList_Manggag_view);

        getFileList(Subject);   // 파일 리스트 불러오기
        startToast("확인하고자 하는 파일을 선택하세요.");

    }
    // Toast 출력
    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 선택한 과목의 파일 리스트를 가져오는 메소드
    private void getFileList(String Subject) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference()
                .child("users/"+user.getUid()+"/Subject/"+Subject);

        storageRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        // 리스트뷰 어댑터 생성 (getView 이용)
                        FileAdapter adapter = new FileAdapter(FileList_Manggag_view.this, listResult.getItems());
                        fileListView.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "파일 불러오기 실패", e);
                    }
                });
    }

    // 파일리스트를 출력하기위한 커스텀 어댑터
    public class FileAdapter extends ArrayAdapter<StorageReference> {
        private static final String TAG = "FileAdapter";
        private final Context context;
        private final List<StorageReference> files;

        public FileAdapter(Context context, List<StorageReference> files) {     // 생성자, 멤버변수 초기화
            super(context, R.layout.list_file_item, files);
            this.context = context;
            this.files = files;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Item의 현재 위치 구하기
            StorageReference fileRef = files.get(position);

            // 각 아이템 뷰에 해당하는 XML 파일을 inflate ( XML 파일 -> 실제 뷰 객체 )
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_file_item, parent, false);
            }

            TextView filenameView = (TextView) convertView.findViewById(R.id.file_name);

            // 파일 이름 가져와 나타내기
            filenameView.setText(fileRef.getName());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {        // 파일리스트 아이템 클릭 이벤트 : 망각 진행률 뷰어 이동
                    fileName = fileRef.getName();    // * 전역변수 : fileName 구하기

                    Intent intent = new Intent(FileList_Manggag_view.this, Manggag.class);
                    intent.putExtra("Subject", Subject);    // 과목이름 전달
                    intent.putExtra("fileName", fileName);    // 파일 이름(식별자) 전달

                    Log.d(TAG, "전달한 과목 이름 : " + Subject);
                    Log.d(TAG, "전달한 파일 이름 : " + fileName);

                    startActivity(intent);
                }
            });

            return convertView;
        }
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

}