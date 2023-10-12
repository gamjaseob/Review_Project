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

public class Review_SubjectCategory extends AppCompatActivity {

    private static final String TAG = "Review_SubjectCategory";     // TAG 추가

    // 현재 로그인 되어있는지 확인 ( 현재 사용자 불러오기 )
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListView listView;
    private FloatingActionButton SubjectAddButton;      // 과목 추가 버튼
    private FloatingActionButton SubjectDeleteButton;      // 과목 삭제 버튼
    private FloatingActionButton SubjectDeleteButton_Back;     // 과목 삭제 취소 버튼
    private FloatingActionButton SubjectDeleteButton_Ok;     // 과목 삭제 완료 버튼
    private FloatingActionButton MenuButton;        // 메뉴 선택 버튼
    private FloatingActionButton Menu_XButton;      // 메뉴 선택 취소 버튼

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_subject_category);     // 사용하던 XML파일 사용

        startToast("복습하고자 하는 과목을 선택하세요");

        // inflate된 레이아웃에서 버튼 찾아 초기화
        SubjectAddButton = findViewById(R.id.SubjectAddButton);         // 과목 추가 버튼
        SubjectDeleteButton = findViewById(R.id.SubjectDeleteButton);   // 과목 삭제 버튼
        SubjectDeleteButton_Back = findViewById(R.id.SubjectDeleteButton_Back);   // 과목 삭제 "취소" 버튼
        SubjectDeleteButton_Ok = findViewById(R.id.SubjectDeleteButton_OK);   // 과목 삭제 "완료" 버튼
        MenuButton = findViewById(R.id.Subject_MenuButton);             // 메뉴 선택 버튼
        Menu_XButton = findViewById(R.id.Subject_Menu_XButton);         // 메뉴 선택 취소 버튼

        MenuRemove();    // 메뉴 버튼 지우기 ( 필요없으니 숨김 )

        startToast("집중모드 실행");

        // 리스트뷰와 어댑터 초기화
        listView = (ListView) findViewById(R.id.SubjectList);

        // if( 해당 과목 카테고리의 파일의 개수가 0라면, 과목 삭제 )
        // 삭제할 카테고리인지 확인
        Delete_Check();

        // But 나갔다가 다시 들어와야 삭제 완료된 화면이 Load 된다. : 수정 필요

        // 카테고리 목록 불러오기
        CategoryLoad();
    }

    // 과목 리스트를 출력하기위한 커스텀 어댑터
    public class Review_SubjectAdapter extends ArrayAdapter<StorageReference> {
        private static final String TAG = "Review_SubjectAdapter";
        private final Context context;
        private final List<StorageReference> subject;       // 과목 정보(데이터)를 담고 있는 리스트

        public Review_SubjectAdapter(Context context, List<StorageReference> subject) {     // 생성자, 멤버변수 초기화
            super(context, R.layout.list_item, subject);
            this.context = context;
            this.subject = subject;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Item의 현재 위치 구하기 ( 참조 변수 )
            StorageReference SubjectRef = subject.get(position);

            // 각 아이템 뷰에 해당하는 XML 파일을 inflate ( XML 파일 -> 실제 뷰 객체 )
            if (convertView == null) {
                // 일반 아이템 사용
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            // 텍스트뷰만 사용
            TextView SubjectnameView = (TextView) convertView.findViewById(R.id.item_name);

            // 과목 이름 설정
            SubjectnameView.setText(SubjectRef.getName());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {        // 과목 리스트 아이템 클릭 이벤트

                        // 클릭한 아이템의 정보를 가져옴 : 아이템 이름
                        String selectedSubject = SubjectRef.getName();
                        // 선택한 항목의 정보를 Intent에 담아 Review_File.Class를 시작
                        Intent intent = new Intent(Review_SubjectCategory.this, Review_FileList.class);
                        intent.putExtra("Subject", selectedSubject);    // 과목이름 전달
                        Log.d(TAG, "전달한 과목 이름 : " + selectedSubject);

                        startActivity(intent);
                    }
            });

            return convertView;
        }
    }

    // Toast 띄우는 함수
    private void startToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
    private void myStartActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }

    // 카테고리 불러오는 메소드
    private void CategoryLoad() {

        CollectionReference userRef = db.collection("users");   //  컬렉션 참조 변수
        DocumentReference userDocRef = userRef.document(user.getUid()); // 문서 참조 변수 : 현재 사용자 정보

        userDocRef.collection("Review_SubjectCategory")
                .get()  // 컬렉션 전체 데이터 가져오기
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<StorageReference> subjectList = new ArrayList<>(); // StorageReference 리스트를 담을 리스트

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subject_name = (String) document.get("subject");

                                // StorageReference를 생성하여 리스트에 추가
                                if (subject_name != null) {     // 삭제되지 않은 카테고리인 경우
                                    subjectList.add(FirebaseStorage.getInstance().getReference().child(subject_name));
                                } else {  startToast("과목 불러오기 실패" + "\n 다시 시도해 주세요.");}
                            }

                            // 어댑터를 생성하고 리스트뷰에 설정
                            Review_SubjectAdapter adapter =
                                    new Review_SubjectAdapter(Review_SubjectCategory.this, subjectList);
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

    // 메뉴 가리기 버튼 ( 초기화 )
    private void MenuRemove() {
        MenuButton.setVisibility(View.GONE);
        Menu_XButton.setVisibility(View.GONE);
        SubjectAddButton.setVisibility(View.GONE);
        SubjectDeleteButton.setVisibility(View.GONE);
        SubjectDeleteButton_Back.setVisibility(View.GONE);
        SubjectDeleteButton_Ok.setVisibility(View.GONE);
    }

    // 삭제해야할 카테고리인지 검사하는 메서드
    private void Delete_Check() {
        CollectionReference userRef = db.collection("users");
        DocumentReference userDocRef = userRef.document(user.getUid());

        userDocRef.collection("Review_SubjectCategory")
                .get()  // 컬렉션 전체 데이터 가져오기
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subject_name = (String) document.get("subject");

                                Delete_FireStore(subject_name);     // 삭제할 카테고리인지 확인
                            }
                        } else {
                            Log.d(TAG, "Delete_Check : Review_SubjectCategory 컬렉션 불러오기 실패 : Error getting documents: ", task.getException());
                            //startToast("Review_SubjectCategory 컬렉션 불러오기" + "\n 다시 시도해 주세요.");
                        }
                    }
                });
    }

    // FireStore에서 Review_SubjectCategory 정보 삭제 : 해당 과목의 모든 파일을 복습 완료 했을 때
    // 즉, 과목 카테고리 안에 존재하는 파일이 없을 때 자동 삭제해주는 메서드
    private void Delete_FireStore(String SubjectToDelete) {

        CollectionReference userRef = db.collection("users");
        DocumentReference userDocRef = userRef.document(user.getUid());

        // Review_SubjectCategory 컬렉션 접근
        CollectionReference subjectCategoryRef = userDocRef
                .collection("Review_SubjectCategory");

        subjectCategoryRef.whereEqualTo("subject", SubjectToDelete)     // 받아온 값과 일치하는 문서 찾기
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subjectDocId = document.getId();    // 아이디 구하기
                                Log.d(TAG," Delete_FireStore 메서드 subjectDocId : " + subjectDocId + " : " + SubjectToDelete);

                                // Review_FileInfo 컬렉션 접근   : 해당 과목의 파일의 개수가 0인지 조사
                                CollectionReference FileInfoRef = subjectCategoryRef
                                        .document(subjectDocId)
                                        .collection("Review_FileInfo");

                                // 하위 컬렉션의 모든 문서를 쿼리
                                FileInfoRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                                        if (queryDocumentSnapshots.isEmpty()) {    // Review_FileList 컬렉션이 없는 경우

                                            // 과목 카테고리 삭제 (문서 삭제)
                                            subjectCategoryRef.document(subjectDocId)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        // 상위 문서 (과목 카테고리) 삭제 성공 시 처리
                                                        //startToast("FireStore (복습 필요) 과목 삭제 성공");
                                                        Log.d(TAG, "FireStore (복습 필요) 과목 삭제 성공 : " + SubjectToDelete);

                                                        CategoryLoad();     // 여러번 호출..
                                                        // 구현 완료 후에 다시 한 번 확인하기
                                                        Log.d(TAG, "Delete_FireStore : CategoryLoad() 메서드 실행");
                                                    }).addOnFailureListener(e -> {
                                                        // 상위 문서 삭제 실패
                                                        Log.d(TAG, "FireStore (복습 필요) 과목 삭제 실패 : " + SubjectToDelete + " : " + e.getMessage());
                                                    });

                                        } else {    // 파일이 남아있다면, 삭제를 하지않는다.
                                            Log.d(TAG, "카테고리 삭제 Pass : 복습 필요한 파일이 남아있음 : " + SubjectToDelete);
                                            //startToast("카테고리 삭제 Pass" + "\n 복습 필요한 파일이 남아있음");
                                        }

                                }).addOnFailureListener(e -> {
                                    // 하위 컬렉션 쿼리 실패
                                    Log.d(TAG, SubjectToDelete + " : Review_FileInfo 문서 쿼리 실패 " + e.getMessage());
                                });
                            }
                        } else {
                            Log.d(TAG, "ReturnDocRef 메서드 : Error getting documents: ", task.getException());
                        }
                    }
                });

    }

}