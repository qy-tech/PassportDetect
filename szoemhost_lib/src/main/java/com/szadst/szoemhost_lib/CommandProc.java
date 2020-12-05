package com.szadst.szoemhost_lib;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Spinner;

public class CommandProc implements ICommandProc {

    private static CommandProc m_hCommandProc;
    private static Activity m_hActivity;

    public static short m_dwCode;
    public static boolean m_bThreadWork;
    public static boolean m_bCmdDone = true;
    public static boolean m_bSendResult;

    boolean m_bParamGet;

    byte[] m_TemplateData = new byte[DevComm.SZ_MAX_RECORD_SIZE];
    byte[] m_TemplateData2 = new byte[DevComm.SZ_MAX_RECORD_SIZE];

    int m_nTemplateSize = 0;
    int m_nTemplateSize2 = 0;

    int m_nParam, m_nImgWidth, m_nImgHeight;
    long m_nPassedTime;
    byte[] m_binImage, m_bmpImage;
    int m_nImageBufOffset = 0;
    String m_strPost;

    // listener
    IFPListener.FPCommandListener m_lsCmdProc;
    IFPListener.FPCancelListener m_lsCancel;
    IFPListener.FPCancelWaitListener m_lsCancelWait;

    public CommandProc(Activity parentActivity) {
        this.m_hActivity = parentActivity;

        m_bThreadWork = false;
        m_binImage = new byte[1024 * 100];
        m_bmpImage = new byte[1024 * 100];
    }

    public static CommandProc getInstance(Activity parentActivity) {
        if (m_hCommandProc == null) {
            synchronized (CommandProc.class) {
                if (m_hCommandProc == null) {
                    m_hCommandProc = new CommandProc(parentActivity);
                }
            }
        }
        return m_hCommandProc;
    }

    public int OpenDevice(String p_szDevice, int p_nBaudrate) {
        if (!DevComm.getInstance(m_hActivity).IsInit()) {
            if (!DevComm.getInstance(m_hActivity).OpenComm(p_szDevice, p_nBaudrate)) {
                return 1; // failed init device
            }
        }
        if (DevComm.getInstance(m_hActivity).Run_TestConnection() == (short) DevComm.ERR_SUCCESS) {
            if (DevComm.getInstance(m_hActivity).Run_GetDeviceInfo() == (short) DevComm.ERR_SUCCESS) {
                return 0; // success
            }
        }

        DevComm.getInstance(m_hActivity).CloseComm();
        return 2; // communication error
    }

    public int CloseDevice() {
        DevComm.getInstance(m_hActivity).CloseComm();
        return 0; // success
    }

    public int GetDeviceList(Spinner p_spDevice) {
        DevComm.getInstance(m_hActivity).GetDeviceList(p_spDevice);
        return 0;
    }

    public int SetListener(IFPListener.FPCommandListener p_iCmdRet, IFPListener.FPCancelListener p_iCancelRet) {
        m_lsCmdProc = p_iCmdRet;
        m_lsCancel = p_iCancelRet;

        return 0;
    }

    public int Run_CmdCancel() {
        new Thread(new Runnable() {
            // @Override
            public void run() {
                boolean w_bRet;
                // Init Packet
                DevComm.getInstance(m_hActivity).InitPacket2((short) DevComm.CMD_FP_CANCEL_CODE, true);
                DevComm.getInstance(m_hActivity).SetDataLen2((short) 0x00);
                DevComm.getInstance(m_hActivity).AddCheckSum2(true);

                //. Send Packet
                w_bRet = false;
                if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3)) {
                    w_bRet = DevComm.getInstance(m_hActivity).UART_SendCommand2((short) DevComm.CMD_FP_CANCEL_CODE);
                } else if (DevComm.getInstance(m_hActivity).m_nConnected == 2) {
                    w_bRet = DevComm.getInstance(m_hActivity).USB_SendPacket2((short) DevComm.CMD_FP_CANCEL_CODE);
                }
                if (w_bRet != true) {
                    m_strPost = "Result : Cancel Send Failed\r\n";
                    m_hActivity.runOnUiThread(runShowStatus);
                    m_hActivity.runOnUiThread(runCancelRet);
                    return;
                }

                //. Wait while processing cmd exit
                while (m_bCmdDone == false) {
                    try {
                        Thread.currentThread().sleep(1);//毫秒
                    } catch (Exception e) {
                    }
                }

                if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3)) {
                    w_bRet = DevComm.getInstance(m_hActivity).UART_ReceiveAck2((short) DevComm.CMD_FP_CANCEL_CODE);
                } else if (DevComm.getInstance(m_hActivity).m_nConnected == 2) {
                    w_bRet = DevComm.getInstance(m_hActivity).USB_ReceiveAck2((short) DevComm.CMD_FP_CANCEL_CODE);
                }
                if (w_bRet == true) {
                    m_strPost = "Result : FP Cancel Success.";
                    m_hActivity.runOnUiThread(runCancelRet);
                } else {
                    m_strPost = "Result : Cancel Failed\r\n";
                    m_hActivity.runOnUiThread(runCancelRet);
                }

                m_hActivity.runOnUiThread(runShowStatus);
            }
        }).start();

        return 0;
    }

    public int Run_CmdEnroll(final int p_nTmpNo, final boolean p_bForce) {
        int w_nTemplateNo = 0;
        int w_nIntRet = 0;

        // check inputed template no
        if (CheckInputTemplateNo(p_nTmpNo) == false) {
            return 1;
        }
        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdEnroll(p_nTmpNo, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99;
        } else if (w_nIntRet == 2) {
            return 0;
        }
        w_nTemplateNo = p_nTmpNo;
        m_strPost = "放置你的手指";
        m_lsCmdProc.cmdProcShowText(m_strPost);
        Run_Command_1P((short) DevComm.CMD_ENROLL_CODE, (short) w_nTemplateNo);
        Log.e("TAG", "Run_CmdEnroll: "+w_nIntRet );

        return 0;
    }

    public int Run_CmdVerify(final int p_nTmpNo, final boolean p_bForce) {
        int w_nTemplateNo = 0;
        int w_nIntRet = 0;

        // check inputed template no
        if (CheckInputTemplateNo(p_nTmpNo) == false)
            return 1;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdVerify(p_nTmpNo, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        w_nTemplateNo = p_nTmpNo;
        m_strPost = "放置你的手指";
        m_lsCmdProc.cmdProcShowText(m_strPost);

        Run_Command_1P((short) DevComm.CMD_VERIFY_CODE, (short) w_nTemplateNo);

        return 0;
    }

    public int Run_CmdIdentify(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdIdentify(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        m_strPost = "放置你的手指";
        m_lsCmdProc.cmdProcShowText(m_strPost);

        Run_Command_NP((short) DevComm.CMD_IDENTIFY_CODE);

        return 0;
    }

    public int Run_CmdIdentifyFree(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdIdentifyFree(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        m_strPost = "放置你的手指";
        m_lsCmdProc.cmdProcShowText(m_strPost);

        Run_Command_NP((short) DevComm.CMD_IDENTIFY_FREE_CODE);

        return 0;
    }

    public int Run_CmdGetEmptyID(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdGetEmptyID(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        Run_Command_NP((short) DevComm.CMD_GET_EMPTY_ID_CODE);

        return 0;
    }

    public int Run_CmdGetUserCount(final boolean p_bForce) {
        int w_nIntRet;
        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdGetUserCount(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        Run_Command_NP((short) DevComm.CMD_GET_ENROLL_COUNT_CODE);

        return 0;
    }

    public int Run_CmdDeleteID(final int p_nTmpNo, final boolean p_bForce) {
        int w_nTemplateNo = 0;
        int w_nIntRet;

        // check inputed template no
        if (CheckInputTemplateNo(p_nTmpNo) == false)
            return 1;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdDeleteID(p_nTmpNo, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        w_nTemplateNo = p_nTmpNo;
        Run_Command_1P((short) DevComm.CMD_CLEAR_TEMPLATE_CODE, (short) w_nTemplateNo);

        return 0;
    }

    public int Run_CmdDeleteAll(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdDeleteAll(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        Run_Command_NP((short) DevComm.CMD_CLEAR_ALLTEMPLATE_CODE);

        return 0;
    }

    public int Run_CmdReadTemplate(final int p_nTmpNo, final boolean p_bForce) {
        boolean w_blRet = false;
        int w_nTemplateNo = 0;
        int w_nLen = 0;
        int w_nBufOffset = 0;
        int w_nIntRet;

        // check inputed template no
        if (CheckInputTemplateNo(p_nTmpNo) == false)
            return 1;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdReadTemplate(p_nTmpNo, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        w_nTemplateNo = p_nTmpNo;
        DevComm.getInstance(m_hActivity).memset(m_TemplateData, (byte) 0, DevComm.SZ_MAX_RECORD_SIZE);

        //. Assemble command packet
        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_READ_TEMPLATE_CODE, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0002);
        DevComm.getInstance(m_hActivity).SetCmdData((short) w_nTemplateNo, true);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = DevComm.CMD_READ_TEMPLATE_CODE;
        w_blRet = DevComm.getInstance(m_hActivity).Send_Command((short) DevComm.CMD_READ_TEMPLATE_CODE);
        if (w_blRet == false) {
            CloseDevice();
            return 1;
        }
        if (DevComm.getInstance(m_hActivity).GetRetCode() != (short) DevComm.ERR_SUCCESS) {
            SendResponseRet();
            return 1;
        }

        if (DevComm.getInstance(m_hActivity).GetCmdData(false) == DevComm.SZ_TEMPLATE_SIZE) {
            w_blRet = DevComm.getInstance(m_hActivity).Receive_DataPacket((short) DevComm.CMD_READ_TEMPLATE_CODE);
            w_nLen = DevComm.SZ_TEMPLATE_SIZE;
            System.arraycopy(DevComm.getInstance(m_hActivity).m_abyPacket, 10, m_TemplateData, 0, DevComm.SZ_TEMPLATE_SIZE);
        } else {
            w_nLen = DevComm.getInstance(m_hActivity).GetCmdData(false);
            w_nBufOffset = 0;

            while (true) {
                w_blRet = DevComm.getInstance(m_hActivity).Receive_DataPacket((short) DevComm.CMD_READ_TEMPLATE_CODE);

                if (w_blRet == false) {
                    break;
                } else {
                    if (DevComm.getInstance(m_hActivity).GetRetCode() == DevComm.ERR_SUCCESS) {
                        if (DevComm.getInstance(m_hActivity).GetDataLen() > (DevComm.DATA_SPLIT_UNIT + 4)) {
                            DevComm.getInstance(m_hActivity).SetCmdData((short) DevComm.ERR_FAIL, true);
                            DevComm.getInstance(m_hActivity).SetCmdData((short) DevComm.ERR_INVALID_PARAM, false);
                            w_blRet = false;
                            break;
                        } else {
                            System.arraycopy(DevComm.getInstance(m_hActivity).m_abyPacket, 10, m_TemplateData, w_nBufOffset, DevComm.getInstance(m_hActivity).GetDataLen() - 4);
                            w_nBufOffset = w_nBufOffset + (DevComm.getInstance(m_hActivity).GetDataLen() - 4);
                            if (w_nBufOffset == w_nLen) {
                                break;
                            }
                        }
                    } else {
                        w_blRet = false;
                        break;
                    }
                }
            }
        }

        if (w_blRet == false) {
            return 2;
        } else {
            m_nTemplateSize = w_nLen;
            m_hActivity.runOnUiThread(runSetTemplate);
            SendResponseRet();
        }

        return 0;
    }

    public int Run_CmdWriteTemplate(final int p_nTmpNo, final byte[] p_pTemplate, final int p_nSize, final boolean p_bForce) {
        boolean w_blRet = false;
        int w_nTemplateNo = 0;
        int i, n, r;
        int w_nIntRet;

        // check inputed template no and Read template file
        if (CheckInputTemplateNo(p_nTmpNo) == false) {
            return 1;
        }

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdWriteTemplate(p_nTmpNo, p_pTemplate, p_nSize, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        m_nTemplateSize = p_nSize;
        System.arraycopy(p_pTemplate, 0, m_TemplateData, 0, p_nSize);

        w_nTemplateNo = p_nTmpNo;

        // assemble command packet
        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0002);
        DevComm.getInstance(m_hActivity).SetCmdData((short) m_nTemplateSize, true);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        // send command packet to target
        w_blRet = DevComm.getInstance(m_hActivity).Send_Command((short) DevComm.CMD_WRITE_TEMPLATE_CODE);
        if (w_blRet == false) {
            CloseDevice();
            return 1;
        }

        if (DevComm.getInstance(m_hActivity).GetRetCode() != (short) DevComm.ERR_SUCCESS) {
            SendResponseRet();
            return 1;
        }

        if ((m_nTemplateSize == DevComm.SZ_TEMPLATE_SIZE) || (m_nTemplateSize == DevComm.OLD_USER_TEMPLATE_SIZE)) {
            // assemble data packet
            DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE, false);
            DevComm.getInstance(m_hActivity).SetDataLen((short) (m_nTemplateSize + 2));
            DevComm.getInstance(m_hActivity).SetCmdData((short) w_nTemplateNo, true);
            System.arraycopy(m_TemplateData, 0, DevComm.getInstance(m_hActivity).m_abyPacket, 8, m_nTemplateSize);
            DevComm.getInstance(m_hActivity).AddCheckSum(false);

            // send data packet to target
            w_blRet = DevComm.getInstance(m_hActivity).Send_DataPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE);
            if (w_blRet == false) {
                return 2;
            }
        } else {
            n = m_nTemplateSize / DevComm.DATA_SPLIT_UNIT;
            r = m_nTemplateSize % DevComm.DATA_SPLIT_UNIT;

            for (i = 0; i < n; i++) {
                // assemble data packet
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE, false);
                DevComm.getInstance(m_hActivity).SetDataLen((short) (DevComm.DATA_SPLIT_UNIT + 4));
                DevComm.getInstance(m_hActivity).SetCmdData((short) w_nTemplateNo, true);
                DevComm.getInstance(m_hActivity).SetCmdData((short) DevComm.DATA_SPLIT_UNIT, false);
                System.arraycopy(m_TemplateData, i * DevComm.DATA_SPLIT_UNIT, DevComm.getInstance(m_hActivity).m_abyPacket, 10, DevComm.DATA_SPLIT_UNIT);
                DevComm.getInstance(m_hActivity).AddCheckSum(false);

                // send data packet to target
                w_blRet = DevComm.getInstance(m_hActivity).Send_DataPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE);
                if (w_blRet == false) {
                    return 2;
                }
            }

            if (r > 0) {
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE, false);
                DevComm.getInstance(m_hActivity).SetDataLen((short) (r + 4));
                DevComm.getInstance(m_hActivity).SetCmdData((short) w_nTemplateNo, true);
                DevComm.getInstance(m_hActivity).SetCmdData((short) (r & 0xFFFF), false);
                System.arraycopy(m_TemplateData, i * DevComm.DATA_SPLIT_UNIT, DevComm.getInstance(m_hActivity).m_abyPacket, 10, r);
                DevComm.getInstance(m_hActivity).AddCheckSum(false);

                //. Send data packet to target
                w_blRet = DevComm.getInstance(m_hActivity).Send_DataPacket((short) DevComm.CMD_WRITE_TEMPLATE_CODE);
                if (w_blRet == false) {
                    return 2;
                }
            }
        }

        // display response packet
        SendResponseRet();

        return 0;
    }

    public int Run_CmdUpImage(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdUpImage(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        m_bCmdDone = false;
        m_nImageBufOffset = 0;

        while (m_bThreadWork) {
            try {
                Thread.currentThread().sleep(1);//毫秒
            } catch (Exception e) {
            }
        }

        new Thread(new Runnable() {
            //    		@Override
            public void run() {
                boolean w_blRet = false;

                m_bThreadWork = true;

                m_strPost = "放置你的手指";
                m_hActivity.runOnUiThread(runShowStatus);

                //. Assemble command packet
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_UP_IMAGE_CODE, true);
                DevComm.getInstance(m_hActivity).SetDataLen((short) 0x00);
                DevComm.getInstance(m_hActivity).AddCheckSum(true);

                m_dwCode = (short) DevComm.CMD_UP_IMAGE_CODE;

                w_blRet = DevComm.getInstance(m_hActivity).Send_Command((short) DevComm.CMD_UP_IMAGE_CODE);
                if (w_blRet == false) {
                    m_bSendResult = w_blRet;

                    m_hActivity.runOnUiThread(procRspPacket);
                    m_bThreadWork = false;
                    return; // goto
                }

                if (DevComm.getInstance(m_hActivity).GetRetCode() != DevComm.ERR_SUCCESS) {
                    m_bSendResult = w_blRet;
                    m_hActivity.runOnUiThread(procRspPacket);
                    m_bThreadWork = false;
                    return; // goto
                }

                m_nImgWidth = (short) (((DevComm.getInstance(m_hActivity).m_abyPacket[9] << 8) & 0x0000FF00) | (DevComm.getInstance(m_hActivity).m_abyPacket[8] & 0x000000FF));
                m_nImgHeight = (short) (((DevComm.getInstance(m_hActivity).m_abyPacket[11] << 8) & 0x0000FF00) | (DevComm.getInstance(m_hActivity).m_abyPacket[10] & 0x000000FF));

                if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3)) {
                    while (true) {
                        w_blRet = DevComm.getInstance(m_hActivity).UART_ReceiveDataPacket((short) DevComm.CMD_UP_IMAGE_CODE);

                        if (w_blRet == false) {
                            m_bSendResult = w_blRet;
                            m_hActivity.runOnUiThread(procRspPacket);
                            m_bThreadWork = false;
                            return; // goto
                        } else {
                            if (DevComm.getInstance(m_hActivity).GetRetCode() == DevComm.ERR_SUCCESS) {
                                if (DevComm.getInstance(m_hActivity).GetDataLen() > (DevComm.IMAGE_RECEIVE_UINT + 2)) {
                                    m_bSendResult = w_blRet;
                                    m_hActivity.runOnUiThread(procRspPacket);
                                    m_bThreadWork = false;
                                    return; // goto
                                } else {
                                    if (m_nImageBufOffset == 0) {
                                        m_strPost = "Uploading image...";
                                        m_hActivity.runOnUiThread(runShowStatus);
                                    }

                                    System.arraycopy(DevComm.getInstance(m_hActivity).m_abyPacket, 8, m_binImage, m_nImageBufOffset, DevComm.getInstance(m_hActivity).GetDataLen() - 2);
                                    m_nImageBufOffset = m_nImageBufOffset + (DevComm.getInstance(m_hActivity).GetDataLen() - 2);

                                    if (m_nImageBufOffset == m_nImgWidth * m_nImgHeight) {
                                        m_bSendResult = w_blRet;
                                        m_hActivity.runOnUiThread(procRspPacket);
                                        m_bThreadWork = false;
                                        return; // goto
                                    }
                                }
                            } else {
                                m_bSendResult = w_blRet;
                                m_hActivity.runOnUiThread(procRspPacket);
                                m_bThreadWork = false;
                                return; // goto
                            }
                        }
                    }
                } else {
                    w_blRet = DevComm.getInstance(m_hActivity).USB_ReceiveImage(m_binImage, m_nImgWidth * m_nImgHeight);
                }

                m_bSendResult = w_blRet;

                m_hActivity.runOnUiThread(runSetImage);

                m_hActivity.runOnUiThread(procRspPacket);

                m_bThreadWork = false;
            }
        }).start();

        return 0;
    }

    public int Run_CmdGetFwVersion(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdGetFwVersion(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        Run_Command_NP((short) DevComm.CMD_GET_FW_VERSION_CODE);

        return 0;
    }

    public int Run_CmdSetDevPass(final String p_szPassword, final boolean p_bForce) {
        int i;
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdSetDevPass(p_szPassword, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        if (p_szPassword.length() != 0 && p_szPassword.length() != 14) {
            m_strPost = "Invalid Device Password. \nPlease input valid device password(length=14)!";
            m_lsCmdProc.cmdProcShowText(m_strPost);
            return 1;
        }

        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_SET_DEVPASS_CODE, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x000E); // 14
        if (p_szPassword.length() == 0) {
            for (i = 0; i < 14; i++)
                DevComm.getInstance(m_hActivity).m_abyPacket[6 + i] = 0x00;
        } else {
            for (i = 0; i < 14; i++)
                DevComm.getInstance(m_hActivity).m_abyPacket[6 + i] = (byte) (p_szPassword.charAt(i) & 0xFF);
        }
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = (short) DevComm.CMD_SET_DEVPASS_CODE;
        StartSendThread();

        return 0;
    }

    public int Run_CmdVerifyPass(final String p_szPassword, final boolean p_bForce) {
        int i;
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdVerifyPass(p_szPassword, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        if (p_szPassword.length() != 14) {
            m_strPost = "Invalid Device Password. \nPlease input valid device password(length=14)!";
            m_lsCmdProc.cmdProcShowText(m_strPost);
            return 1;
        }

        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_VERIFY_DEVPASS_CODE, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x000E); // 14
        for (i = 0; i < 14; i++)
            DevComm.getInstance(m_hActivity).m_abyPacket[6 + i] = (byte) (p_szPassword.charAt(i) & 0xFF);
//    	System.arraycopy(m_editDevPassword.toString().toCharArray(), 0, DevComm.getInstance(m_hActivity).m_abyPacket, 6, 14);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = (short) DevComm.CMD_VERIFY_DEVPASS_CODE;
        StartSendThread();

        return 0;
    }

    public int Run_CmdGetFeatureOfCapturedFP(final boolean p_bForce) {
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdGetFeatureOfCapturedFP(p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        m_bCmdDone = false;

        while (m_bThreadWork) {
            try {
                Thread.currentThread().sleep(1);//毫秒
            } catch (Exception e) {
            }
        }

        new Thread(new Runnable() {
            //    		@Override
            public void run() {
                boolean w_blRet = false;
                int w_nLen = 0;
                int w_nBufOffset = 0;

                m_bThreadWork = true;

                DevComm.getInstance(m_hActivity).memset(m_TemplateData, (byte) 0, DevComm.SZ_TEMPLATE_SIZE);

                m_strPost = "放置你的手指";
                m_hActivity.runOnUiThread(runShowStatus);

                //. Assemble command packet
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE, true);
                DevComm.getInstance(m_hActivity).AddCheckSum(true);

                m_dwCode = DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE;
                w_blRet = DevComm.getInstance(m_hActivity).Send_Command((short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE);
                if (w_blRet == false) {
                    m_bSendResult = w_blRet;
                    m_hActivity.runOnUiThread(procRspPacket);
                    m_bThreadWork = false;
                    return; // goto
                }

                if (DevComm.getInstance(m_hActivity).GetRetCode() != DevComm.ERR_SUCCESS) {
                    m_bSendResult = w_blRet;
                    m_hActivity.runOnUiThread(procRspPacket);
                    m_bThreadWork = false;
                    return; // goto
                }

                // Receive template data
                if (DevComm.getInstance(m_hActivity).GetCmdData(false) == DevComm.SZ_TEMPLATE_SIZE) {
                    w_blRet = DevComm.getInstance(m_hActivity).Receive_DataPacket((short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE);
                    w_nLen = DevComm.SZ_TEMPLATE_SIZE;
                    System.arraycopy(DevComm.getInstance(m_hActivity).m_abyPacket, 8, m_TemplateData, 0, DevComm.SZ_TEMPLATE_SIZE);
                } else {
                    w_nLen = DevComm.getInstance(m_hActivity).GetCmdData(false);
                    w_nBufOffset = 0;

                    while (true) {
                        w_blRet = DevComm.getInstance(m_hActivity).Receive_DataPacket((short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE);
                        if (w_blRet == false) {
                            break;
                        } else {
                            if (DevComm.getInstance(m_hActivity).GetRetCode() == DevComm.ERR_SUCCESS) {
                                if (DevComm.getInstance(m_hActivity).GetDataLen() > (DevComm.DATA_SPLIT_UNIT + 2)) {
                                    DevComm.getInstance(m_hActivity).SetCmdData((short) DevComm.ERR_FAIL, true);
                                    DevComm.getInstance(m_hActivity).SetCmdData((short) DevComm.ERR_INVALID_PARAM, false);
                                    w_blRet = false;
                                    break;
                                } else {
                                    System.arraycopy(DevComm.getInstance(m_hActivity).m_abyPacket, 8, m_TemplateData, w_nBufOffset, DevComm.getInstance(m_hActivity).GetDataLen() - 2);
                                    w_nBufOffset = w_nBufOffset + (DevComm.getInstance(m_hActivity).GetDataLen() - 2);
                                    if (w_nBufOffset == w_nLen) {
                                        break;
                                    }
                                }
                            } else {
                                w_blRet = false;
                                break;
                            }
                        }
                    }
                }

                if (w_blRet == false) {
                    m_bSendResult = w_blRet;
                    m_hActivity.runOnUiThread(procRspPacket);
                    m_bThreadWork = false;
                    return; // goto
                } else {
                    m_bSendResult = w_blRet;
                    m_hActivity.runOnUiThread(runSetTemplate);
                    m_hActivity.runOnUiThread(procRspPacket);
                    m_nTemplateSize = w_nLen;
                    m_bThreadWork = false;
                }
            }
        }).start();

        return 0;
    }

    public int Run_CmdVerifyWithImage(final byte[] p_pImgBuf, final int p_nImgSize, final int p_nTmpNo, final boolean p_bForce) {
        int i, r, n, w_nImgSize, w_nTemplateNo;
        boolean w_blRet = false;
        int w_nIntRet;

        // check inputed template no
        if (CheckInputTemplateNo(p_nTmpNo) == false) {
            return 1;
        }

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdVerifyWithImage(p_pImgBuf, p_nImgSize, p_nTmpNo, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        // read image file
        m_nImgWidth = DevComm.SZ_IMG_WIDTH;
        m_nImgHeight = DevComm.SZ_IMG_HEIGHT;
        System.arraycopy(p_pImgBuf, 0, m_binImage, 0, p_nImgSize);

        w_nTemplateNo = p_nTmpNo;

        // assemble command packet
        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0006);
        DevComm.getInstance(m_hActivity).m_abyPacket[6] = DevComm.getInstance(m_hActivity).LOBYTE((short) w_nTemplateNo);
        DevComm.getInstance(m_hActivity).m_abyPacket[7] = DevComm.getInstance(m_hActivity).HIBYTE((short) w_nTemplateNo);
        DevComm.getInstance(m_hActivity).m_abyPacket[8] = DevComm.getInstance(m_hActivity).LOBYTE((short) m_nImgWidth);
        DevComm.getInstance(m_hActivity).m_abyPacket[9] = DevComm.getInstance(m_hActivity).HIBYTE((short) m_nImgWidth);
        DevComm.getInstance(m_hActivity).m_abyPacket[10] = DevComm.getInstance(m_hActivity).LOBYTE((short) m_nImgHeight);
        DevComm.getInstance(m_hActivity).m_abyPacket[11] = DevComm.getInstance(m_hActivity).HIBYTE((short) m_nImgHeight);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = (short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE;
        w_blRet = DevComm.getInstance(m_hActivity).Send_Command((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE);
        if (w_blRet == false) {
            CloseDevice();
            return 1;
        }

        if (DevComm.getInstance(m_hActivity).GetRetCode() != (short) DevComm.ERR_SUCCESS) {
            SendResponseRet();
            return 1;
        }

        if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3)) {
            m_strPost = "Downloading image...";
            m_lsCmdProc.cmdProcShowText(m_strPost);

            w_nImgSize = m_nImgWidth * m_nImgHeight;
            n = w_nImgSize / DevComm.IMAGE_RECEIVE_UINT;
            r = w_nImgSize % DevComm.IMAGE_RECEIVE_UINT;

            for (i = 0; i < n; i++) {
                // assemble data packet
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE, false);
                DevComm.getInstance(m_hActivity).SetDataLen((short) (0x0004 + DevComm.SZ_TEMPLATE_SIZE));
                DevComm.getInstance(m_hActivity).m_abyPacket[6] = DevComm.getInstance(m_hActivity).LOBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[7] = DevComm.getInstance(m_hActivity).HIBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[8] = DevComm.getInstance(m_hActivity).LOBYTE((short) DevComm.IMAGE_RECEIVE_UINT);
                DevComm.getInstance(m_hActivity).m_abyPacket[9] = DevComm.getInstance(m_hActivity).HIBYTE((short) DevComm.IMAGE_RECEIVE_UINT);
                System.arraycopy(m_binImage, i * DevComm.IMAGE_RECEIVE_UINT, DevComm.getInstance(m_hActivity).m_abyPacket, 10, DevComm.IMAGE_RECEIVE_UINT);
                DevComm.getInstance(m_hActivity).AddCheckSum(false);

                w_blRet = DevComm.getInstance(m_hActivity).UART_SendDataPacket((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE);
                if (!w_blRet) {
                    CloseDevice();
                }

                m_strPost = String.format("%d%%...", (i + 1) * DevComm.IMAGE_RECEIVE_UINT * 100 / w_nImgSize);
                m_lsCmdProc.cmdProcShowText(m_strPost);
            }

            if (r > 0) {
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE, false);
                DevComm.getInstance(m_hActivity).SetDataLen((short) (0x0004 + DevComm.SZ_TEMPLATE_SIZE));
                DevComm.getInstance(m_hActivity).m_abyPacket[6] = DevComm.getInstance(m_hActivity).LOBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[7] = DevComm.getInstance(m_hActivity).HIBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[8] = DevComm.getInstance(m_hActivity).LOBYTE((short) r);
                DevComm.getInstance(m_hActivity).m_abyPacket[9] = DevComm.getInstance(m_hActivity).HIBYTE((short) r);
                System.arraycopy(m_binImage, i * DevComm.IMAGE_RECEIVE_UINT, DevComm.getInstance(m_hActivity).m_abyPacket, 10, r);
                DevComm.getInstance(m_hActivity).AddCheckSum(false);

                w_blRet = DevComm.getInstance(m_hActivity).UART_SendDataPacket((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE);
                if (!w_blRet) {
                    CloseDevice();
                }
            }

            m_strPost = "100%...";
            m_lsCmdProc.cmdProcShowText(m_strPost);
        } else {
            w_blRet = DevComm.getInstance(m_hActivity).USB_DownImage(m_binImage, m_nImgWidth * m_nImgHeight);

            if (!w_blRet) {
                CloseDevice();
                return 1;
            }
        }

        // Identify
        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_VERIFY_WITH_IMAGE_CODE, false);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0004);
        DevComm.getInstance(m_hActivity).m_abyPacket[6] = 0;
        DevComm.getInstance(m_hActivity).m_abyPacket[7] = 0;
        DevComm.getInstance(m_hActivity).m_abyPacket[8] = 0;
        DevComm.getInstance(m_hActivity).m_abyPacket[9] = 0;
        DevComm.getInstance(m_hActivity).AddCheckSum(false);

        StartSendThread();
        try {
            Thread.currentThread().sleep(200);//毫秒
        } catch (Exception e) {
        }

        return 0;
    }

    public int Run_CmdIdentifyWithImage(final byte[] p_pImgBuf, final int p_nImgSize, final boolean p_bForce) {
        int i, r, n, w_nImgSize;
        boolean w_blRet = false;
        int w_nIntRet;

        // check force
        w_nIntRet = CheckForce(p_bForce, new IFPListener.FPCancelWaitListener() {
            @Override
            public void runProcReturn(int p_nRetCode) {
                Run_CmdIdentifyWithImage(p_pImgBuf, p_nImgSize, p_bForce);
            }
        });
        if (w_nIntRet == 1) {
            return 99; // busy
        } else if (w_nIntRet == 2) {
            return 0;
        }

        // read image file
        m_nImgWidth = DevComm.SZ_IMG_WIDTH;
        m_nImgHeight = DevComm.SZ_IMG_HEIGHT;
        System.arraycopy(p_pImgBuf, 0, m_binImage, 0, p_nImgSize);

        // assemble command packet
        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0004);
        DevComm.getInstance(m_hActivity).SetCmdData((short) m_nImgWidth, true);
        DevComm.getInstance(m_hActivity).SetCmdData((short) m_nImgHeight, false);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = (short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE;
        w_blRet = DevComm.getInstance(m_hActivity).Send_Command((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE);
        if (w_blRet == false) {
            CloseDevice();
            return 1;
        }

        if (DevComm.getInstance(m_hActivity).GetRetCode() != (short) DevComm.ERR_SUCCESS) {
            SendResponseRet();
            return 1;
        }

        if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3)) {
            m_strPost = "Downloading image...";
            m_lsCmdProc.cmdProcShowText(m_strPost);

            w_nImgSize = m_nImgWidth * m_nImgHeight;
            n = w_nImgSize / DevComm.IMAGE_RECEIVE_UINT;
            r = w_nImgSize % DevComm.IMAGE_RECEIVE_UINT;

            for (i = 0; i < n; i++) {
                // assemble data packet
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE, false);
                DevComm.getInstance(m_hActivity).SetDataLen((short) (0x0004 + DevComm.SZ_TEMPLATE_SIZE));
                DevComm.getInstance(m_hActivity).m_abyPacket[6] = DevComm.getInstance(m_hActivity).LOBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[7] = DevComm.getInstance(m_hActivity).HIBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[8] = DevComm.getInstance(m_hActivity).LOBYTE((short) DevComm.IMAGE_RECEIVE_UINT);
                DevComm.getInstance(m_hActivity).m_abyPacket[9] = DevComm.getInstance(m_hActivity).HIBYTE((short) DevComm.IMAGE_RECEIVE_UINT);
                System.arraycopy(m_binImage, i * DevComm.IMAGE_RECEIVE_UINT, DevComm.getInstance(m_hActivity).m_abyPacket, 10, DevComm.IMAGE_RECEIVE_UINT);
                DevComm.getInstance(m_hActivity).AddCheckSum(false);

                w_blRet = DevComm.getInstance(m_hActivity).UART_SendDataPacket((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE);
                if (!w_blRet) {
                    CloseDevice();
                    return 1;
                }

                m_strPost = String.format("%d%%...", (i + 1) * DevComm.IMAGE_RECEIVE_UINT * 100 / w_nImgSize);
                m_lsCmdProc.cmdProcShowText(m_strPost);
            }

            if (r > 0) {
                DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE, false);
                DevComm.getInstance(m_hActivity).SetDataLen((short) (0x0004 + DevComm.SZ_TEMPLATE_SIZE));
                DevComm.getInstance(m_hActivity).m_abyPacket[6] = DevComm.getInstance(m_hActivity).LOBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[7] = DevComm.getInstance(m_hActivity).HIBYTE((short) i);
                DevComm.getInstance(m_hActivity).m_abyPacket[8] = DevComm.getInstance(m_hActivity).LOBYTE((short) r);
                DevComm.getInstance(m_hActivity).m_abyPacket[9] = DevComm.getInstance(m_hActivity).HIBYTE((short) r);
                System.arraycopy(m_binImage, i * DevComm.IMAGE_RECEIVE_UINT, DevComm.getInstance(m_hActivity).m_abyPacket, 10, r);
                DevComm.getInstance(m_hActivity).AddCheckSum(false);

                w_blRet = DevComm.getInstance(m_hActivity).UART_SendDataPacket((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE);
                if (!w_blRet) {
                    CloseDevice();
                    return 1;
                }
            }

            m_strPost = "100%...";
            m_lsCmdProc.cmdProcShowText(m_strPost);
        } else {
            w_blRet = DevComm.getInstance(m_hActivity).USB_DownImage(m_binImage, m_nImgWidth * m_nImgHeight);
            if (!w_blRet) {
                CloseDevice();
                return 1;
            }
        }

        // Identify
        DevComm.getInstance(m_hActivity).InitPacket((short) DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE, false);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0004);
        DevComm.getInstance(m_hActivity).m_abyPacket[6] = 0;
        DevComm.getInstance(m_hActivity).m_abyPacket[7] = 0;
        DevComm.getInstance(m_hActivity).m_abyPacket[8] = 0;
        DevComm.getInstance(m_hActivity).m_abyPacket[9] = 0;
        DevComm.getInstance(m_hActivity).AddCheckSum(false);

        StartSendThread();
        try {
            Thread.currentThread().sleep(200);//毫秒
        } catch (Exception e) {
        }

        return 0;
    }

    private void Run_Command_NP(short p_wCmd) {
        // assemble command packet
        DevComm.getInstance(m_hActivity).InitPacket(p_wCmd, true);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = p_wCmd;
        StartSendThread();
    }

    private void Run_Command_1P(short p_wCmd, short p_wData) {
        // assemble command packet
        DevComm.getInstance(m_hActivity).InitPacket(p_wCmd, true);
        DevComm.getInstance(m_hActivity).SetDataLen((short) 0x0002);
        DevComm.getInstance(m_hActivity).SetCmdData(p_wData, true);
        DevComm.getInstance(m_hActivity).AddCheckSum(true);

        m_dwCode = p_wCmd;
        StartSendThread();
    }

    private boolean CheckInputTemplateNo(int p_nTmpNo) {
        if (p_nTmpNo > (DevComm.MAX_RECORD_COUNT) || p_nTmpNo < 1) {
            m_lsCmdProc.cmdProcShowText("Please input correct user id(1~" + (short) DevComm.MAX_RECORD_COUNT + ")");
            return false;
        }

        return true;
    }

    public void StartSendThread() {
        m_bCmdDone = false;

        while (m_bThreadWork) {
            try {
                Thread.currentThread().sleep(1);//毫秒
            } catch (Exception e) {
            }
        }

        new Thread(new Runnable() {
            public void run() {
                boolean w_blRet = false;
                short w_wPrefix = 0;

                m_bThreadWork = true;

                w_wPrefix = (short) (((DevComm.getInstance(m_hActivity).m_abyPacket[1] << 8) & 0x0000FF00) | (DevComm.getInstance(m_hActivity).m_abyPacket[0] & 0x000000FF));
                if (w_wPrefix == (short) (DevComm.CMD_PREFIX_CODE)) {
                    if (m_dwCode != (short) (DevComm.CMD_FP_CANCEL_CODE)) {
                        if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3))
                            w_blRet = DevComm.getInstance(m_hActivity).UART_SendCommand(m_dwCode);
                        else if (DevComm.getInstance(m_hActivity).m_nConnected == 2)
                            w_blRet = DevComm.getInstance(m_hActivity).USB_SendPacket(m_dwCode);
                    } else {
                        if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3))
                            w_blRet = DevComm.getInstance(m_hActivity).UART_ReceiveAck(m_dwCode, true);
                        else if (DevComm.getInstance(m_hActivity).m_nConnected == 2)
                            w_blRet = DevComm.getInstance(m_hActivity).USB_ReceiveAck(m_dwCode);
                    }
                } else if (w_wPrefix == (short) (DevComm.CMD_DATA_PREFIX_CODE)) {
                    if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3))
                        w_blRet = DevComm.getInstance(m_hActivity).UART_SendDataPacket(m_dwCode);
                    else if (DevComm.getInstance(m_hActivity).m_nConnected == 2)
                        w_blRet = DevComm.getInstance(m_hActivity).USB_SendDataPacket(m_dwCode);
                } else {
                    if (m_dwCode != (short) (DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE)) {
                        if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3))
                            w_blRet = DevComm.getInstance(m_hActivity).UART_ReceiveAck(m_dwCode, true);
                        else if (DevComm.getInstance(m_hActivity).m_nConnected == 2)
                            w_blRet = DevComm.getInstance(m_hActivity).USB_ReceiveAck(m_dwCode);
                    } else {
                        if ((DevComm.getInstance(m_hActivity).m_nConnected == 1) || (DevComm.getInstance(m_hActivity).m_nConnected == 3))
                            w_blRet = DevComm.getInstance(m_hActivity).UART_ReceiveDataPacket((short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE);
                        else if (DevComm.getInstance(m_hActivity).m_nConnected == 2)
                            w_blRet = DevComm.getInstance(m_hActivity).USB_ReceiveDataPacket((short) DevComm.CMD_FEATURE_OF_CAPTURED_FP_CODE);
                    }
                }
                m_bSendResult = w_blRet;
                m_hActivity.runOnUiThread(procRspPacket);

                m_bThreadWork = false;
            }
        }).start();
    }

    Runnable runSetImage = new Runnable() {
        @Override
        public void run() {
            m_lsCmdProc.cmdProcReturnData(m_binImage, m_nImgWidth * m_nImgHeight);
        }
    };

    Runnable runSetTemplate = new Runnable() {
        @Override
        public void run() {
            m_lsCmdProc.cmdProcReturnData(m_TemplateData, m_nTemplateSize);
        }
    };

    Runnable runShowStatus = new Runnable() {
        public void run() {
            m_lsCmdProc.cmdProcShowText(m_strPost);
        }
    };

    Runnable runCancelRet = new Runnable() {
        @Override
        public void run() {
            m_lsCancel.cancelReturn(0);
            if (m_lsCancelWait != null)
                m_lsCancelWait.runProcReturn(0);
        }
    };

    Runnable procRspPacket = new Runnable() {
        public void run() {
            if (m_bSendResult == false) {
                m_strPost = "接受响应失败! \n请检查与目标的连接";
                m_lsCmdProc.cmdProcShowText(m_strPost);
                m_bCmdDone = true;
                return;
            }
            SendResponseRet();
        }
    };

    private void SendResponseRet() {
        short w_nCmd;
        short w_nRet;
        short w_nData1, w_nData2;

        // Display response packet
        w_nCmd = (short) (((DevComm.getInstance(m_hActivity).m_abyPacket[3] << 8) & 0x0000FF00) | (DevComm.getInstance(m_hActivity).m_abyPacket[2] & 0x000000FF));
        w_nRet = DevComm.getInstance(m_hActivity).MAKEWORD(DevComm.getInstance(m_hActivity).m_abyPacket[6], DevComm.getInstance(m_hActivity).m_abyPacket[7]);
        w_nData1 = DevComm.getInstance(m_hActivity).MAKEWORD(DevComm.getInstance(m_hActivity).m_abyPacket[8], DevComm.getInstance(m_hActivity).m_abyPacket[9]);
        w_nData2 = DevComm.getInstance(m_hActivity).MAKEWORD(DevComm.getInstance(m_hActivity).m_abyPacket[10], DevComm.getInstance(m_hActivity).m_abyPacket[11]);

        m_lsCmdProc.cmdProcReturn(w_nCmd, w_nRet, w_nData1, w_nData2);
        LoopResponse(w_nCmd, w_nRet, w_nData1, w_nData2);
    }

    private void LoopResponse(int p_nCode, int p_nRet, int p_nParam1, int p_nParam2) {
        m_strPost = "";

        if ((p_nCode == (short) DevComm.CMD_IDENTIFY_FREE_CODE)) {
            if (p_nRet == (short) DevComm.ERR_SUCCESS ||
                    DevComm.getInstance(m_hActivity).LOBYTE((short) p_nParam1) != DevComm.ERR_NOT_AUTHORIZED &&
                            DevComm.getInstance(m_hActivity).LOBYTE((short) p_nParam1) != DevComm.ERR_FP_CANCEL &&
                            DevComm.getInstance(m_hActivity).LOBYTE((short) p_nParam1) != DevComm.ERR_INVALID_OPERATION_MODE &&
                            DevComm.getInstance(m_hActivity).LOBYTE((short) p_nParam1) != DevComm.ERR_ALL_TMPL_EMPTY) {
                DevComm.getInstance(m_hActivity).memset(DevComm.getInstance(m_hActivity).m_abyPacket, (byte) 0, 64 * 1024);
                StartSendThread();
                return;
            }
        }
        if ((p_nCode == (short) DevComm.CMD_ENROLL_CODE) ||
                (p_nCode == (short) DevComm.CMD_CHANGE_TEMPLATE_CODE)) {
            switch (p_nParam1) {
                case (short) DevComm.NEED_RELEASE_FINGER:
                case (short) DevComm.NEED_FIRST_SWEEP:
                case (short) DevComm.NEED_SECOND_SWEEP:
                case (short) DevComm.NEED_THIRD_SWEEP:
                case (short) DevComm.ERR_BAD_QUALITY:
                    DevComm.getInstance(m_hActivity).memset(DevComm.getInstance(m_hActivity).m_abyPacket, (byte) 0, 64 * 1024);
                    StartSendThread();
                    return;
                default:
                    break;
            }
        }
        if ((p_nCode == (short) DevComm.CMD_ENROLL_ONETIME_CODE) || (p_nCode == (short) DevComm.CMD_VERIFY_CODE) ||
                (p_nCode == (short) DevComm.CMD_IDENTIFY_CODE) || (p_nCode == (short) DevComm.CMD_IDENTIFY_FREE_CODE)) {
            switch (p_nParam1) {
                case (short) DevComm.NEED_RELEASE_FINGER:
                    DevComm.getInstance(m_hActivity).memset(DevComm.getInstance(m_hActivity).m_abyPacket, (byte) 0, 64 * 1024);
                    StartSendThread();
                    return;
                default:
                    break;
            }
        }

        DevComm.getInstance(m_hActivity).memset(DevComm.getInstance(m_hActivity).m_abyPacket, (byte) 0, 64 * 1024);
        m_bCmdDone = true;
    }

    private int CheckForce(boolean p_bForce, IFPListener.FPCancelWaitListener iCancel) {
        m_lsCancelWait = null;

        // check force
        if (!m_bCmdDone) {
            if (!p_bForce) {
                return 1; // busy
            }
            m_lsCancelWait = iCancel;
            Run_CmdCancel();
            return 2;
        }
        return 0;
    }

}
