package com.qytech.securitycheck.ui.fingerprint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qytech.securitycheck.MainActivity;
import com.qytech.securitycheck.R;
import com.qytech.securitycheck.adapter.LoginRecyclerView;
import com.qytech.securitycheck.db.DbBean;
import com.qytech.securitycheck.utils.MD5Utils;
import com.qytech.securitycheck.utils.MeiziDaoUtils;
import com.qytech.securitycheck.utils.PreferenceUtils;
import com.qytech.securitycheck.utils.SpUtil;
import com.szadst.szoemhost_lib.HostLib;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private Button btn_login;
    private String userName, psw, spPsw;
    private EditText et_user_name, et_psw;
    private Button tv_register;
    boolean m_bForce = false;
    private ImageView img;
    private MeiziDaoUtils meiziDaoUtils;
    private LoginRecyclerView loginRecyclerView;

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
        setContentView(R.layout.pop_login);
        meiziDaoUtils = new MeiziDaoUtils(this);
        Toolbar toolbar = findViewById(R.id.toolbars);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        initLogin();
        String enrollUsername = SpUtil.INSTANCE.getData(this, "enrollUsername");
        et_user_name.setText(enrollUsername);
        Integer isOnclickLogin = PreferenceUtils.INSTANCE.findPreference("isOnclickLogin", 10);
        if (isOnclickLogin == 10) {
            String editusername = SpUtil.INSTANCE.getData(this, "editusername");
            et_user_name.setText(editusername);
        }
    }

    private void initLogin() {
        tv_register = findViewById(R.id.tv_register);
        btn_login = findViewById(R.id.btn_login);
        et_user_name = findViewById(R.id.et_user_name);
        et_psw = findViewById(R.id.et_psw);
        img = findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<DbBean> dbBeans = meiziDaoUtils.queryAllMeizi();
                View inflate = LayoutInflater.from(LoginActivity.this).inflate(R.layout.popupwindow_login, null);
                ImageView delete = inflate.findViewById(R.id.delete);
                if (dbBeans.size() <= 0) {
                    delete.setVisibility(View.INVISIBLE);
                }
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder normalDialog = new AlertDialog.Builder(LoginActivity.this);
                        normalDialog.setTitle("确定要删除？");
                        normalDialog.setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        et_user_name.setText("");
                                        SpUtil.INSTANCE.clearData(LoginActivity.this, "loginusername"); //清除当前所登录的用户
                                        SpUtil.INSTANCE.clearData(LoginActivity.this, "loginInfo");     //清除所有用户
                                        SpUtil.INSTANCE.clearData(LoginActivity.this, "editusername"); //选择列表里的其他用户
                                        SpUtil.INSTANCE.clearData(LoginActivity.this, "enrollUsername"); //清除注册保存的账号
                                        PreferenceUtils.INSTANCE.putPreference("isOnclickLogin", 1); //是否选中列表里的用户
                                        PreferenceUtils.INSTANCE.putPreference("TEMPLATE_NO", 1); //删除所有用户指纹置为1
                                        PreferenceUtils.INSTANCE.putPreference("isLogins", 1); //清除账号登陆状态
                                        PreferenceUtils.INSTANCE.clear();//清除所有
                                        meiziDaoUtils.deleteAll(); //删除数据库
                                        HostLib.getInstance(LoginActivity.this).FPCmdProc().Run_CmdDeleteAll(m_bForce); //清除所有指纹
                                        finish();
                                    }
                                });
                        normalDialog.setNegativeButton("关闭",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                        normalDialog.show();
                    }
                });
                PopupWindow popupWindow = new PopupWindow(inflate, 1200, 500, true);
                RecyclerView recyclerView = inflate.findViewById(R.id.recycler);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(LoginActivity.this);
                linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                recyclerView.setLayoutManager(linearLayoutManager);
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(LoginActivity.this, DividerItemDecoration.VERTICAL);
                recyclerView.addItemDecoration(dividerItemDecoration);
                loginRecyclerView = new LoginRecyclerView(LoginActivity.this, dbBeans);
                loginRecyclerView.notifyDataSetChanged();
                recyclerView.setAdapter(loginRecyclerView);
                loginRecyclerView.setOnItemClickListener(new LoginRecyclerView.OnItemClickListener() {
                    @Override
                    public void onClick(int position) {
                        PreferenceUtils.INSTANCE.putPreference("isOnclickLogin", 10);
                        et_user_name.setText(dbBeans.get(position).getUsername());
                        SpUtil.INSTANCE.saveData(LoginActivity.this, "editusername", dbBeans.get(position).getUsername());
                        popupWindow.dismiss();
                    }
                });
                popupWindow.setContentView(inflate);
                popupWindow.setFocusable(true);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setAnimationStyle(R.style.popmenu_animation);
                popupWindow.showAtLocation(inflate, Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, -200);
            }
        });

        tv_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, EnrollActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = et_user_name.getText().toString().trim();
                psw = et_psw.getText().toString().trim();
                String md5Psw = MD5Utils.md5(psw);
                spPsw = readPsw(userName);
                if (TextUtils.isEmpty(userName)) {
                    Toast toast = Toast.makeText(LoginActivity.this, "请输入用户名", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (TextUtils.isEmpty(psw)) {
                    Toast toast = Toast.makeText(LoginActivity.this, "请输入密码", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else if (md5Psw.equals(spPsw)) {
                    btn_login.setText("登录");
                    if (btn_login.getText().toString().equals("登录")) {
                        Toast toast = Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT);
                        LinearLayout layout = (LinearLayout) toast.getView();
                        TextView tv = (TextView) layout.getChildAt(0);
                        tv.setTextSize(30);
                        tv.setTextColor(Color.WHITE);
                        toast.getView().setBackgroundColor(Color.DKGRAY);
                        toast.show();
                        saveLoginStatus(userName);
                        Intent data = new Intent();
                        data.putExtra("isLogin", true);
                        setResult(RESULT_OK, data);
                        PreferenceUtils.INSTANCE.putPreference("isLogins", 13);
                        SpUtil.INSTANCE.saveData(LoginActivity.this, "loginusername", userName);
                        finish();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                } else if ((spPsw != null && !TextUtils.isEmpty(spPsw) && !md5Psw.equals(spPsw))) {
                    Toast toast = Toast.makeText(LoginActivity.this, "输入的用户名和密码不一致", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(LoginActivity.this, "此用户名不存在", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PreferenceUtils.INSTANCE.findPreference("isLogins", 13) == 13) {
            btn_login.setText("退出登录");
            et_user_name.setInputType(InputType.TYPE_NULL);
            img.setVisibility(View.INVISIBLE);
            tv_register.setVisibility(View.INVISIBLE);
            et_psw.setText("     ");
            et_psw.setInputType(InputType.TYPE_NULL);
            et_psw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder normalDialog = new AlertDialog.Builder(LoginActivity.this);
                    normalDialog.setTitle("确定要退出登录？");
                    normalDialog.setPositiveButton("确定",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SpUtil.INSTANCE.clearData(LoginActivity.this, "enrollUsername");
                                    SpUtil.INSTANCE.clearData(LoginActivity.this, "editusername");
                                    SpUtil.INSTANCE.clearData(LoginActivity.this, "loginusername");
                                    SpUtil.INSTANCE.clearData(LoginActivity.this, "password");
                                    PreferenceUtils.INSTANCE.putPreference("isOnclickLogin", 1);
                                    PreferenceUtils.INSTANCE.putPreference("isLogins", 1);
                                    et_user_name.setText("");
                                    et_psw.setText("");
                                    btn_login.setText("登录");
                                    img.setVisibility(View.VISIBLE);
                                    tv_register.setVisibility(View.VISIBLE);
                                    et_user_name.setInputType(InputType.TYPE_CLASS_TEXT);
                                    finish();

                                }
                            });
                    normalDialog.setNegativeButton("关闭",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    normalDialog.show();
                }
            });
        }
    }

    private String readPsw(String userName) {
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
        return sp.getString(userName, "");
    }

    @SuppressLint("ApplySharedPref")
    private void saveLoginStatus(String userName) {
        SharedPreferences sp = getSharedPreferences("loginInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("isLogin", true);
        editor.putString("loginUserName", userName);
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            String userName = data.getStringExtra("userName");
            if (!TextUtils.isEmpty(userName)) {
                et_user_name.setText(userName);
                et_user_name.setSelection(userName.length());
            }
            SpUtil.INSTANCE.saveData(LoginActivity.this, "loginusername", userName);
        }
    }
}
