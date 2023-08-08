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
import android.widget.Button;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
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
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FileList extends AppCompatActivity {
    private static final String TAG = "FileList";     // TAG 추가

    // 현재 사용자 불러오기
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public ListView fileListView;
    public FloatingActionButton FileAddButton;     // 파일 추가 버튼
    public FloatingActionButton ManggagViewButton;     // 망각곡선 확인 버튼

    public String subjectDocId;     // SubjectCategory 문서 ID
    public String fileName;         // 파일 이름
    public String Subject;          // 해당 과목
    public MutableLiveData<Uri> selectedFileUri;       // File Uri

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        // Intent에서 선택한 과목 이름 받아오기
        Subject = getIntent().getStringExtra("selectedSubject");

        // 받아온 값을 통해 Subject Category Document ID 추출
        ReturnSubjectDocRef(Subject);

        // 과목 이름을 타이틀로 설정
        setTitle(Subject);

        //값 전달 test
        Log.d(TAG, "받아온 과목 이름 : " + Subject);
        Log.d(TAG, "추출한 Subject Collection DocumentID : " + subjectDocId);  // 여기가 왜 null인지 모르겠다.


        selectedFileUri = new MutableLiveData<>();     // 사용자가 선택한 파일의 URI 변수
        // 파일 선택 다이얼로그를 띄우는 ActivityResultLauncher
        ActivityResultLauncher<String> selectFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        // 선택한 파일의 URI 가져오기
                        selectedFileUri.setValue(result);
                    }
                }
        );

        // 파일 리스트 출력을 위한 ListView 초기화
        fileListView = (ListView) findViewById(R.id.FileList);
        // FileUploadButton 찾아서 초기화
        FileAddButton = findViewById(R.id.FileAddButton);
        ManggagViewButton = findViewById(R.id.ManggagViewButton);

        // 파일 업로드 버튼 클릭 이벤트 처리
        FileAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UploadFile_Dialog(selectFileLauncher ,Subject, selectedFileUri);    // 파일 업로드 Dialog 띄우기
            }
        });

        // 망각곡선 확인 버튼 클릭 이벤트 처리 : 망각 진행률을 확인하기 위한 리스트뷰로 이동
        ManggagViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 선택한 항목의 정보를 Intent에 담아 FileList_Manggag_view.Class를 시작

                Intent intent = new Intent(FileList.this, FileList_Manggag_view.class);
                intent.putExtra("Subject", Subject);    // 과목이름 전달
                //intent.putExtra("fileName", fileName);    // 파일 이름(식별자) 전달

                Log.d(TAG, "전달한 과목 이름 : " + Subject);
                //Log.d(TAG, "전달한 파일 이름 : " + fileName);

                startActivity(intent);

            }
        });

        getFileList(Subject);   // 파일 리스트 불러오기

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
                        FileAdapter adapter = new FileAdapter(FileList.this, listResult.getItems());
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
                public void onClick(View view) {        // 파일리스트 아이템 클릭 이벤트 : 공부 시작 시간 저장, 뷰어 이동

                    fileName = fileRef.getName();  // 전역변수 : fileName 구하기
                    TimeStore(fileName);


                    // PDF 파일을 업로드하고 파일 경로 값을 전달하는 부분
                    //String filePath = "users/" + user.getUid() + "/Subject/" + Subject + "/" + fileName;
                    String DirectoryPath = "users/" + user.getUid() + "/Subject/" + Subject;

                    Log.d(TAG, "FileAdapter : 전달된 파일 경로 : " + DirectoryPath);

                    // Intent를 생성하고 파일 경로 값을 설정하여 PdfViewerActivity로 전달
                    Intent intent = new Intent(FileList.this, PDFViewerActivity.class);
                    intent.putExtra("DirectoryPath", DirectoryPath);
                    intent.putExtra("fileName", fileName);
                    intent.putExtra("Subject", Subject);    // 과목이름 전달
                    startActivity(intent);

                }
            });

            return convertView;
        }
    }

    // 업로드할 파일을 입력받기 위한 Dialog (FileLauncher, 과목 이름, 파일 URI 전달)
    private void UploadFile_Dialog(ActivityResultLauncher<String> selectFileLauncher, String Subject, MutableLiveData<Uri> selectedFileUri) {

        // Dialog Builder 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(FileList.this);
        builder.setTitle("파일 업로드");

        // Dialog 레이아웃 설정
        View view = LayoutInflater.from(FileList.this).inflate(R.layout.dialog_upload_file, null);
        builder.setView(view);

        // Dialog 의 EditText, Button 추가
        EditText fileNameEditText = view.findViewById(R.id.file_name);          // 파일 이름
        Button selectFileButton = view.findViewById(R.id.select_button);        // 파일 선택 버튼
        Button uploadButton = view.findViewById(R.id.upload_button);            // 업로드 버튼
        Button cancelButton = view.findViewById(R.id.cancel_button);            // 취소 버튼

        // Dialog 생성
        AlertDialog alertDialog = builder.create();     // 객체 생성
        alertDialog.show();         // 사용자에게 보여주기

        // 파일 선택 버튼 클릭 시 파일 선택창 띄우기
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //launchFilePicker(selectFileLauncher);
                startToast("파일 선택 후, 파일 이름을 입력해주세요!");
                selectFileLauncher.launch("*/*");      // 모든 파일 형식
            }
        });

        // 업로드 버튼 클릭 시 파일 업로드
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EditText에서 파일 이름 가져오기
                fileName = fileNameEditText.getText().toString();
                // 파일 정보 FireStore에 저장
                UploadFireStore(fileName,Subject);

                // 선택한 파일이 없는 경우 오류 메시지 출력
                if (selectedFileUri.getValue() == null) {
                    startToast("파일을 선택해주세요");
                    return;
                }

                // Firebase Storage에 업로드
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference()
                        .child("users/" + user.getUid() + "/Subject/" + Subject)
                        .child(fileName);
                storageRef.putFile(selectedFileUri.getValue())
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // 업로드 완료 시 메시지 출력
                                startToast("파일 업로드 완료");
                                getFileList(Subject);       // 업로드 후 리스트 다시 불러오기
                                alertDialog.dismiss();      // Dialog창 닫기
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // 업로드 실패 시 오류 메시지 출력
                                startToast("파일 업로드 실패, 다시 시도해 주세요.");
                            }
                        });
            }
        });
        // 취소 버튼
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();      // Dialog창 닫기
            }
        });
    }
    // 아이템(파일) 클릭 시간(공부 시작시간)을 Firestore에 저장하는 함수
    public void TimeStore(String fileName) {
        // Firestore 초기화
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("SubjectCategory")
                .document(subjectDocId)
                .collection("FileInfo");

        // (Test) 값 확인
        Log.d(TAG, "Timestore : Subjcet Collection Document ID: " + subjectDocId);
        Log.d(TAG, "Timestore : FileInfo Collection Document ID: " + fileName);

        // 현재 시간 가져오기
        Date currentTime = Calendar.getInstance().getTime();

        // fileName을 식별자로 사용
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

    // 파일 정보를 FireStore에 저장
    public void UploadFireStore(String FileName, String subject) {

        // Firestore 초기화
        CollectionReference FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("SubjectCategory")
                .document(subjectDocId)
                .collection("FileInfo");

        // 현재 시간 가져오기
        Date currentTime = Calendar.getInstance().getTime();

        Map<String, Object> FileInfoMap = new HashMap<>();      // 데이터를 저장할 Map 객체 생성
        FileInfoMap.put("SubjectCategoryId", subject);     // 해당 파일의 과목 카테고리(필드) 추가
        FileInfoMap.put("FileName", FileName);  // 식별자로 사용
        FileInfoMap.put("UploadDate", currentTime); // 파일 업로드날짜

        // FireStore : FileInfoRef Collection에 데이터 추가
        FileInfoRef.document(FileName).set(FileInfoMap)            // 데이터 추가 ( Collection : add / Document : set )
                .addOnSuccessListener(aVoid -> {
                    //Log.d(TAG, "Subjcet Collection Documentv ID: " + subjectDocId);
                    Log.d(TAG, "FileInfo Collection 데이터(문서)추가: " + FileName);
                    // 성공적으로 문서가 추가되었을 때 실행되는 코드
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "FileInfo Collection : Error adding document", e);
                    // 문서 추가 실패 시 실행되는 코드
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

}