package com.example.reviewproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
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
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubjectCategory extends AppCompatActivity {

    private static final String TAG = "SubjectCategoryActivity";     // TAG 추가

    // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    private ListView listView;
    private ArrayAdapter<String> adapter;   // 어댑터 ( ListView와 데이터 배열의 다리 역할 )
    private FloatingActionButton SubjectAddButton;      // 과목 추가 버튼
    private FloatingActionButton SubjectDeleteButton;      // 과목 삭제 버튼
    private FloatingActionButton SubjectDeleteButton_Back;     // 과목 삭제 취소 버튼
    private FloatingActionButton SubjectDeleteButton_Ok;     // 과목 삭제 완료 버튼
    private FloatingActionButton MenuButton;        // 메뉴 선택 버튼
    private FloatingActionButton Menu_XButton;      // 메뉴 선택 취소 버튼

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_subject_category);

        // inflate된 레이아웃에서 버튼 찾아 초기화
        SubjectAddButton = findViewById(R.id.SubjectAddButton);         // 과목 추가 버튼
        SubjectDeleteButton = findViewById(R.id.SubjectDeleteButton);   // 과목 삭제 버튼
        SubjectDeleteButton_Back = findViewById(R.id.SubjectDeleteButton_Back);   // 과목 삭제 "취소" 버튼
        SubjectDeleteButton_Ok = findViewById(R.id.SubjectDeleteButton_OK);   // 과목 삭제 "완료" 버튼
        MenuButton = findViewById(R.id.Subject_MenuButton);             // 메뉴 선택 버튼
        Menu_XButton = findViewById(R.id.Subject_Menu_XButton);         // 메뉴 선택 취소 버튼

        MenuCancleClick();    // 메뉴 선택하기 버튼 생성 ( 메뉴 초기화 )

        // 메뉴 선택하기 버튼 이벤트 처리 : 버튼 클릭했을 때 과목 추가&삭제 버튼 나옴
        MenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { MenuClick(); }
        });

        // 메뉴 선택 취소 버튼 이벤트 처리 : 버튼 클릭했을 때 과목 추가&삭제 버튼 사라짐
        Menu_XButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { MenuCancleClick(); }
        });

        // 과목 추가 버튼 클릭 이벤트 처리
        SubjectAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { addCategory(); }
        });

        // 리스트뷰와 어댑터 초기화
        listView = (ListView) findViewById(R.id.SubjectList);

        // 카테고리 목록 불러오기
        CategoryLoad();
    }

    // 과목 리스트를 출력하기위한 커스텀 어댑터
    public class SubjectAdapter extends ArrayAdapter<StorageReference> {
        private static final String TAG = "SubjectAdapter";
        private final Context context;
        private final List<StorageReference> subject;       // 과목 정보(데이터)를 담고 있는 리스트
        private final SparseBooleanArray selectedItems;     // 체크박스 선택 상태 저장
        boolean isSubjectDeleteButtonClicked; // (과목 삭제)SubjectDeleteButton 버튼 클릭 여부를 나타내는 변수
        final List<Integer> selectedIndexes;     // 선택된 아이템의 인덱스를 저장할 리스트

        public SubjectAdapter(Context context, List<StorageReference> subject) {     // 생성자, 멤버변수 초기화
            super(context, R.layout.list_item, subject);
            this.context = context;
            this.subject = subject;
            this.selectedItems = new SparseBooleanArray();
            this.selectedIndexes = new ArrayList<>();
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Item의 현재 위치 구하기 ( 참조 변수 )
            StorageReference SubjectRef = subject.get(position);

            // 각 아이템 뷰에 해당하는 XML 파일을 inflate ( XML 파일 -> 실제 뷰 객체 )
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_checkbox, parent, false);
            }

            final CheckBox checkBox = convertView.findViewById(R.id.checkBox);
            TextView SubjectnameView = (TextView) convertView.findViewById(R.id.item_name);

            // 체크박스 상태 초기화
            checkBox.setChecked(selectedItems.get(position, false));

            // 과목 이름 설정
            SubjectnameView.setText(SubjectRef.getName());

            // 체크박스 표시 여부 ( 삭제 버튼 안눌렀을 땐 표시 X )
            if (isSubjectDeleteButtonClicked) {
                // 체크박스 설정
                checkBox.setChecked(selectedItems.get(position, false));
                checkBox.setVisibility(View.VISIBLE);

            } else {
                checkBox.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {        // 과목 리스트 아이템 클릭 이벤트

                    if (isSubjectDeleteButtonClicked) {     // '삭제'버튼 클릭 후 아이템을 클릭했을 때의 동작
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

                    } else {        // 다른 동작 처리 (아이템 클릭 시의 다른 동작)

                        // 클릭한 아이템의 정보를 가져옴 : 아이템 이름
                        String selectedSubject = SubjectRef.getName();
                        // 선택한 항목의 정보를 Intent에 담아 File.Class를 시작
                        Intent intent = new Intent(SubjectCategory.this, FileList.class);
                        intent.putExtra("selectedSubject", selectedSubject);    // 과목이름 전달
                        Log.d(TAG, "전달한 과목 이름 : " + selectedSubject);

                        startActivity(intent);
                    }
                }
            });

            // 과목 삭제 버튼 클릭 이벤트 처리  : 체크박스 나타내기
            SubjectDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        isSubjectDeleteButtonClicked = true; // 버튼이 클릭되었음을 표시
                        notifyDataSetChanged(); // 각 아이템 업데이트

                        startToast("삭제할 과목을 선택하세요");
                        MenuButton.setVisibility(View.GONE);    // 버튼 숨기기
                        Menu_XButton.setVisibility(View.GONE);
                        SubjectAddButton.setVisibility(View.GONE);
                        SubjectDeleteButton.setVisibility(View.GONE);
                        SubjectDeleteButton_Ok.setVisibility(View.VISIBLE);
                        SubjectDeleteButton_Back.setVisibility(View.VISIBLE);
                }
            });

            // 과목 삭제 완료 버튼 이벤트 처리 : FireStore, Storage 내용 삭제
            SubjectDeleteButton_Ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // * FireStore 참조 변수 생성 : 과목 카테고리 참조 변수
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    CollectionReference subjectCategoryRef = db.collection("users")
                            .document(user.getUid())
                            .collection("SubjectCategory");

                    // * Storage 참조 변수 생성
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();

                    // * 삭제 여부를 묻는 Dialog

                        // Dialog Builder 생성
                        AlertDialog.Builder builder = new AlertDialog.Builder(SubjectCategory.this);

                        // Dialog 레이아웃 설정
                        View Dialog_view = LayoutInflater.from(SubjectCategory.this).inflate(R.layout.dialog_yes_or_back, null);
                        builder.setView(Dialog_view);

                        // Dialog 의 TextvView, Button 추가
                        TextView Text1 = Dialog_view.findViewById(R.id.TimeCheck_Text1);
                        TextView Text2 = Dialog_view.findViewById(R.id.TimeCheck_Text2);

                        String dynamicText1 = "정말로 삭제하시겠습니까?";      // TextView에 세팅하기위한 Text
                        String dynamicText2 = "<확인>버튼을 클릭하면 하위 파일이 모두 삭제됩니다.";
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
                                    startToast("삭제할 과목을 선택해주세요.");
                                }
                                else {
                                    // 사용자가 선택한 인덱스 가져오기
                                    for (int position : selectedIndexes) {

                                        StorageReference selectedSubject = subject.get(position);     // 가져온 인덱스를 통해 선택된 아이템 내용 가져오기
                                        String SubjectToDelete = selectedSubject.getName();     // 삭제하기 위한 과목 이름 가져오기

                                        // * << Firestore >> 문서 삭제 메서드 *
                                        Delete_FireStore(subjectCategoryRef, SubjectToDelete);

                                        // * << Storage >> 파일 삭제 메서드 *
                                        deleteCategory(SubjectToDelete);
                                    }

                                    selectedIndexes.clear();            // 선택 상태와 맵 초기화
                                    notifyDataSetChanged();             // 각 아이템 업데이트 ( 먼저 실행됨..)
                                    checkBox.setChecked(false);         // 체크박스 초기화
                                    checkBox.setVisibility(View.GONE);  // 체크박스 숨기기
                                    isSubjectDeleteButtonClicked = false;   // '삭제' 버튼 클릭 상태 초기화
                                    MenuCancleClick();          // 메뉴 초기화
                                    startToast("과목 삭제가 완료되었습니다.");
                                }
                                alertDialog.dismiss();      // Dialog창 닫기
                            }
                        });

                    // 돌아가기 버튼 클릭 : 삭제할 과목 선택 화면으로 되돌아가기
                    BackButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { alertDialog.dismiss();      // Dialog창 닫기
                        }
                    });
                }
            });

            // 과목 삭제 취소 버튼 이벤트 처리 (돌아가기) : 체크박스 지우기
            SubjectDeleteButton_Back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkBox.setChecked(false);         // 체크박스
                    checkBox.setVisibility(View.GONE); // 체크박스 숨기기

                    MenuCancleClick();    // 메뉴 선택하기 버튼 생성 ( 메뉴 초기화 )
                    //startToast("과목 카테고리 삭제 취소");
                    isSubjectDeleteButtonClicked = false; // 버튼 클릭 상태 초기화

                    CategoryLoad();
                }
            });

            return convertView;
        }
    }

    // 카테고리 추가 버튼 클릭 시 동작할 메소드 : 과목 추가 Dialog 생성
    private void addCategory() {
        EditText editText = new EditText(this);     // 과목이름 입력받을 텍스트 뷰 생성
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)          // AlertDialog를 만들기 위한 Builder 생성
                .setTitle("과목 추가")
                .setMessage("새로운 과목을 입력해주세요.")
                .setView(editText)
                .setPositiveButton("추가", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String subject = editText.getText().toString();
                        CategoryStore(subject);         // 입력한 과목을 FireStore에 저장
                        CategoryLoad();
                   }
                })
                .setNegativeButton("취소",null);
        dialog.show();
    }

    private void startToast(String msg) {     // Toast 띄우는 함수
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 입력한 과목을 FireStore에 저장하는 메소드
    private void CategoryStore(String subject_name) {    //ArrayList<String> List
        FirebaseFirestore db = FirebaseFirestore.getInstance();     // FireStore 인스턴스 가져오기
        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        DocumentReference userDocRef = userRef.document(user.getUid()); // 문서 참조 변수 : 현재 사용자 정보

        CollectionReference subjectRef = userDocRef.collection("SubjectCategory");  // 현재 사용자의 SubjectCategory 서브컬렉션 접근

        Map<String, Object> subjectMap = new HashMap<>();      // 데이터를 저장할 Map 객체 생성
        subjectMap.put("subject", subject_name);     // subject 필드에 List 배열 값을 추가
                                                        // subject 필드에 과목이름 추가

        // FireStore에 데이터 추가
        subjectRef.add(subjectMap)            // 데이터 추가
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {        // 성공적으로 추가되었을 때
                        String documentId = documentReference.getId();      // 문서 식별자 가져오기
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        startToast("과목 추가 완료");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        startToast("과목 추가 실패");
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    // 카테고리 불러오는 메소드
    private void CategoryLoad() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();     // FireStore 인스턴스 가져오기
        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        DocumentReference userDocRef = userRef.document(user.getUid()); // 문서 참조 변수 : 현재 사용자 정보

        userDocRef.collection("SubjectCategory")   // 현재 사용자의 SubjectCategory 서브컬렉션 접근
                .get()  // 컬렉션 전체 데이터 가져오기
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<StorageReference> subjectList = new ArrayList<>(); // StorageReference 리스트를 담을 리스트

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subject_name = (String) document.get("subject");
                                // StorageReference를 생성하여 리스트에 추가
                                if (subject_name != null) {     // null 처리
                                    subjectList.add(FirebaseStorage.getInstance().getReference().child(subject_name));
                                } else {  startToast("과목 불러오기 실패" + "\n 다시 시도해 주세요.");}
                            }

                            // 어댑터를 생성하고 리스트뷰에 설정
                            SubjectAdapter adapter = new SubjectAdapter(SubjectCategory.this, subjectList);
                            listView.setAdapter(adapter);

                            // 어댑터에 데이터가 변경되었음을 알려줌
                            adapter.notifyDataSetChanged();
                            //startToast("과목 불러오기 성공");
                            Log.d(TAG,"과목 불러오기 성공");
                        } else {
                            Log.d(TAG, "과목 불러오기 실패 : Error getting documents: ", task.getException());
                            startToast("과목 불러오기 실패" + "\n 다시 시도해 주세요.");
                        }
                    }
                });
    }
    private void myStartActivity(Class c) {    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }

    // 메뉴 선택하기 버튼 : 나머지 다른 옵션(과목 추가,삭제) 버튼들이 나오도록 함.
    private void MenuClick() {
        MenuButton.setVisibility(View.GONE);
        Menu_XButton.setVisibility(View.VISIBLE);
        SubjectAddButton.setVisibility(View.VISIBLE);
        SubjectDeleteButton.setVisibility(View.VISIBLE);

    }
    // 메뉴 선택 취소 버튼 ( 초기화 )
    private void MenuCancleClick() {
        MenuButton.setVisibility(View.VISIBLE);
        Menu_XButton.setVisibility(View.GONE);
        SubjectAddButton.setVisibility(View.GONE);
        SubjectDeleteButton.setVisibility(View.GONE);
        SubjectDeleteButton_Back.setVisibility(View.GONE);
        SubjectDeleteButton_Ok.setVisibility(View.GONE);
    }

    // FireStore에서 SubjectCategory, FileInfo 정보 삭제
    private void Delete_FireStore(CollectionReference subjectCategoryRef, String SubjectToDelete) {
        // 'SubjectCategory' 컬렉션에서 'subject' 필드 값이 지정한 값과 일치하는 문서를 검색
        Query query = subjectCategoryRef.whereEqualTo("subject", SubjectToDelete);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 일치하는 문서를 찾았을 때 삭제 ( 과목 카테고리 )
                for (QueryDocumentSnapshot document : task.getResult()) {

                    // 해당 과목 문서 ID 추출
                    String documentId = document.getId();

                    // 과목 카테고리 삭제 (문서 삭제)
                    subjectCategoryRef.document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // 상위 문서 (과목 카테고리) 삭제 성공 시 처리
                                //startToast("FireStore 과목 삭제 성공");
                                Log.d(TAG, "FireStore 과목 삭제 성공");

                                // 삭제하려는 FileInfo 컬렉션 접근 ( 하위 컬렉션 접근 )
                                CollectionReference FileInfoRef = subjectCategoryRef.document(documentId)
                                        .collection("FileInfo");

                                // 하위 컬렉션의 모든 문서를 쿼리하고 삭제
                                FileInfoRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot subDocument : queryDocumentSnapshots) {
                                        subDocument.getReference().delete()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    // 하위 컬렉션 문서 삭제 성공
                                                    Log.d(TAG, SubjectToDelete +" : FileInfo 문서 삭제 성공");
                                                    //startToast("FileInfo 문서 삭제 성공");
                                                })
                                                .addOnFailureListener(e -> {
                                                    // 하위 컬렉션 문서 삭제 실패
                                                    Log.d(TAG, SubjectToDelete +" : FileInfo 문서 삭제 실패" + e.getMessage());
                                                    //startToast("FileInfo 문서 삭제 실패");
                                                });
                                    }
                                }).addOnFailureListener(e -> {
                                    // 하위 컬렉션 쿼리 실패
                                    Log.d(TAG, SubjectToDelete + " : FileInfo 문서 쿼리 실패 " + e.getMessage());
                                });
                            }).addOnFailureListener(e-> {
                                // 상위 문서 삭제 실패
                                Log.d(TAG, "FireStore 과목 삭제 실패 : " + e.getMessage());
                            });
                }
            } else {
                // 과목 카테고리 검색 실패 시 처리 ( Query )
                System.out.println("쿼리 실패: " + task.getException().getMessage());
            }
        });
    }
    // Storage에서 해당 디렉토리와 파일 삭제
    private void deleteCategory(String SubjectToDelete) {
        // * Storage 참조 변수 생성
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference DirectoryRef = storageRef
                .child("users/" + user.getUid() + "/Subject/" + SubjectToDelete);

        // Storage 경로 : DirectoryRef
        String Test_Ref = DirectoryRef.toString();
        Log.d(TAG, "DirectoryRef : 받아온 Storage 경로 : " + Test_Ref);

        DirectoryRef.listAll()
                .addOnSuccessListener(listResult -> {
                    if (listResult.getItems().isEmpty()) {
                        // 하위 파일이 존재하지 않는 경우 디렉토리만 삭제 ( 아무 작업 없음 )
                        Log.d(TAG, SubjectToDelete + " : 하위 파일 없음, 디렉토리만 삭제");
                    } else {
                        // 하위 파일이 존재하는 경우 전체 삭제
                        for (StorageReference fileRef : listResult.getItems()) {
                            fileRef.delete()
                                    .addOnSuccessListener(taskSnapshot -> {
                                        // 파일 삭제 성공
                                        startToast(SubjectToDelete + " : 파일 삭제 성공");
                                        Log.d(TAG, SubjectToDelete + " : 파일 삭제 성공");
                                    })
                                    .addOnFailureListener(exception -> {
                                        // 파일 삭제 실패
                                        startToast(SubjectToDelete + " : 파일 삭제 실패");
                                        Log.d(TAG, SubjectToDelete + " : 파일 삭제 실패");
                                    });
                        }
                    }
                    // 하위 파일이 전부 삭제되면, 디렉토리는 자동 삭제

                   CategoryLoad();      // 화면 새로 고침 ( 여러번 호출..)
                   Log.d(TAG,"deleteCategory : CategoryLoad() 실행");
                })
                .addOnFailureListener(exception -> {
                    // 디렉토리 내 파일리스트 조회 실패
                    startToast(SubjectToDelete + " : 파일리스트 조회 실패");
                    Log.d(TAG, SubjectToDelete + " : 파일리스트 조회 실패");
                });
    }
}