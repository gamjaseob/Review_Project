package com.example.reviewproject;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class SubjectCategory extends AppCompatActivity {

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
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
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
}
