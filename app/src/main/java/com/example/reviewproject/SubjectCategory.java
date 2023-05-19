package com.example.reviewproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
    private ArrayList<String> subjectList;     // 카테고리 리스트 배열
    private FloatingActionButton SubjectAddButton;      // 과목 추가 버튼

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_subject_category);

        // inflate된 레이아웃에서 SubjectAddButton 찾아서 초기화
        SubjectAddButton = findViewById(R.id.SubjectAddButton);

        // 과목 추가 버튼 클릭 이벤트 처리
        SubjectAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCategory();
            }
        });

        // 리스트뷰와 어댑터 초기화
        listView = (ListView) findViewById(R.id.SubjectList);

        subjectList = new ArrayList<>();       // 데이터 배열 생성
        adapter =  new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjectList); // 넣을 레이아웃, 데이터 배열 선언
        listView.setAdapter(adapter);   // 리스트뷰와 연결시키기

        // 리스트뷰 아이템 클릭 이벤트 처리
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> SubjectAdapter, View view, int i, long l) {
                // 클릭한 항목의 정보를 가져옴
                String selectedSubject = ((TextView) view).getText().toString();

                // 선택한 항목의 정보를 Intent에 담아 File 클래스를 시작
                Intent intent = new Intent(SubjectCategory.this, FileList.class);
                intent.putExtra("selectedSubject", selectedSubject);
                startActivity(intent);
                Log.d(TAG, "전달한 과목 이름 : " + selectedSubject);
            }
        });

        // 카테고리 목록 불러오기
        CategoryLoad();
    }

    //카테고리 추가 버튼 클릭 시 동작할 메소드 : 과목 추가 Dialog 생성
    public void addCategory() {
        EditText editText = new EditText(this);     // 과목이름 입력받을 텍스트 뷰 생성
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)          // AlertDialog를 만들기 위한 Builder 생성
                .setTitle("과목 추가")
                .setMessage("새로운 과목을 입력해주세요.")
                .setView(editText)
                .setPositiveButton("추가", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String subject = editText.getText().toString();
                        subjectList.add(subject);           // 리스트에 과목 추가

                        //CategoryStore(subjectList);         // 입력한 과목을 FireStore에 저장
                        CategoryStore(subject);

                        adapter.notifyDataSetChanged();     // 어댑터에 데이터가 추가되었다고 알려주기
                        startToast("과목 추가 완료");
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

        Map<String, Object> subjectMap = new HashMap<>();      // 데이터를 저장할 Map 객체 생성
        subjectMap.put("subject", subject_name);     // subject 필드에 List 배열 값을 추가
                                                        // subject 필드에 과목이름 추가

        // FireStore에 데이터 추가
        userDocRef.collection("SubjectCategory")   // 현재 사용자의 SubjectCategory 서브컬렉션 접근
                .add(subjectMap)            // 데이터 추가
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {        // 성공적으로 추가되었을 때
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        //startToast("과목 추가 완료");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
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
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String subject_name = (String) document.get("subject");
                                subjectList.add(subject_name);
                                //startToast("과목 불러오기 성공");
                            }

                            adapter.notifyDataSetChanged();     // 어댑터에 데이터가 추가되었다고 알려주기
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            //startToast("과목 불러오기 실패");
                        }
                    }
                });
    }

    private void myStartActivity(Class c) {    // 원하는 화면으로 이동하는 함수 (화면 이동 함수)
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }
}
