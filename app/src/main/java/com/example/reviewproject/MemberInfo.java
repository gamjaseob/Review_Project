package com.example.reviewproject;

public class MemberInfo {
    private String name;
    private String email;

    public MemberInfo(String name, String email) {    // 생성자함수 ( 초기화 )
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return this.email;
    }
    public void setEmail(String phoneNumber) {
        this.email = email;
    }
}
