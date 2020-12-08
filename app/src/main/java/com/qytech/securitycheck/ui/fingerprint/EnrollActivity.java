package com.qytech.securitycheck.ui.fingerprint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qytech.securitycheck.R;
import com.qytech.securitycheck.db.DbBean;
import com.qytech.securitycheck.utils.MD5Utils;
import com.qytech.securitycheck.utils.MeiziDaoUtils;
import com.qytech.securitycheck.utils.SpUtil;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnrollActivity extends AppCompatActivity {

    private Button btn_register;
    private EditText et_user_name, et_psw, et_psw_again;
    private String userName, psw, pswAgain;
    private MeiziDaoUtils meiziDaoUtils;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);
        meiziDaoUtils = new MeiziDaoUtils(this);
        Toolbar toolbar = findViewById(R.id.toolbars);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        initEnroll();
    }

    private void initEnroll() {
        btn_register = findViewById(R.id.btn_register);
        et_user_name = findViewById(R.id.et_user_name);
        et_psw = findViewById(R.id.et_psw);
        et_psw_again = findViewById(R.id.et_psw_again);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getEditString();
                if (TextUtils.isEmpty(userName)) {
                    Toast toast = Toast.makeText(EnrollActivity.this, "请输入用户名", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (TextUtils.isEmpty(psw)) {
                    Toast toast = Toast.makeText(EnrollActivity.this, "请输入密码", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (TextUtils.isEmpty(pswAgain)) {
                    Toast toast = Toast.makeText(EnrollActivity.this, "请再次输入密码", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (!psw.equals(pswAgain)) {
                    Toast toast = Toast.makeText(EnrollActivity.this, "输入两次的密码不一样", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (isExistUserName(userName)) {
                    Toast toast = Toast.makeText(EnrollActivity.this, "此账户名已经存在", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (et_psw.getText().toString().length() < 5 || et_psw_again.getText().toString().length() < 5) {
                    Toast toast = Toast.makeText(EnrollActivity.this, "密码不能少于五位", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(EnrollActivity.this, "注册成功", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                    saveRegisterInfo(userName, psw);
                    SpUtil.INSTANCE.saveData(EnrollActivity.this,"enrollUsername",userName);
                    Intent data = new Intent();
                    data.putExtra("userName", userName);
                    setResult(RESULT_OK, data);
                    DbBean dbBean = new DbBean(null, userName, null);
                    meiziDaoUtils.insertMeizi(dbBean);
                    finish();
                }
            }
        });
    }

    private void getEditString() {
        userName = et_user_name.getText().toString().trim();
        psw = et_psw.getText().toString().trim();
        pswAgain = et_psw_again.getText().toString().trim();
    }

    private boolean isExistUserName(String userName) {
        boolean has_userName = false;
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
        String spPsw = sp.getString(userName, "");
        if (!TextUtils.isEmpty(spPsw)) {
            has_userName = true;
        }
        return has_userName;
    }

    @SuppressLint("ApplySharedPref")
    private void saveRegisterInfo(String userName, String psw) {
        String md5Psw = MD5Utils.md5(psw);
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(userName, md5Psw);
        editor.commit();
    }
}