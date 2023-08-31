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

public class FileList extends AppCompatActivity {
    private static final String TAG = "FileList";     // TAG 추가

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
    private MutableLiveData<Uri> selectedFileUri;       // File Uri

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

        // inflate된 레이아웃에서 버튼 찾아 초기화
        FileAddButton = findViewById(R.id.FileAddButton);           // 파일 추가 버튼
        FileDeleteButton = findViewById(R.id.FileDeleteButton);     // 파일 삭제 버튼
        MenuButton = findViewById(R.id.File_MenuButton);         // 메뉴 선택 버튼
        Menu_XButton = findViewById(R.id.File_Menu_XButton);     // 메뉴 선택 취소 버튼
        FileDeleteButton_Back = findViewById(R.id.FileDeleteButton_Back);   // 파일 삭제 "취소" 버튼
        FileDeleteButton_Ok = findViewById(R.id.FileDeleteButton_OK);   // 파일 삭제 "완료" 버튼
        ManggagViewButton = findViewById(R.id.ManggagViewButton);   // 망각 곡선 버튼

        MenuCancleClick();    // 메뉴 선택하기 버튼 생성

        // 메뉴 선택하기 버튼 이벤트 처리 : 버튼 클릭했을 때 파일 추가&삭제&망각곡선 버튼 나옴
        MenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { MenuClick(); }
        });

        // 메뉴 선택 취소 버튼 이벤트 처리 : 버튼 클릭했을 때 파일 추가&삭제&망각곡선 버튼 사라짐
        Menu_XButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { MenuCancleClick(); }
        });

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
                        Log.d(TAG,"파일 리스트 조회 성공");
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
        private final SparseBooleanArray selectedItems;     // 체크박스 선택 상태 저장
        boolean isDeleteButtonClicked;               // 삭제 버튼 클릭 여부를 나타내는 변수
        final List<Integer> selectedIndexes;     // 선택된 아이템의 인덱스를 저장할 리스트

        public FileAdapter(Context context, List<StorageReference> files) {     // 생성자, 멤버변수 초기화
            super(context, R.layout.list_item_checkbox, files);
            this.context = context;
            this.files = files;
            this.selectedItems = new SparseBooleanArray();
            this.selectedIndexes = new ArrayList<>();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Item의 현재 위치 구하기
            StorageReference fileRef = files.get(position);

            // 각 아이템 뷰에 해당하는 XML 파일을 inflate ( XML 파일 -> 실제 뷰 객체 )
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_checkbox, parent, false);
            }

            final CheckBox checkBox = convertView.findViewById(R.id.checkBox);
            TextView filenameView = (TextView) convertView.findViewById(R.id.item_name);

            // 체크박스 상태 초기화
            checkBox.setChecked(selectedItems.get(position, false));

            // 파일 이름 가져와 나타내기
            filenameView.setText(fileRef.getName());

            // 체크박스 표시 여부 ( 삭제 버튼 안눌렀을 땐 표시 X )
            if (isDeleteButtonClicked) {
                // 체크박스 설정
                checkBox.setChecked(selectedItems.get(position, false));
                checkBox.setVisibility(View.VISIBLE);

            } else {
                checkBox.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {        // 파일리스트 아이템 클릭 이벤트

                    if (isDeleteButtonClicked) {     // '삭제'버튼 클릭 후 아이템을 클릭했을 때의 동작
                        boolean currentCheckedState = selectedItems.get(position, false);
                        selectedItems.put(position, !currentCheckedState); // 체크 상태 반전
                        notifyDataSetChanged(); // 아이템 업데이트

                        // 체크박스 리스너
                        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                // 체크 박스의 위치(position)를 가져옴
                                // 체크 박스 선택 상태를 맵에 저장
                                selectedItems.put(position, isChecked);

                                if (isChecked) {
                                    // 체크된 아이템의 인덱스를 리스트에 추가
                                    if (!selectedIndexes.contains(position)) {
                                        selectedIndexes.add(position);
                                        Log.d(TAG, selectedIndexes + " : 인덱스 배열에서 추가");
                                    }

                                } else {
                                    // 체크 해제된 아이템의 인덱스를 리스트에서 제거
                                    selectedIndexes.remove((Integer) position);
                                    Log.d(TAG, selectedIndexes + " : 인덱스 배열에서 제거");
                                }
                                Log.d(TAG, selectedIndexes + " : 인덱스 선택");
                            }
                        });
                    } else {
                        fileName = fileRef.getName();  // 전역변수 : fileName 구하기
                        TimeStore(fileName);           // 공부 시작 시간 저장

                        // PDF 파일을 업로드하고 파일 경로 값을 전달하는 부분
                        String DirectoryPath = "users/" + user.getUid() + "/Subject/" + Subject;

                        Log.d(TAG, "FileAdapter : 전달된 파일 경로 : " + DirectoryPath);

                        // Intent를 생성하고 파일 경로 값을 설정하여 PdfViewerActivity로 전달 ( 뷰어 이동 )
                        Intent intent = new Intent(FileList.this, PDFViewerActivity.class);
                        intent.putExtra("DirectoryPath", DirectoryPath);
                        intent.putExtra("fileName", fileName);
                        intent.putExtra("Subject", Subject);    // 과목이름 전달
                        startActivity(intent);
                    }
                }
            });

            // 파일 삭제 버튼 클릭 이벤트 처리  : 체크박스 나타내기
            FileDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isDeleteButtonClicked = true; // 버튼이 클릭되었음을 표시
                    notifyDataSetChanged(); // 각 아이템 업데이트

                    startToast("삭제할 파일을 선택하세요");
                    MenuButton.setVisibility(View.GONE);    // 버튼 숨기기
                    Menu_XButton.setVisibility(View.GONE);
                    FileAddButton.setVisibility(View.GONE);
                    FileDeleteButton.setVisibility(View.GONE);
                    ManggagViewButton.setVisibility(View.GONE);
                    FileDeleteButton_Ok.setVisibility(View.VISIBLE);    // 버튼 보여주기
                    FileDeleteButton_Back.setVisibility(View.VISIBLE);
                }
            });

            // 파일 삭제 완료 버튼 이벤트 처리 : FireStore, Storage 내용 삭제
            FileDeleteButton_Ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // * FireStore 참조 변수 생성 : 파일리스트 참조 변수
                    CollectionReference FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                            .document(user.getUid())
                            .collection("SubjectCategory")
                            .document(subjectDocId)
                            .collection("FileInfo");

                    // * Storage 참조 변수 생성
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();

                    // * 삭제 여부를 묻는 Dialog

                    // Dialog Builder 생성
                    AlertDialog.Builder builder = new AlertDialog.Builder(FileList.this);

                    // Dialog 레이아웃 설정
                    View Dialog_view = LayoutInflater.from(FileList.this).inflate(R.layout.dialog_yes_or_back, null);
                    builder.setView(Dialog_view);

                    // Dialog 의 TextvView, Button 추가
                    TextView Text1 = Dialog_view.findViewById(R.id.TimeCheck_Text1);
                    TextView Text2 = Dialog_view.findViewById(R.id.TimeCheck_Text2);

                    String dynamicText1 = "정말로 삭제하시겠습니까?";      // TextView에 세팅하기위한 Text
                    String dynamicText2 = "<확인>버튼을 클릭하면 삭제됩니다.";
                    Text1.setText(dynamicText1);    // 텍스트 설정
                    Text2.setText(dynamicText2);

                    Button OKButton = Dialog_view.findViewById(R.id.TimeCheck_Ok_Button);            // 확인 버튼
                    Button BackButton = Dialog_view.findViewById(R.id.TimeCheck_Back_Button);        // 돌아가기 버튼

                    // Dialog 생성
                    AlertDialog alertDialog = builder.create();     // 객체 생성
                    alertDialog.show();         // 사용자에게 보여주기

                    // 확인 버튼 클릭 : 삭제하기
                    OKButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            // 선택한 체크박스가 없을 때
                            if(selectedIndexes.size() == 0) {
                                startToast("삭제할 파일을 선택해주세요.");
                            }
                            else {
                                // 사용자가 선택한 인덱스 가져오기
                                for (int position : selectedIndexes) {

                                    StorageReference selectedFiles = files.get(position);     // 가져온 인덱스를 통해 선택된 아이템 내용 가져오기
                                    String FileToDelete = selectedFiles.getName();     // 삭제하기 위한 파일 이름 가져오기

                                    // * << Firestore >> 문서 삭제 메서드 *
                                    Delete_FireStore(FileInfoRef, FileToDelete);

                                    // * << Storage >> 파일 삭제 메서드 *
                                    TodeleteFile(FileToDelete);
                                }

                                selectedIndexes.clear();            // 선택 상태와 맵 초기화
                                notifyDataSetChanged();             // 각 아이템 업데이트 ( 먼저 실행됨..)
                                checkBox.setChecked(false);         // 체크박스 초기화
                                checkBox.setVisibility(View.GONE);  // 체크박스 숨기기
                                isDeleteButtonClicked = false;   // '삭제' 버튼 클릭 상태 초기화
                                MenuCancleClick();          // 메뉴 초기화
                                startToast("파일 삭제가 완료되었습니다.");
                            }
                            alertDialog.dismiss();      // Dialog창 닫기
                        }
                    });

                    // 돌아가기 버튼 클릭 : 삭제할 과목 선택 화면으로 되돌아가기
                    BackButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { alertDialog.dismiss();  }   // Dialog창 닫기
                    });
                }
            });

            // 과목 삭제 취소 버튼 이벤트 처리 (돌아가기) : 체크박스 지우기
            FileDeleteButton_Back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkBox.setChecked(false);         // 체크박스
                    checkBox.setVisibility(View.GONE); // 체크박스 숨기기

                    MenuCancleClick();    // 메뉴 선택하기 버튼 생성 ( 메뉴 초기화 )
                    //startToast("파일리스트 삭제 취소");
                    isDeleteButtonClicked = false; // 버튼 클릭 상태 초기화

                    getFileList(Subject);
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

        // EditText에 세팅하기위한 Text
        String dynamicText1 = "파일 이름을 입력해주세요. (특수문자 제외)";
        fileNameEditText.setText(dynamicText1);    // 텍스트 설정

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
    private void TimeStore(String fileName) {
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
    private void UploadFireStore(String FileName, String subject) {

        // Firestore 초기화
        CollectionReference FileInfoRef = db.collection("users")   // 해당 파일의 문서 접근
                .document(user.getUid())
                .collection("SubjectCategory")
                .document(subjectDocId)
                .collection("FileInfo");

        // 현재 시간 가져오기
        Date currentTime = Calendar.getInstance().getTime();
        // 파일 경로 ( 과목 카테고리까지 )
        String DirectoryPath = "users/" + user.getUid() + "/Subject/" + Subject;

        Map<String, Object> FileInfoMap = new HashMap<>();      // 데이터를 저장할 Map 객체 생성
        FileInfoMap.put("SubjectCategoryId", subject);      // 해당 파일의 과목 카테고리(필드) 추가
        FileInfoMap.put("FileName", FileName);              // 식별자로 사용
        FileInfoMap.put("UploadDate", currentTime);         // 파일 업로드날짜
        FileInfoMap.put("DirectoryPath", DirectoryPath);         // 파일 경로

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
                                subjectDocId = document.getId();    // 아이디 구하기
                                Log.d(TAG,"ReturnDocRef 메서드 Return 값 : " + subjectDocId);
                            }
                        } else {
                            Log.d(TAG, "ReturnDocRef 메서드 : Error getting documents: ", task.getException());
                        }
                    }
                });
    }
    // 메뉴 선택하기 버튼 : 나머지 다른 옵션(과목 추가,삭제) 버튼들이 나오도록 함.
    private void MenuClick() {
        MenuButton.setVisibility(View.GONE);
        Menu_XButton.setVisibility(View.VISIBLE);
        FileAddButton.setVisibility(View.VISIBLE);
        FileDeleteButton.setVisibility(View.VISIBLE);
        ManggagViewButton.setVisibility(View.VISIBLE);
    }

    // 메뉴 선택 취소 버튼
    private void MenuCancleClick() {
        MenuButton.setVisibility(View.VISIBLE);
        Menu_XButton.setVisibility(View.GONE);
        FileAddButton.setVisibility(View.GONE);
        FileDeleteButton.setVisibility(View.GONE);
        ManggagViewButton.setVisibility(View.GONE);
        FileDeleteButton_Back.setVisibility(View.GONE);
        FileDeleteButton_Ok.setVisibility(View.GONE);
    }

    // FireStore에서 FileInfo 정보 삭제
    private void Delete_FireStore(CollectionReference FileInfoRef, String FileToDelete) {
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
                                //startToast("FireStore 파일 삭제 성공");
                                Log.d(TAG, FileToDelete + " : FireStore 파일 삭제 성공");

                            }).addOnFailureListener(e-> {
                                // 삭제 실패
                                Log.d(TAG,FileToDelete + " : FireStore 파일 삭제 실패 : " + e.getMessage());
                            });
                }
            } else {
                // 파일리스트 검색 실패 시 처리 ( Query )
                System.out.println("파일리스트 쿼리 실패: " + task.getException().getMessage());
            }
        });
    }
    // Storage에서 해당 파일 삭제
    private void TodeleteFile(String FileToDelete) {
        // * Storage 참조 변수 생성
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference FileRef = storageRef
                .child("users/" + user.getUid() + "/Subject/" + Subject + "/" + FileToDelete);

        // (Test) Storage 경로 : FileRef
        String Test_Ref = FileRef.toString();
        Log.d(TAG, "FileRef : 받아온 Storage 경로 : " + Test_Ref);

        // 해당 파일 삭제
        FileRef.delete()
                .addOnSuccessListener(taskSnapshot -> {
                    // 파일 삭제 성공
                    startToast(FileToDelete + " : 파일 삭제 성공");
                    Log.d(TAG, FileToDelete + " : 파일 삭제 성공");
                    getFileList(Subject);   // 화면 새로고침
                    Log.d(TAG,"deleteCategory : CategoryLoad() 실행");
                })
                .addOnFailureListener(exception -> {
                    // 파일 삭제 실패
                    startToast(FileToDelete + " : 파일 삭제 실패");
                    Log.d(TAG, FileToDelete + " : 파일 삭제 실패");
                });
    }

}