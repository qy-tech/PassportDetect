package com.qytech.securitycheck.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DbBean {
    @Id(autoincrement = true)
    private Long id;
    String username;
    String passWord;
    @Generated(hash = 1556954947)
    public DbBean(Long id, String username, String passWord) {
        this.id = id;
        this.username = username;
        this.passWord = passWord;
    }
    @Generated(hash = 1953169116)
    public DbBean() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassWord() {
        return this.passWord;
    }
    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }
}
