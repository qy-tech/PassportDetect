package com.qytech.securitycheck.ui.fingerprint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.qytech.securitycheck.GlobalApplication;
import com.qytech.securitycheck.R;
import com.qytech.securitycheck.utils.PreferenceUtils;
import com.qytech.securitycheck.utils.SpUtil;
import com.szadst.szoemhost_lib.CommandProc;
import com.szadst.szoemhost_lib.DevComm;
import com.szadst.szoemhost_lib.HostLib;
import com.szadst.szoemhost_lib.IFPListener;


public class FingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    Button m_btnEnroll;
    Button m_btnIdentify;
    Button m_btnGetUserCount;
    Button m_btnDeleteAll;
    EditText m_editUserID;
    TextView m_txtStatus;
    Spinner m_spBaudrate;
    Spinner m_spDevice;
    String m_strPost;
    boolean m_bForce = false;
    private PopupWindow popupWindow;
    private LayoutInflater inflater;
    private View inflate;
    private int w_nTemplateNo;

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
        setContentView(R.layout.activity_fingerprint);
        Toolbar toolbar = findViewById(R.id.toolbars);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initWidget();
        m_spBaudrate.setSelection(4);
        HostLib.getInstance(this).FPCmdProc().GetDeviceList(m_spDevice);
        HostLib.getInstance(this).FPCmdProc().SetListener(new IFPListener.FPCommandListener() {
            @Override
            public void cmdProcReturn(int p_nCmdCode, int p_nRetCode, int p_nParam1, int p_nParam2) { // command process result
                procResponsePacket(p_nCmdCode, p_nRetCode, p_nParam1, p_nParam2);
            }

            @Override
            public void cmdProcReturnData(byte[] p_pData, int p_nSize) { // command process data result

            }

            @Override
            public void cmdProcShowText(String p_szInfo) { // show information
                //m_txtStatus.setText(p_szInfo);
            }
        }, new IFPListener.FPCancelListener() {
            @Override
            public void cancelReturn(int p_nRetCode) { // cancel result

            }
        });
    }

    public void onClick(View view) {
        if (view == m_btnEnroll)
            onEnrollBtn();
        else if (view == m_btnIdentify)
            onIdentifyBtn();
        else if (view == m_btnGetUserCount)
            onGetUserCount();
        else if (view == m_btnDeleteAll)
            onDeleteAllBtn();
    }

    public void initWidget() {
        m_btnEnroll = findViewById(R.id.btnEnroll);
        m_btnIdentify = findViewById(R.id.btnIdentify);
        m_btnGetUserCount = findViewById(R.id.btnGetEnrollCount);
        m_btnDeleteAll = findViewById(R.id.btnRemoveAll);
        m_editUserID = findViewById(R.id.editUserID);
        m_spBaudrate = findViewById(R.id.spnBaudrate);
        m_spDevice = findViewById(R.id.spnDevice);

        m_btnEnroll.setOnClickListener(this);
        m_btnIdentify.setOnClickListener(this);
        m_btnGetUserCount.setOnClickListener(this);
        m_btnDeleteAll.setOnClickListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (popupWindow != null && popupWindow.isShowing()) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    public void onEnrollBtn() {
        popupWindow = new PopupWindow(this);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflate = inflater.inflate(R.layout.pop_fingerprint, null);
        m_txtStatus = inflate.findViewById(R.id.txtStatus);
        popupWindow.setContentView(inflate);
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(inflate, Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
        w_nTemplateNo = PreferenceUtils.INSTANCE.findPreference("TEMPLATE_NO", 1);//GetInputTemplateNo();
        /*if (w_nTemplateNo < 0) return;
        Log.e("TAG", "onEnrollBtn: "+w_nTemplateNo);*/
        HostLib.getInstance(this).FPCmdProc().Run_CmdEnroll(w_nTemplateNo, m_bForce);
        Toast toast = Toast.makeText(FingerprintActivity.this, "确保手指清洁干燥,录制中请勿切换手指", Toast.LENGTH_SHORT);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView tv = (TextView) layout.getChildAt(0);
        tv.setTextSize(30);
        tv.setTextColor(Color.WHITE);
        toast.getView().setBackgroundColor(Color.DKGRAY);
        toast.show();
    }

    public void onIdentifyBtn() {
        popupWindow = new PopupWindow(this);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflate = inflater.inflate(R.layout.pop_fingerprint, null);
        m_txtStatus = inflate.findViewById(R.id.txtStatus);
        popupWindow.setContentView(inflate);
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(inflate, Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
        HostLib.getInstance(this).FPCmdProc().Run_CmdIdentify(m_bForce);
    }

    public void onGetUserCount() {
        HostLib.getInstance(this).FPCmdProc().Run_CmdGetUserCount(m_bForce);
    }

    public void onDeleteAllBtn() {
        HostLib.getInstance(this).FPCmdProc().Run_CmdDeleteAll(m_bForce);
        PreferenceUtils.INSTANCE.putPreference("TEMPLATE_NO", 1);
        PreferenceUtils.INSTANCE.clear();
    }

    @SuppressLint("DefaultLocale")
    private void procResponsePacket(int p_nCode, int p_nRet, int p_nParam1, int p_nParam2) {

        m_strPost = "";
        if (m_txtStatus != null) {
            m_txtStatus.setText(m_strPost);
        }
        switch (p_nCode) {
            case (short) DevComm.CMD_ENROLL_CODE:
            case (short) DevComm.CMD_ENROLL_ONETIME_CODE:
            case (short) DevComm.CMD_CHANGE_TEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    switch (p_nParam1) {
                        case (short) DevComm.NEED_RELEASE_FINGER:
                            m_strPost = "收起手指";
                            break;
                        case (short) DevComm.NEED_FIRST_SWEEP:
                            m_strPost = "放置手指";
                            break;
                        case (short) DevComm.NEED_SECOND_SWEEP:
                            m_strPost = "再来二次";
                            break;
                        case (short) DevComm.NEED_THIRD_SWEEP:
                            m_strPost = "再来一次";
                            break;
                        default:
                            int lastNo = PreferenceUtils.INSTANCE.findPreference("TEMPLATE_NO", 1) + 1;
                            PreferenceUtils.INSTANCE.putPreference("TEMPLATE_NO", lastNo);
                            Log.e("TAG", "procResponsePacket: "+lastNo);
                            Toast toast = Toast.makeText(this, "录制成功", Toast.LENGTH_SHORT);
                            LinearLayout layout = (LinearLayout) toast.getView();
                            TextView tv = (TextView) layout.getChildAt(0);
                            tv.setTextSize(30);
                            tv.setTextColor(Color.WHITE);
                            toast.getView().setBackgroundColor(Color.DKGRAY);
                            toast.show();
                            popupWindow.setOutsideTouchable(false);
                            popupWindow.dismiss();
                            break;
                    }
                } else {
                    if (HostLib.getInstance(this).FPDevComm().LOBYTE((short) p_nParam1) == DevComm.ERR_BAD_QUALITY) {
                        m_strPost = "继续录制";
                    } else if (p_nParam1 == DevComm.ERR_DUPLICATION_ID) {
                        popupWindow.setOutsideTouchable(false);
                        popupWindow.dismiss();
                        Toast toast = Toast.makeText(this, "指纹重复", Toast.LENGTH_SHORT);
                        LinearLayout layout = (LinearLayout) toast.getView();
                        TextView tv = (TextView) layout.getChildAt(0);
                        tv.setTextSize(30);
                        tv.setTextColor(Color.WHITE);
                        toast.getView().setBackgroundColor(Color.DKGRAY);
                        toast.show();
                        popupWindow.dismiss();
                    } else {
                        Toast toast = Toast.makeText(this, "录制失败,请确保手指清洁干燥", Toast.LENGTH_SHORT);
                        LinearLayout layout = (LinearLayout) toast.getView();
                        TextView tv = (TextView) layout.getChildAt(0);
                        tv.setTextSize(30);
                        tv.setTextColor(Color.WHITE);
                        toast.getView().setBackgroundColor(Color.DKGRAY);
                        toast.show();
                        popupWindow.dismiss();
                    }
                }
                break;
            case (short) DevComm.CMD_IDENTIFY_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    switch (p_nParam1) {
                        case (short) DevComm.NEED_RELEASE_FINGER:
                            m_strPost = "收起手指";
                            break;
                        case (short) DevComm.NEED_FIRST_SWEEP:
                            m_strPost = "放置手指";
                            break;
                        default:
                            Toast toast = Toast.makeText(this, "验证成功", Toast.LENGTH_SHORT);
                            LinearLayout layout = (LinearLayout) toast.getView();
                            TextView tv = (TextView) layout.getChildAt(0);
                            tv.setTextSize(30);
                            tv.setTextColor(Color.WHITE);
                            toast.getView().setBackgroundColor(Color.DKGRAY);
                            toast.show();
                            if (popupWindow != null && popupWindow.isShowing()) {
                                popupWindow.dismiss();
                            }
                    }
                } else {
                    Toast toast = Toast.makeText(this, "验证失败", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                }
                break;
            case (short) DevComm.CMD_GET_ENROLL_COUNT_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    Toast toast = Toast.makeText(this, String.format("当前指纹数为 : %d", p_nParam1), Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(this, "获取指纹数失败", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                }
                break;
            case (short) DevComm.CMD_CLEAR_ALLTEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    Toast toast = Toast.makeText(this, String.format("清除指纹数为 : %d", p_nParam1), Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(this, "清除失败", Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setTextSize(30);
                    tv.setTextColor(Color.WHITE);
                    toast.getView().setBackgroundColor(Color.DKGRAY);
                    toast.show();
                }
                break;
        }
        if (m_txtStatus != null) {
            m_txtStatus.setText(m_strPost);
        }
    }
}