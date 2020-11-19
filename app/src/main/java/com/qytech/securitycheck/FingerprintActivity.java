package com.qytech.securitycheck;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.szadst.szoemhost_lib.DevComm;
import com.szadst.szoemhost_lib.HostLib;
import com.szadst.szoemhost_lib.IFPListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FingerprintActivity extends AppCompatActivity implements View.OnClickListener {
    Button m_btnOpenDevice;
    Button m_btnCloseDevice;
    Button m_btnEnroll;
    Button m_btnIdentify;
    Button m_btnVerify;
    Button m_btnGetUserCount;
    Button m_btnDeleteAll;
    EditText m_editUserID;
    TextView m_txtStatus;
    Spinner m_spBaudrate;
    Spinner m_spDevice;
    // member variables
    int m_nBaudrate;
    String m_szDevice;
    int m_nUserID;
    String m_strPost;

    byte[] m_TemplateData;
    int m_nTemplateSize = 0;

    boolean m_bForce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        // set keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // initialize widget
        InitWidget();
        SetInitialState();
        m_spBaudrate.setSelection(4);

        // allocate image buffer
        m_TemplateData = new byte[DevComm.SZ_MAX_RECORD_SIZE];

        // get device list
        HostLib.getInstance(this).FPCmdProc().GetDeviceList(m_spDevice);

        // set listener
        HostLib.getInstance(this).FPCmdProc().SetListener(new IFPListener.FPCommandListener() {
            @Override
            public void cmdProcReturn(int p_nCmdCode, int p_nRetCode, int p_nParam1, int p_nParam2) { // command process result
                ProcResponsePacket(p_nCmdCode, p_nRetCode, p_nParam1, p_nParam2);
            }

            @Override
            public void cmdProcReturnData(byte[] p_pData, int p_nSize) { // command process data result
                int i;
                if (p_nSize > DevComm.SZ_MAX_RECORD_SIZE) {
                } else {
                    System.arraycopy(p_pData, 0, m_TemplateData, 0, p_nSize);
                    m_nTemplateSize = p_nSize;
                }
            }

            @Override
            public void cmdProcShowText(String p_szInfo) { // show information
                m_txtStatus.setText(p_szInfo);
            }
        }, new IFPListener.FPCancelListener() {
            @Override
            public void cancelReturn(int p_nRetCode) { // cancel result

            }
        });

        m_spBaudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    m_nBaudrate = 9600;
                else if (position == 1)
                    m_nBaudrate = 19200;
                else if (position == 2)
                    m_nBaudrate = 38400;
                else if (position == 3)
                    m_nBaudrate = 57600;
                else
                    m_nBaudrate = 115200;
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        m_spDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                m_szDevice = m_spDevice.getItemAtPosition(position).toString();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void onClick(View view) {
        if (view == m_btnOpenDevice)
            OnOpenDeviceBtn();
        else if (view == m_btnCloseDevice)
            OnCloseDeviceBtn();
        else if (view == m_btnEnroll)
            OnEnrollBtn();
        else if (view == m_btnVerify)
            OnVerifyBtn();
        else if (view == m_btnGetUserCount)
            OnGetUserCount();
        else if (view == m_btnDeleteAll)
            OnDeleteAllBtn();
        else if (view == m_btnIdentify)
            OnIdentifyBtn();
    }

    public void InitWidget() {
        m_btnEnroll = findViewById(R.id.btnEnroll);
        m_btnVerify = findViewById(R.id.btnVerify);
        m_btnGetUserCount = findViewById(R.id.btnGetEnrollCount);
        m_btnDeleteAll = findViewById(R.id.btnRemoveAll);
        m_txtStatus = findViewById(R.id.txtStatus);
        m_editUserID = findViewById(R.id.editUserID);
        m_spBaudrate = findViewById(R.id.spnBaudrate);
        m_spDevice = findViewById(R.id.spnDevice);
        m_btnIdentify = findViewById(R.id.btnIdentify);

        m_btnOpenDevice.setOnClickListener(this);
        m_btnCloseDevice.setOnClickListener(this);
        m_btnEnroll.setOnClickListener(this);
        m_btnIdentify.setOnClickListener(this);
        m_btnVerify.setOnClickListener(this);
        m_btnGetUserCount.setOnClickListener(this);
        m_btnDeleteAll.setOnClickListener(this);
    }

    public void EnableCtrl(boolean bEnable) {
        m_btnCloseDevice.setEnabled(bEnable);
        m_btnEnroll.setEnabled(bEnable);
        m_btnVerify.setEnabled(bEnable);
        m_btnIdentify.setEnabled(bEnable);
        m_btnGetUserCount.setEnabled(bEnable);
        m_btnDeleteAll.setEnabled(bEnable);
        m_editUserID.setEnabled(bEnable);
    }

    public void SetInitialState() {
        m_txtStatus.setText("请打开设备");
        EnableCtrl(false);
        m_btnOpenDevice.setEnabled(true);
    }

    public void OnOpenDeviceBtn() {
        int device = HostLib.getInstance(this).FPCmdProc().OpenDevice(m_szDevice, m_nBaudrate);
        if (device == 0) {
            m_btnOpenDevice.setEnabled(false);
            EnableCtrl(true);
        }
    }

    public void OnCloseDeviceBtn() {
        HostLib.getInstance(this).FPCmdProc().CloseDevice();
        SetInitialState();
    }

    public void OnEnrollBtn() {
        int w_nTemplateNo;
        w_nTemplateNo = GetInputTemplateNo();
        if (w_nTemplateNo < 0)
            return;
        EnableCtrl(false);
        int enroll = HostLib.getInstance(this).FPCmdProc().Run_CmdEnroll(w_nTemplateNo, m_bForce);
        if (enroll != 0) {
            EnableCtrl(true);
        }
    }

    public void OnVerifyBtn() {
        int w_nTemplateNo;

        w_nTemplateNo = GetInputTemplateNo();
        if (w_nTemplateNo < 0)
            return;

        EnableCtrl(false);
        if (HostLib.getInstance(this).FPCmdProc().Run_CmdVerify(w_nTemplateNo, m_bForce) != 0) {
            EnableCtrl(true);
        }
    }

    public void OnIdentifyBtn() {
        EnableCtrl(false);
        int identify = HostLib.getInstance(this).FPCmdProc().Run_CmdIdentify(m_bForce);
        if ( identify!= 0) {
            EnableCtrl(true);
        }
    }

    public void OnGetUserCount() {
        HostLib.getInstance(this).FPCmdProc().Run_CmdGetUserCount(m_bForce);
    }

    public void OnDeleteAllBtn() {
        HostLib.getInstance(this).FPCmdProc().Run_CmdDeleteAll(m_bForce);
    }


    private int GetInputTemplateNo() {
        String str;
        str = m_editUserID.getText().toString();
        if (str.isEmpty()) {
            m_txtStatus.setText("请输入用户ID");
            return -1;
        }
        try {
            m_nUserID = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            m_txtStatus.setText(String.format("请输入正确的用户ID(1~%d)", (short) HostLib.getInstance(this).FPDevComm().MAX_RECORD_COUNT));
            return -1;
        }
        return m_nUserID;
    }

    @Override
    public boolean onKeyDown(int KeyCode, KeyEvent event) {
        switch (KeyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0) {
                    if (m_btnCloseDevice.isEnabled())
                        OnCloseDeviceBtn();
                }
                break;
        }

        return super.onKeyDown(KeyCode, event);
    }

    public boolean WriteTemplateFile(int p_nUserID, byte[] pTemplate, int p_nSize) {
        // Save Template to (mnt/sdcard/sz_template)
        // Create Directory
        String w_szSaveDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sz_template";
        File w_fpDir = new File(w_szSaveDirPath);
        if (!w_fpDir.exists())
            w_fpDir.mkdirs();

        // Create Template File
        File w_fpTemplate = new File(w_szSaveDirPath + "/" + String.valueOf(p_nUserID) + ".fpt");
        if (!w_fpTemplate.exists()) {
            try {
                w_fpTemplate.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        FileOutputStream w_foTemplate = null;
        try {
            w_foTemplate = new FileOutputStream(w_fpTemplate);
            w_foTemplate.write(pTemplate, 0, p_nSize);
            w_foTemplate.close();
            m_strPost += "\nSaved file path = " + w_szSaveDirPath + "/" + String.valueOf(p_nUserID) + ".fpt";
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void ProcResponsePacket(int p_nCode, int p_nRet, int p_nParam1, int p_nParam2) {
        m_strPost = "";
        m_txtStatus.setText(m_strPost);

        switch (p_nCode) {
            case (short) DevComm.CMD_ENROLL_CODE:
            case (short) DevComm.CMD_VERIFY_CODE:
            case (short) DevComm.CMD_IDENTIFY_CODE:
            case (short) DevComm.CMD_IDENTIFY_FREE_CODE:
            case (short) DevComm.CMD_ENROLL_ONETIME_CODE:
            case (short) DevComm.CMD_CHANGE_TEMPLATE_CODE:
            case (short) DevComm.CMD_VERIFY_WITH_DOWN_TMPL_CODE:
            case (short) DevComm.CMD_IDENTIFY_WITH_DOWN_TMPL_CODE:
            case (short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE:
            case (short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE:
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
                            m_strPost = String.format("Result : Success\r\nTemplate No : %d", p_nParam1);
                            if (p_nCode != DevComm.CMD_IDENTIFY_FREE_CODE)
                                EnableCtrl(true);
                            break;
                    }
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                    if (HostLib.getInstance(this).FPDevComm().LOBYTE((short) p_nParam1) == DevComm.ERR_BAD_QUALITY) {
                        m_strPost += "\r\nAgain... !";
                    } else {
                        if (p_nParam1 == DevComm.ERR_DUPLICATION_ID) {
                            m_strPost += String.format(" %d.", p_nParam2);
                        } else if (p_nParam1 == DevComm.ERR_ALL_TMPL_EMPTY) {
                            EnableCtrl(true);
                        }
                    }
                    if (p_nCode != DevComm.CMD_IDENTIFY_FREE_CODE)
                        EnableCtrl(true);
                }
                break;

            case (short) DevComm.CMD_GET_EMPTY_ID_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nEmpty ID : %d", p_nParam1);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_GET_ENROLL_COUNT_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nEnroll Count : %d", p_nParam1);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_CLEAR_TEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nTemplate No : %d", p_nParam1);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_CLEAR_ALLTEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nCleared Template Count : %d", p_nParam1);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_READ_TEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nTemplate No : %d", p_nParam1);
                    WriteTemplateFile(p_nParam1, m_TemplateData, m_nTemplateSize);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_WRITE_TEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nTemplate No : %d", p_nParam1);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);

                    if (p_nParam1 == DevComm.ERR_DUPLICATION_ID) {
                        m_strPost += String.format(" %d.", p_nParam2);
                    }
                }
                break;

            case (short) DevComm.CMD_UP_IMAGE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Receive Image Success");
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                EnableCtrl(true);
                break;

            case (short) DevComm.CMD_GET_FW_VERSION_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nFirmware Version: %d.%d", HostLib.getInstance(this).FPDevComm().LOBYTE((short) p_nParam1), HostLib.getInstance(this).FPDevComm().HIBYTE((short) p_nParam1));
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_VERIFY_DEVPASS_CODE:
            case (short) DevComm.CMD_SET_DEVPASS_CODE:
            case (short) DevComm.CMD_EXIT_DEVPASS_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success.");
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success");
                    WriteTemplateFile(0, m_TemplateData, m_nTemplateSize);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                EnableCtrl(true);
                break;

            case (short) DevComm.CMD_FP_CANCEL_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : FP Cancel Success.");
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_GET_BROKEN_TEMPLATE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nBroken Template Count : %d\r\nFirst Broken Template ID : %d", p_nParam1, p_nParam2);
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_ADJUST_SENSOR_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Adjust Success");
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_ENTERSTANDBY_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Enter Standby Mode Success");
                } else {
                    m_strPost = String.format("Result : Enter Standby Mode Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.CMD_FINGER_DETECT_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    if (p_nParam1 == (short) HostLib.getInstance(this).FPDevComm().DETECT_FINGER) {
                        m_strPost = String.format("Finger Detected.");
                    } else if (p_nParam1 == (short) HostLib.getInstance(this).FPDevComm().NO_DETECT_FINGER) {
                        m_strPost = String.format("Finger not Detected.");
                    }
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                EnableCtrl(true);
                break;

            case (short) DevComm.CMD_IDENTIFY_TEMPLATE_WITH_FP_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    if (HostLib.getInstance(this).FPDevComm().LOBYTE((short) p_nParam1) == (short) DevComm.DOWNLOAD_SUCCESS) {
                        m_strPost = String.format("Result : Download Success\r\nInput your finger");
                        m_txtStatus.setText(m_strPost);
                        return;
                    } else {
                        m_strPost = String.format("Result : Identify OK.");
                        m_txtStatus.setText(m_strPost);
                    }
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            case (short) DevComm.RCM_INCORRECT_COMMAND_CODE:
                m_strPost = String.format("Received incorrect command !");
                break;

            case (short) DevComm.CMD_ENTER_ISPMODE_CODE:
                if (p_nRet == (short) DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Success\r\nRunning ISP. Can you programming.");
                } else {
                    m_strPost = String.format("Result : Fail\r\n");
                    m_strPost += GetErrorMsg((short) p_nParam1);
                }
                break;

            default:
                break;
        }

        m_txtStatus.setText(m_strPost);
    }

    private String GetErrorMsg(short p_wErrorCode) {
        String w_ErrMsg;
        switch (p_wErrorCode & 0xFF) {
            case DevComm.ERR_VERIFY:
                w_ErrMsg = "证实指纹";
                break;
            case DevComm.ERR_IDENTIFY:
                w_ErrMsg = "认出指纹";
                break;
            case DevComm.ERR_EMPTY_ID_NOEXIST:
                w_ErrMsg = "空模板不存在";
                break;
            case DevComm.ERR_BROKEN_ID_NOEXIST:
                w_ErrMsg = "模板破损不存在";
                break;
            case DevComm.ERR_TMPL_NOT_EMPTY:
                w_ErrMsg = "此ID的模板已经存在";
                break;
            case DevComm.ERR_TMPL_EMPTY:
                w_ErrMsg = "此模板已经为空";
                break;
            case DevComm.ERR_INVALID_TMPL_NO:
                w_ErrMsg = "无效的模板";
                break;
            case DevComm.ERR_ALL_TMPL_EMPTY:
                w_ErrMsg = "所有模板为空";
                break;
            case DevComm.ERR_INVALID_TMPL_DATA:
                w_ErrMsg = "无效的模板数据";
                break;
            case DevComm.ERR_DUPLICATION_ID:
                w_ErrMsg = "ID重复 : ";
                break;
            case DevComm.ERR_TIME_OUT:
                w_ErrMsg = "超时";
                break;
            case DevComm.ERR_NOT_AUTHORIZED:
                w_ErrMsg = "设备未授权";
                break;
            case DevComm.ERR_EXCEPTION:
                w_ErrMsg = "异常程序错误 ";
                break;
            case DevComm.ERR_MEMORY:
                w_ErrMsg = "内存错误 ";
                break;
            case DevComm.ERR_INVALID_PARAM:
                w_ErrMsg = "无效的参数";
                break;
            case DevComm.ERR_NO_RELEASE:
                w_ErrMsg = "手指松开失败";
                break;
            case DevComm.ERR_INTERNAL:
                w_ErrMsg = "内部错误";
                break;
            case DevComm.ERR_INVALID_OPERATION_MODE:
                w_ErrMsg = "无效的操作模式";
                break;
            case DevComm.ERR_FP_NOT_DETECTED:
                w_ErrMsg = "未检测到手指";
                break;
            case DevComm.ERR_ADJUST_SENSOR:
                w_ErrMsg = "传感器调整失败";
                break;
            default:
                w_ErrMsg = "失败";
                break;

        }
        return w_ErrMsg;
    }
}