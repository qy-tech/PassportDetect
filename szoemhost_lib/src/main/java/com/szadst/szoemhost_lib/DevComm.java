package com.szadst.szoemhost_lib;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;
import android_serialport_api.ComBean;
import android_serialport_api.SerialHelper;
import android_serialport_api.SerialPortFinder;

public class DevComm {

    private static DevComm m_hDevComm;
    private static Activity m_hActivity;

	// Packet Prefix
    public static final int CMD_PREFIX_CODE	= (0xAA55);
    public static final int RCM_PREFIX_CODE = (0x55AA);
    public static final int CMD_DATA_PREFIX_CODE = (0xA55A);
    public static final int RCM_DATA_PREFIX_CODE = (0x5AA5);

    // Command
    public static final int CMD_VERIFY_CODE = (0x0101);
    public static final int CMD_IDENTIFY_CODE = (0x0102);
    public static final int CMD_ENROLL_CODE = (0x0103);
    public static final int CMD_ENROLL_ONETIME_CODE = (0x0104);
    public static final int CMD_CLEAR_TEMPLATE_CODE = (0x0105);
    public static final int CMD_CLEAR_ALLTEMPLATE_CODE = (0x0106);
    public static final int CMD_GET_EMPTY_ID_CODE = (0x0107);
    public static final int CMD_GET_BROKEN_TEMPLATE_CODE = (0x0109);
    public static final int CMD_READ_TEMPLATE_CODE = (0x010A);
    public static final int CMD_WRITE_TEMPLATE_CODE = (0x010B);
    public static final int CMD_GET_FW_VERSION_CODE = (0x0112);
    public static final int CMD_FINGER_DETECT_CODE = (0x0113);
    public static final int CMD_FEATURE_OF_CAPTURED_FP_CODE = (0x011A);
    public static final int CMD_IDENTIFY_TEMPLATE_WITH_FP_CODE = (0x011C);
    public static final int CMD_SLED_CTRL_CODE = (0x0124);
    public static final int CMD_IDENTIFY_FREE_CODE = (0x0125);
    public static final int CMD_SET_DEVPASS_CODE = (0x0126);
    public static final int CMD_VERIFY_DEVPASS_CODE = (0x0127);
    public static final int CMD_GET_ENROLL_COUNT_CODE = (0x0128);
    public static final int CMD_CHANGE_TEMPLATE_CODE = (0x0129);
    public static final int CMD_UP_IMAGE_CODE = (0x012C);
    public static final int CMD_VERIFY_WITH_DOWN_TMPL_CODE = (0x012D);
    public static final int CMD_IDENTIFY_WITH_DOWN_TMPL_CODE = (0x012E);
    public static final int CMD_FP_CANCEL_CODE = (0x0130);
    public static final int CMD_ADJUST_SENSOR_CODE = (0x0137);
    public static final int CMD_IDENTIFY_WITH_IMAGE_CODE = (0x0138);
    public static final int CMD_VERIFY_WITH_IMAGE_CODE = (0x0139);
    public static final int CMD_SET_PARAMETER_CODE = (0x013A);
    public static final int CMD_EXIT_DEVPASS_CODE = (0x013B);
    public static final int CMD_TEST_CONNECTION_CODE = (0x0150);
    public static final int CMD_ENTERSTANDBY_CODE = (0x0155);
    public static final int RCM_INCORRECT_COMMAND_CODE = (0x0160);
    public static final int CMD_ENTER_ISPMODE_CODE = (0x0171);

    // Error Code
    public static final int ERR_SUCCESS = (0);
    public static final int	ERR_FAIL = (1);
    public static final int	ERR_CONTINUE = (2);
    public static final int ERR_COMM_FAIL = (3);
    public static final int	ERR_VERIFY = (0x11);
    public static final int	ERR_IDENTIFY = (0x12);
    public static final int	ERR_TMPL_EMPTY = (0x13);
    public static final int	ERR_TMPL_NOT_EMPTY = (0x14);
    public static final int	ERR_ALL_TMPL_EMPTY = (0x15);
    public static final int	ERR_EMPTY_ID_NOEXIST = (0x16);
    public static final int	ERR_BROKEN_ID_NOEXIST = (0x17);
    public static final int	ERR_INVALID_TMPL_DATA = (0x18);
    public static final int	ERR_DUPLICATION_ID = (0x19);
    public static final int	ERR_TOO_FAST = (0x20);
    public static final int	ERR_BAD_QUALITY = (0x21);
    public static final int	ERR_SMALL_LINES = (0x22);
    public static final int	ERR_TIME_OUT = (0x23);
    public static final int	ERR_NOT_AUTHORIZED = (0x24);
    public static final int	ERR_GENERALIZE = (0x30);
    public static final int	ERR_COM_TIMEOUT = (0x40);
    public static final int	ERR_FP_CANCEL = (0x41);
    public static final int	ERR_INTERNAL = (0x50);
    public static final int	ERR_MEMORY = (0x51);
    public static final int	ERR_EXCEPTION = (0x52);
    public static final int	ERR_INVALID_TMPL_NO = (0x60);
    public static final int	ERR_INVALID_PARAM = (0x70);
    public static final int	ERR_NO_RELEASE = (0x71);
    public static final int	ERR_INVALID_OPERATION_MODE = (0x72);
    public static final int ERR_NOT_SET_PWD = (0x74);
    public static final int	ERR_FP_NOT_DETECTED = (0x75);
    public static final int	ERR_ADJUST_SENSOR = (0x76);

    // Return Value
    public static final int	NEED_FIRST_SWEEP = (0xFFF1);
    public static final int	NEED_SECOND_SWEEP = (0xFFF2);
    public static final int	NEED_THIRD_SWEEP = (0xFFF3);
    public static final int	NEED_RELEASE_FINGER = (0xFFF4);
    public static final int	TEMPLATE_NOT_EMPTY = (0x01);
    public static final int	TEMPLATE_EMPTY = (0x00);
    public static final int	DETECT_FINGER = (0x01);
    public static final int	NO_DETECT_FINGER = (0x00);
    public static final int DOWNLOAD_SUCCESS = (0xA1);

    // Packet
    public static final int MAX_DATA_LEN = (610);
    public static final int CMD_PACKET_LEN = (22);
    public static final int ST_COMMAND_LEN = (66);
    public static final int IMAGE_RECEIVE_UINT = (498);
    public static final int DATA_SPLIT_UNIT = (498);
    public static final int OLD_USER_TEMPLATE_SIZE = (498);
    
    // Template
    public static final int MAX_RECORD_COUNT = (5000);
    public static final int SZ_TEMPLATE_SIZE = (570);
    public static final int SZ_MAX_RECORD_SIZE = (900);

    // Image
    public static final int SZ_IMG_WIDTH = (256);
    public static final int SZ_IMG_HEIGHT = (288);

    public static final int SCSI_TIMEOUT = (3000);
    public static final int SZ_MAX_FP_TIME_OUT = (60);
    public static final int COMM_SLEEP_TIME = (100);
    public static final int ONCE_UP_IMAGE_UINT = (60000);
    public static final int COMM_TIMEOUT = (15000);
    
    public int m_nPacketSize;
    public byte m_bySrcDeviceID = 1, m_byDstDeviceID = 1;
    public byte[] m_abyPacket = new byte[64 * 1024];
    public byte[] m_abyPacket2 = new byte[MAX_DATA_LEN + 10];
    public byte[] m_abyPacketTmp = new byte[64 * 1024];

    private final Context mApplicationContext;
    private static final int VID = 0x2009;
    private static final int PID = 0x7638;
    
    public static boolean m_bSendPacketWork = false;

    // USB
    private UsbController m_usbBase;
    
    // UART ch34xuartdriver
	private static final String UART_ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
	public static CH34xUARTDriver m_uartDriver;
	public byte[] m_pWriteBuffer;
	public byte[] m_pReadBuffer;
	public byte[] m_pUARTReadBuf;
	public int m_nUARTReadLen;
	public boolean m_bBufferHandle = false;
	
	// Serial Port
	SerialPortFinder mSerialPortFinder;
	DispQueueThread DispQueue;
	SerialControl m_SerialPort;
	
	// Connection
	public byte m_nConnected;	// 0 : Not Connected, 1 : UART, 2 : USB, 3 : ttyUART

    public DevComm(Activity parentActivity){

        this.m_hActivity = parentActivity;
        mApplicationContext = parentActivity.getApplicationContext();
        
        // USB Init
        m_usbBase = new UsbController(parentActivity, m_IConnectionHandler, VID, PID);
        
        // UART Driver Init
    	m_uartDriver = new CH34xUARTDriver(
 				(UsbManager) mApplicationContext.getSystemService(Context.USB_SERVICE), m_hActivity,
 				UART_ACTION_USB_PERMISSION);
 		
 		// Buffer Init
 		m_nConnected = 0;
 		m_nUARTReadLen = 0;
 		m_pWriteBuffer = new byte[DevComm.MAX_DATA_LEN];
		m_pReadBuffer = new byte[DevComm.MAX_DATA_LEN];
		m_pUARTReadBuf = new byte[DevComm.MAX_DATA_LEN];
		
		DispQueue = new DispQueueThread();
        DispQueue.start();
        m_SerialPort = new SerialControl();
		
		mSerialPortFinder= new SerialPortFinder();
    }

    public static DevComm getInstance(Activity parentActivity) {
        if (m_hDevComm == null) {
            synchronized (DevComm.class) {
                if (m_hDevComm == null) {
                    m_hDevComm = new DevComm(parentActivity);
                }
            }
        }
        return m_hDevComm;
    }

    public int GetDeviceList(Spinner p_spDevice) {
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        List<String> allDevices = new ArrayList<>();
        allDevices.add("/dev/ttyS4");
        /*for (int i = 0; i < entryValues.length; i++) {
            allDevices.add(entryValues[i]);
        }*/
        ArrayAdapter<String> aspnDevices = new ArrayAdapter<String>(m_hActivity, android.R.layout.simple_spinner_item, allDevices);
        p_spDevice.setAdapter(aspnDevices);
        return 0;
    }

    public boolean IsInit(){
    	if (m_nConnected == 0)
    		return false;
    	else if (m_nConnected == 1)
    		return true;
    	else if (m_nConnected == 2)
    		return m_usbBase.IsInit();
    	else
    		return true;
    }

    public boolean  OpenComm(String p_szDevice, int p_nBaudrate){
    	if (m_nConnected != 0)
    		return false;
    	
    	if (p_szDevice == "USB") // USB
    	{
    		if (!m_usbBase.IsInit())
    			m_usbBase.init();
            if (!m_usbBase.IsInit())
                return false;
    		m_nConnected = 2;
    	}
    	else if (p_szDevice == "CH34xUART") // UART
    	{
    		if (!m_uartDriver.ResumeUsbList())
			{
				Toast.makeText(mApplicationContext, "Open UART device failed!", Toast.LENGTH_SHORT).show();
				m_uartDriver.CloseDevice();
				return false;
			}
    		else
    		{
				if (!m_uartDriver.UartInit())
				{
					Toast.makeText(mApplicationContext, "Initialize UART device failed!", Toast.LENGTH_SHORT).show();
					Toast.makeText(mApplicationContext, "Open UART device failed!", Toast.LENGTH_SHORT).show();
					return false;
				}
				
				if (!m_uartDriver.SetConfig(p_nBaudrate, (byte)8, (byte)1, (byte)0, (byte)0))
				{
					Toast.makeText(mApplicationContext, "Configuration UART device failed!", Toast.LENGTH_SHORT).show();
					Toast.makeText(mApplicationContext, "Open UART device failed!", Toast.LENGTH_SHORT).show();
					return false;
				}
				
				Toast.makeText(mApplicationContext, "Open UART device success!", Toast.LENGTH_SHORT).show();
				m_nConnected = 1;
				m_nUARTReadLen = 0;
				new UART_ReadThread().start();
			}
    	}
    	else	// ttyUART
    	{
    		m_SerialPort.setPort(p_szDevice);
    		m_SerialPort.setBaudRate(p_nBaudrate);
    		try
    		{
    			m_SerialPort.open();
    		} catch (SecurityException e) {
    			Toast.makeText(mApplicationContext, "Open ttyUART device failed!", Toast.LENGTH_SHORT).show();
                return false;
    		} catch (IOException e) {
    			Toast.makeText(mApplicationContext, "Open ttyUART device failed!", Toast.LENGTH_SHORT).show();
                return false;
    		} catch (InvalidParameterException e) {
    			Toast.makeText(mApplicationContext, "Open ttyUART device failed!", Toast.LENGTH_SHORT).show();
                return false;
    		}
    		m_nConnected = 3;
    	}

        return true;
    }

    public boolean  CloseComm(){
    	if (m_nConnected == 0) {
    		return false;
    	}
    	else if (m_nConnected == 1)	// UART
    	{
    		m_uartDriver.CloseDevice();
    		m_nConnected = 0;
    	}
    	else if (m_nConnected == 2)	// USB
    	{
    		m_nConnected = 0;
    		m_usbBase.uninit();
    	}
    	else	// ttyUART
    	{
    		m_SerialPort.stopSend();
    		m_SerialPort.close();
    		m_nConnected = 0;
    	}
        return true;
    }

    int	Run_TestConnection()
    {
    	boolean	w_bRet;
    	
    	InitPacket((short)CMD_TEST_CONNECTION_CODE, true);
    	AddCheckSum(true);
    	
    	w_bRet = Send_Command((short)CMD_TEST_CONNECTION_CODE);
    	
    	if(!w_bRet)
    	{
    		return ERR_COMM_FAIL;
    	}
    	
    	if (GetRetCode() != ERR_SUCCESS)
    	{
    		return ERR_FAIL;
    	}
    	
    	return ERR_SUCCESS;
    }

    int Run_GetDeviceInfo()
    {
    	return ERR_SUCCESS;
    }
    
    public  boolean GetDeviceInformation(String[] deviceInfo)
    {
        int[]  w_nRecvLen = new int[1];
        byte[] w_abyPCCmd = new byte[6];
        byte[] w_abyData = new byte[32];

        String  w_strTmp;
        boolean w_bRet;

        Arrays.fill(w_abyPCCmd, (byte) 0);

        w_abyPCCmd[2] = 0x04;

        w_bRet = SendPackage(w_abyPCCmd, w_abyData);

        if (!w_bRet)
        {
            return  false;
        }

        w_bRet = RecvPackage(w_abyData, w_nRecvLen);

        if (!w_bRet)
        {
            return  false;
        }

        w_strTmp = new String(w_abyData);
        deviceInfo[0] = w_strTmp;

        return true;
    }
    
    private  boolean SendPackage(byte[] pPCCmd, byte[] pData)
    {
        int    nDataLen;

        pPCCmd[0] = (byte)0xEF;
        pPCCmd[1] = 0x01;

        nDataLen = (int)((((pPCCmd[5] & 0xFF) << 8) & 0x0000FF00) | (pPCCmd[4] & 0x000000FF));

        return m_usbBase.UsbSCSIWrite(pPCCmd, 6, pData, nDataLen, 5000);
    }

    private  boolean RecvPackage(byte[] pData, int[] pLevRen)
    {
        int    w_nLen;
        byte[] w_abyPCCmd = new byte[6];
        byte[] w_abyRespond = new byte[4];
        boolean w_bRet;

        w_abyPCCmd[0] = (byte)0xEF;
        w_abyPCCmd[1] = 0x02;
        w_abyPCCmd[2] = 0;
        w_abyPCCmd[3] = 0;
        w_abyPCCmd[4] = 0;
        w_abyPCCmd[5] = 0;

        // receive status
        w_bRet = m_usbBase.UsbSCSIRead(w_abyPCCmd, 6, w_abyRespond, 4, 5000);

        if (!w_bRet)
            return false;

        // receive data
        w_nLen = (int)((int)((w_abyRespond[3] << 8) & 0x0000FF00) | (int)(w_abyRespond[2] & 0x000000FF));

        if (w_nLen > 0)
        {
            //w_nTime = SystemClock.elapsedRealtime();

            w_abyPCCmd[1] = 0x03;
            w_bRet = m_usbBase.UsbSCSIRead(w_abyPCCmd, 6, pData, w_nLen, 5000);

            if (!w_bRet)
                return false;

            pLevRen[0] = w_nLen;
        }

        return  true;
    }

    public short GetRetCode()
    {
    	return (short)((int)((m_abyPacket[7] << 8) & 0x0000FF00) | (int)(m_abyPacket[6] & 0x000000FF));
    }

    public short GetDataLen()
    {
    	return (short)(((m_abyPacket[5] << 8) & 0x0000FF00) | (m_abyPacket[4] & 0x000000FF));
    }

    public void SetDataLen(short p_wDataLen)
    {
    	m_abyPacket[4] = (byte)(p_wDataLen & 0xFF);
		m_abyPacket[5] = (byte)(((p_wDataLen & 0xFF00) >> 8) & 0xFF);
    }

    public void SetDataLen2(short p_wDataLen)
    {
    	m_abyPacket2[4] = (byte)(p_wDataLen & 0xFF);
		m_abyPacket2[5] = (byte)(((p_wDataLen & 0xFF00) >> 8) & 0xFF);
    }

    public void SetCmdData(short p_wData, boolean p_bFirst)
    {
    	if (p_bFirst)
    	{
    		m_abyPacket[6] = (byte)(p_wData & 0xFF);
    		m_abyPacket[7] = (byte)(((p_wData & 0xFF00) >> 8) & 0xFF);
    	}
    	else
    	{
    		m_abyPacket[8] = (byte)(p_wData & 0xFF);
    		m_abyPacket[9] = (byte)(((p_wData & 0xFF00) >> 8) & 0xFF);
    	}
    }

    public short GetCmdData(boolean p_bFirst)
    {
    	if (p_bFirst)
    	{
    		return (short)(((m_abyPacket[7] << 8) & 0x0000FF00) | (m_abyPacket[6] & 0x000000FF));
    	}
    	else
    	{
    		return (short)(((m_abyPacket[9] << 8) & 0x0000FF00) | (m_abyPacket[8] & 0x000000FF));
    	}
    }

    private short GetDataPacketLen()
    {
    	return (short)(((m_abyPacket[5] << 8) & 0x0000FF00) | (m_abyPacket[4] & 0x000000FF) + 6);
    }

    void InitPacket(short p_wCmd, boolean p_bCmdData)
    {
    	memset(m_abyPacket, (byte)0, CMD_PACKET_LEN);
    	
    	//g_pPacketBuffer->wPrefix = p_bCmdData?CMD_PREFIX_CODE:CMD_DATA_PREFIX_CODE;
    	if (p_bCmdData)
    	{
    		m_abyPacket[0] = (byte)(CMD_PREFIX_CODE & 0xFF);
    		m_abyPacket[1] = (byte)(((CMD_PREFIX_CODE & 0xFF00) >> 8) & 0xFF);
    	}
    	else
    	{
    		m_abyPacket[0] = (byte)(CMD_DATA_PREFIX_CODE & 0xFF);
    		m_abyPacket[1] = (byte)(((CMD_DATA_PREFIX_CODE & 0xFF00) >> 8) & 0xFF);
    	}
    	
    	//g_pPacketBuffer->wCMD_RCM = p_wCMD;
    	m_abyPacket[2] = (byte)(p_wCmd & 0xFF);
    	m_abyPacket[3] = (byte)(((p_wCmd & 0xFF00) >> 8) & 0xFF);
    }
    
    void InitPacket2(short p_wCmd, boolean p_bCmdData)
    {
    	memset(m_abyPacket2, (byte)0, CMD_PACKET_LEN);
    	
    	//g_pPacketBuffer->wPrefix = p_bCmdData?CMD_PREFIX_CODE:CMD_DATA_PREFIX_CODE;
    	if (p_bCmdData)
    	{
    		m_abyPacket2[0] = (byte)(CMD_PREFIX_CODE & 0xFF);
    		m_abyPacket2[1] = (byte)(((CMD_PREFIX_CODE & 0xFF00) >> 8) & 0xFF);
    	}
    	else
    	{
    		m_abyPacket2[0] = (byte)(CMD_DATA_PREFIX_CODE & 0xFF);
    		m_abyPacket2[1] = (byte)(((CMD_DATA_PREFIX_CODE & 0xFF00) >> 8) & 0xFF);
    	}
    	
    	//g_pPacketBuffer->wCMD_RCM = p_wCMD;
    	m_abyPacket2[2] = (byte)(p_wCmd & 0xFF);
    	m_abyPacket2[3] = (byte)(((p_wCmd & 0xFF00) >> 8) & 0xFF);
    }

    short GetCheckSum(boolean p_bCmdData)
    {
    	short w_wRet = 0;
    	short w_nI = 0;
    	
    	w_wRet = 0;
    	if (p_bCmdData)
    	{
	    	for (w_nI = 0; w_nI < CMD_PACKET_LEN; w_nI ++)
	    		w_wRet += (m_abyPacket[w_nI] & 0xFF);
    	}
    	else
    	{
    		for (w_nI = 0; w_nI < GetDataPacketLen(); w_nI ++)
    			w_wRet += (m_abyPacket[w_nI] & 0xFF);
    	}
    	return w_wRet;
    }

    short AddCheckSum(boolean p_bCmdData)
    {
    	short w_wRet = 0;
    	short w_wLen = 0;
    	int w_nI;
    	
    	if (p_bCmdData)
    		w_wLen = CMD_PACKET_LEN;
    	else
    		w_wLen = GetDataPacketLen();
    	
    	w_wRet = 0;
    	for (w_nI = 0; w_nI < w_wLen; w_nI ++)
    		w_wRet += (m_abyPacket[w_nI] & 0xFF);
    	
    	m_abyPacket[w_wLen] = (byte)(w_wRet & 0xFF);
    	m_abyPacket[w_wLen + 1] = (byte)(((w_wRet & 0xFF00) >> 8) & 0xFF);
    	
    	return w_wRet;
    }
    
    short AddCheckSum2(boolean p_bCmdData)
    {
    	short w_wRet = 0;
    	short w_wLen = 0;
    	int w_nI;
    	
    	if (p_bCmdData)
    		w_wLen = CMD_PACKET_LEN;
    	else
    		w_wLen = GetDataPacketLen();
    	
    	w_wRet = 0;
    	for (w_nI = 0; w_nI < w_wLen; w_nI ++)
    		w_wRet += (m_abyPacket2[w_nI] & 0xFF);
    	
    	m_abyPacket2[w_wLen] = (byte)(w_wRet & 0xFF);
    	m_abyPacket2[w_wLen + 1] = (byte)(((w_wRet & 0xFF00) >> 8) & 0xFF);
    	
    	return w_wRet;
    }

    boolean CheckReceive(short p_wPrefix, short p_wCmd)
    {
    	short	w_wCheckSum;
    	short	w_wTmpPrefix;
    	short	w_wTmpCmd;
    	short	w_wLen;
    	
    	// Check Prefix Code
    	w_wTmpPrefix = (short)(((m_abyPacket[1] << 8) & 0x0000FF00) | (m_abyPacket[0] & 0x000000FF));
    	w_wTmpCmd = (short)(((m_abyPacket[3] << 8) & 0x0000FF00) | (m_abyPacket[2] & 0x000000FF));
    	
    	if ( w_wTmpCmd != CMD_FP_CANCEL_CODE )
    	{
    		if ((p_wPrefix != w_wTmpPrefix) || (p_wCmd != w_wTmpCmd))
    		{
    			return false;
    		}
    	}

    	if (p_wPrefix == RCM_PREFIX_CODE)
    		w_wLen = CMD_PACKET_LEN;
    	else
    		w_wLen = GetDataPacketLen();
    	
    	w_wCheckSum = (short)(((m_abyPacket[w_wLen + 1] << 8) & 0x0000FF00) | (m_abyPacket[w_wLen] & 0x000000FF));
    	
    	if (w_wCheckSum != GetCheckSum(p_wPrefix == RCM_PREFIX_CODE))
    	{
    		return false;
    	}
    	return true;
    }

    public boolean Send_Command(short p_wCmd)
    {
    	if ((m_nConnected == 1) || (m_nConnected == 3))
    		return UART_SendCommand(p_wCmd);
    	else if (m_nConnected == 2)
    		return USB_SendPacket(p_wCmd);
    	else
    		return false;
    }

    public boolean Send_DataPacket(short p_wCmd)
    {
    	if ((m_nConnected == 1) || (m_nConnected == 3))
    		return UART_SendDataPacket(p_wCmd);
    	else if (m_nConnected == 2)
    		return USB_SendDataPacket(p_wCmd);
    	else
    		return false;
    }

    public boolean Receive_DataPacket(short p_wCmd)
    {
    	if ((m_nConnected == 1) || (m_nConnected == 3))
    		return UART_ReceiveDataPacket(p_wCmd);
    	else if (m_nConnected == 2)
    		return USB_ReceiveDataPacket(p_wCmd);
    	else
    		return false;
    }

    public  boolean USB_SendPacket(short wCMD)
    {
    	byte[]	btCDB = new byte[8];
    	boolean	w_bRet;
    	
    	Arrays.fill(btCDB, (byte)0);

    	btCDB[0] = (byte)0xEF; btCDB[1] = 0x11; btCDB[4] = CMD_PACKET_LEN + 2;

    	while (m_bSendPacketWork)
    	{
            try {
                Thread.currentThread().sleep(3);//毫秒
            } catch (Exception e) {
            }
    	}
    	m_bSendPacketWork = true;
    	w_bRet = m_usbBase.UsbSCSIWrite(btCDB, 8, m_abyPacket, (int)(CMD_PACKET_LEN + 2), SCSI_TIMEOUT);
    	m_bSendPacketWork = false;
    	
    	if (!w_bRet)
    		return false;

    	return USB_ReceiveAck( wCMD );
    }

    public  boolean USB_SendPacket2(short wCMD)
    {
    	byte[]	btCDB = new byte[8];
    	boolean	w_bRet;
    	
    	Arrays.fill(btCDB, (byte)0);

    	btCDB[0] = (byte)0xEF; btCDB[1] = 0x11; btCDB[4] = CMD_PACKET_LEN + 2;
    	
    	while (m_bSendPacketWork)
    	{
            try {
                Thread.currentThread().sleep(3);//毫秒
            } catch (Exception e) {
            }
    	}
    	m_bSendPacketWork = true;
    	w_bRet = m_usbBase.UsbSCSIWrite(btCDB, 8, m_abyPacket2, (int)(CMD_PACKET_LEN + 2), SCSI_TIMEOUT);
    	m_bSendPacketWork = false;

    	if (!w_bRet)
    		return false;

    	return true;
    }

    public boolean USB_ReceiveAck(short p_wCmd)
    {
    	int		w_nLen;
    	byte[]	btCDB = new byte[8];
    	byte[]	w_abyWaitPacket = new byte[CMD_PACKET_LEN + 2];
    	int	w_dwTimeOut = SCSI_TIMEOUT;
    	
    	if (p_wCmd == CMD_VERIFY_CODE ||
			p_wCmd == CMD_IDENTIFY_CODE ||
			p_wCmd == CMD_IDENTIFY_FREE_CODE ||
			p_wCmd == CMD_ENROLL_CODE ||
			p_wCmd == CMD_ENROLL_ONETIME_CODE)
    		w_dwTimeOut = (SZ_MAX_FP_TIME_OUT + 1)*(1000);
    	
    	Arrays.fill(btCDB, (byte)0);
    	
    	//w_nReadCount = GetReadWaitTime(p_byCMD);

    	Arrays.fill(w_abyWaitPacket, (byte)0xAF);
    	    	
    	do 
    	{
    		Arrays.fill(m_abyPacket, (byte)0);
    		
    		btCDB[0] = (byte)0xEF; btCDB[1] = (byte)0x12;
    		
    		w_nLen = CMD_PACKET_LEN + 2;

            while (m_bSendPacketWork)
            {
                try {
                    Thread.currentThread().sleep(3);//毫秒
                } catch (Exception e) {
                }
            }
            m_bSendPacketWork = true;
    		
    		if (!m_usbBase.UsbSCSIRead(btCDB, 8, m_abyPacket, w_nLen, w_dwTimeOut))
    			return false;

            m_bSendPacketWork = false;

            try {
                Thread.currentThread().sleep(COMM_SLEEP_TIME);//毫秒
            } catch (Exception e) {
            }
    	} while ( memcmp(m_abyPacket, w_abyWaitPacket, CMD_PACKET_LEN + 2) == true );
    	
    	m_nPacketSize = w_nLen;
    	
    	if (!CheckReceive((short)RCM_PREFIX_CODE, p_wCmd))
    		return false;

    	return true;
    }

    public boolean USB_ReceiveAck2(short p_wCmd)
    {
    	int		w_nLen;
    	byte[]	btCDB = new byte[8];
    	byte[]	w_abyWaitPacket = new byte[CMD_PACKET_LEN + 2];
    	int	w_dwTimeOut = SCSI_TIMEOUT;
    	
    	Arrays.fill(btCDB, (byte)0);
    	Arrays.fill(w_abyWaitPacket, (byte)0xAF);

    	do
    	{
    		Arrays.fill(m_abyPacket2, (byte)0);
    		
    		btCDB[0] = (byte)0xEF; btCDB[1] = (byte)0x12;
    		
    		w_nLen = CMD_PACKET_LEN + 2;

            while (m_bSendPacketWork)
            {
                try {
                    Thread.currentThread().sleep(3);//毫秒
                } catch (Exception e) {
                }
            }
            m_bSendPacketWork = true;
    		
    		if (!m_usbBase.UsbSCSIRead(btCDB, 8, m_abyPacket2, w_nLen, w_dwTimeOut))
    			return false;

            m_bSendPacketWork = false;

            try {
                Thread.currentThread().sleep(COMM_SLEEP_TIME);//毫秒
            } catch (Exception e) {
            }
    	} while ( memcmp(m_abyPacket2, w_abyWaitPacket, CMD_PACKET_LEN + 2) == true );
    	
    	m_nPacketSize = w_nLen;

    	return true;
    }

    boolean USB_ReceiveDataAck(short p_wCmd)
    {
    	byte[]	btCDB = new byte[8];
    	byte[]	w_WaitPacket = new byte[8];
    	int		w_nLen;
    	int	w_dwTimeOut = COMM_TIMEOUT;
    	
    	if (p_wCmd == CMD_VERIFY_CODE ||
			p_wCmd == CMD_IDENTIFY_CODE ||
			p_wCmd == CMD_IDENTIFY_FREE_CODE ||
			p_wCmd == CMD_ENROLL_CODE ||
			p_wCmd == CMD_ENROLL_ONETIME_CODE)
			w_dwTimeOut = (SZ_MAX_FP_TIME_OUT + 1)*(1000);

        memset(btCDB, (byte)0, 8);
    	memset(w_WaitPacket, (byte)0xAF, 8);
    	Arrays.fill(m_abyPacketTmp, (byte)0);
    	
    	do 
    	{
    		btCDB[0] = (byte)0xEF; btCDB[1] = 0x15;
    		w_nLen = 6;
    		
    		if (!m_usbBase.UsbSCSIRead(btCDB, 8, m_abyPacket, w_nLen, w_dwTimeOut))
    		{
    			return false;
    		}

            try {
                Thread.currentThread().sleep(COMM_SLEEP_TIME);//毫秒
            } catch (Exception e) {
            }
    	}while(memcmp(m_abyPacket, w_WaitPacket, 6) == true);

        do
    	{
            w_nLen = GetDataLen() + 2;
    		if (USB_ReceiveRawData(m_abyPacketTmp, w_nLen) == false) {
                return false;
            }
    		System.arraycopy(m_abyPacketTmp, 0, m_abyPacket, 6, w_nLen);
            try {
                Thread.currentThread().sleep(COMM_SLEEP_TIME);//毫秒
            } catch (Exception e) {
            }
    	} while(memcmp(m_abyPacket, w_WaitPacket, 4) == true);

    	if (!CheckReceive((short)RCM_DATA_PREFIX_CODE, p_wCmd)) {
            return false;
        }

    	return true;
    }

    boolean USB_SendDataPacket(short wCMD)
    {
    	byte[]	btCDB = new byte[8];
    	short	w_wLen = (short)(GetDataPacketLen() + 2);
    	
    	memset(btCDB, (byte)0, 8);

    	btCDB[0] = (byte)0xEF; btCDB[1] = 0x13;
    	
    	btCDB[4] = (byte)(w_wLen & 0xFF);
    	btCDB[5] = (byte)(((w_wLen & 0xFF00) >> 8) & 0xFF);
    	
    	if (!m_usbBase.UsbSCSIWrite(btCDB, 8, m_abyPacket, GetDataPacketLen() + 2, SCSI_TIMEOUT ) )
    		return false;
    	
    	return USB_ReceiveDataAck(wCMD);
    }

    public boolean USB_ReceiveDataPacket(short wCMD)
    {
    	return USB_ReceiveDataAck(wCMD);
    }

    boolean USB_ReceiveRawData(byte[] pBuffer, int nDataLen)
    {
        int     w_nDataCnt = nDataLen;
        byte[]  btCDB = new byte[8];

        memset(btCDB, (byte)0, 8);
        btCDB[0] = (byte)0xEF; btCDB[1] = (byte)0x14;
        if (!m_usbBase.UsbSCSIRead(btCDB, 8, pBuffer, w_nDataCnt, SCSI_TIMEOUT)) {
            return false;
        }

        return true;
    }

    public boolean USB_ReceiveImage(byte[] p_pBuffer, int p_dwDataLen )
    {
    	byte[]	btCDB = new byte[8];
    	byte[]	w_WaitPacket = new byte[8];
    	int w_nI;
    	int w_nIndex;
    	int w_nRemainCount;
    	byte[]	w_pTmpImgBuf = new byte[ONCE_UP_IMAGE_UINT];
    	
    	memset( btCDB, (byte)0, 8 );
    	memset( w_WaitPacket, (byte)0xAF, 8 );
    	
    	if (p_dwDataLen == 208*288 || p_dwDataLen == 242*266 || p_dwDataLen == 202*258 || p_dwDataLen == 256*288)
    	{
    		w_nIndex = 0;
    		w_nRemainCount = p_dwDataLen;
    		w_nI = 0;
    		while (w_nRemainCount > ONCE_UP_IMAGE_UINT)
    		{
    			btCDB[0] = (byte)0xEF; btCDB[1] = 0x16; btCDB[2] = (byte)(w_nI & 0xFF);
    			if (!m_usbBase.UsbSCSIRead(btCDB, 8, w_pTmpImgBuf, ONCE_UP_IMAGE_UINT, SCSI_TIMEOUT))
    				return false;
    			System.arraycopy(w_pTmpImgBuf, 0, p_pBuffer, w_nIndex, ONCE_UP_IMAGE_UINT);
    			w_nRemainCount -= ONCE_UP_IMAGE_UINT;
    			w_nIndex += ONCE_UP_IMAGE_UINT;
    			w_nI ++;
    		}
    		btCDB[0] = (byte)0xEF; btCDB[1] = 0x16; btCDB[2] = (byte)(w_nI & 0xFF);
    		if (!m_usbBase.UsbSCSIRead(btCDB, 8, w_pTmpImgBuf, w_nRemainCount, SCSI_TIMEOUT))
    			return false;
    		System.arraycopy(w_pTmpImgBuf, 0, p_pBuffer, w_nIndex, w_nRemainCount);
    	}
    	
    	return true;
    }

    public boolean USB_DownImage(byte[] pBuf, int nBufLen)
    {
    	byte[]	w_pImgBuf = new byte[ONCE_UP_IMAGE_UINT];
    	int		w_nI;
    	int		w_nIndex = 0;
    	int		w_nRemainCount;
    	byte[]	btCDB = new byte[8];

    	w_nIndex = 0;
    	w_nRemainCount = nBufLen;
    	w_nI = 0;
    	memset(btCDB, (byte)0, 8);
    	
    	while (w_nRemainCount > ONCE_UP_IMAGE_UINT)
    	{
    		btCDB[0] = (byte)0xEF; btCDB[1] = 0x17; btCDB[2] = 0; btCDB[3] = (byte)(w_nI & 0xFF);
    		btCDB[4] = LOBYTE((short)(ONCE_UP_IMAGE_UINT & 0x00FF));
    		btCDB[5] = HIBYTE((short)(ONCE_UP_IMAGE_UINT & 0xFF00));
    		
    		System.arraycopy(pBuf, w_nIndex, w_pImgBuf, 0, ONCE_UP_IMAGE_UINT);
    		if (!m_usbBase.UsbSCSIWrite(btCDB, 6, w_pImgBuf,ONCE_UP_IMAGE_UINT, SCSI_TIMEOUT))
    			return false;

    		w_nRemainCount -= ONCE_UP_IMAGE_UINT;
    		w_nIndex += ONCE_UP_IMAGE_UINT;
    		w_nI ++;
    	}

    	btCDB[0] = (byte)0xEF; btCDB[1] = 0x17; btCDB[2] = 0; btCDB[3] = (byte)(w_nI & 0xFF);
		btCDB[4] = LOBYTE((short)(w_nRemainCount & 0x00FF));
		btCDB[5] = HIBYTE((short)(w_nRemainCount & 0xFF00));

	    System.arraycopy(pBuf, w_nIndex, w_pImgBuf, 0, w_nRemainCount);
    	if (!m_usbBase.UsbSCSIWrite(btCDB, 6, w_pImgBuf, w_nRemainCount, SCSI_TIMEOUT))
    		return false;
    	
    	return true;
    }

    public boolean UART_SendCommand(short p_wCmd)
    {
    	int	w_nResult = 0;

    	if (m_nConnected == 1)
    	{
    		w_nResult = m_uartDriver.WriteData(m_abyPacket, CMD_PACKET_LEN + 2);
    		if(w_nResult < 0){
        		return false;
        	}
    	}
    	else if (m_nConnected == 3)
    	{
    		byte[] w_pData = new byte[CMD_PACKET_LEN + 2];
    		System.arraycopy(m_abyPacket, 0, w_pData, 0, CMD_PACKET_LEN + 2);
    		m_SerialPort.send(w_pData);
    	}

    	return UART_ReceiveAck(p_wCmd, true);
    }

    public  boolean UART_SendCommand2(short wCMD)
    {
    	int	w_nResult = 0;

    	if (m_nConnected == 1)
    	{
    		w_nResult = m_uartDriver.WriteData(m_abyPacket2, CMD_PACKET_LEN + 2);
    		if(w_nResult < 0){
        		return false;
        	}
    	}
    	else if (m_nConnected == 3)
    	{
    		byte[] w_pData = new byte[CMD_PACKET_LEN + 2];
    		System.arraycopy(m_abyPacket2, 0, w_pData, 0, CMD_PACKET_LEN + 2);
    		m_SerialPort.send(w_pData);
    	}

    	return true;
    }

    public boolean UART_ReceiveAck(short p_wCmd, boolean p_bCmdData)
    {
//    	int	w_nResult = 0;
    	int w_nReadLen = 0;
    	int w_nTotalLen = CMD_PACKET_LEN + 2;
    	int w_nTmpLen;
    	long w_nTime;
    	int i;

    	w_nTime = System.currentTimeMillis();
    	
    	while (w_nReadLen < w_nTotalLen)
    	{
//	    	w_nResult = m_uartDriver.ReadData(m_abyPacket, CMD_PACKET_LEN + 2);
    		if (System.currentTimeMillis() - w_nTime > 10000)
    		{
    			m_nUARTReadLen = 0;
    			return false;
    		}
    		
    		i = 0;
    		while (m_bBufferHandle)
    		{
    			i++;
    			if (i < 1000)
    				break;

                try {
                    Thread.currentThread().sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
    		}
    		
    		if (m_nUARTReadLen <= 0)
    			continue;
            m_bBufferHandle = true;
    		if (w_nTotalLen - w_nReadLen < m_nUARTReadLen)
    		{
    			w_nTmpLen = w_nTotalLen - w_nReadLen;
    			System.arraycopy(m_pUARTReadBuf, 0, m_abyPacket, w_nReadLen, w_nTmpLen);
	    		w_nReadLen += w_nTmpLen;
	    		m_nUARTReadLen = m_nUARTReadLen - w_nTmpLen;
	    		System.arraycopy(m_pUARTReadBuf, w_nTmpLen, m_abyPacketTmp, 0, m_nUARTReadLen);
	    		System.arraycopy(m_abyPacketTmp, 0, m_pUARTReadBuf, 0, m_nUARTReadLen);
    		}
    		else
    		{
    			System.arraycopy(m_pUARTReadBuf, 0, m_abyPacket, w_nReadLen, m_nUARTReadLen);
	    		w_nReadLen += m_nUARTReadLen;
	    		m_nUARTReadLen = 0;
    		}
    		m_bBufferHandle = false;
    	}
    	
    	if (p_bCmdData)
    		return CheckReceive((short)RCM_PREFIX_CODE, p_wCmd);
    	else
    		return CheckReceive((short)RCM_DATA_PREFIX_CODE, p_wCmd);
    }

    public boolean UART_ReceiveAck2(short p_wCmd)
    {
//    	int	w_nResult = 0;
    	int w_nReadLen = 0;
    	int w_nTotalLen = CMD_PACKET_LEN + 2;
    	int w_nTmpLen;
    	long w_nTime;

    	w_nTime = System.currentTimeMillis();

    	while (w_nReadLen < w_nTotalLen)
    	{
//	    	w_nResult = m_uartDriver.ReadData(m_abyPacket2, CMD_PACKET_LEN + 2);
    		if (System.currentTimeMillis() - w_nTime > 10000)
    		{
    			m_nUARTReadLen = 0;
    			return false;
    		}
    		
    		if (m_nUARTReadLen <= 0)
    			continue;
    		if (w_nTotalLen - w_nReadLen < m_nUARTReadLen)
    		{
    			w_nTmpLen = w_nTotalLen - w_nReadLen;
    			System.arraycopy(m_pUARTReadBuf, 0, m_abyPacket2, w_nReadLen, w_nTmpLen);
	    		w_nReadLen += w_nTmpLen;
	    		m_nUARTReadLen = m_nUARTReadLen - w_nTmpLen;
	    		System.arraycopy(m_pUARTReadBuf, w_nTmpLen, m_abyPacketTmp, 0, m_nUARTReadLen);
	    		System.arraycopy(m_abyPacketTmp, 0, m_pUARTReadBuf, 0, m_nUARTReadLen);
    		}
    		else
    		{
	    		System.arraycopy(m_pUARTReadBuf, 0, m_abyPacket2, w_nReadLen, m_nUARTReadLen);
		    	w_nReadLen += m_nUARTReadLen;
		    	m_nUARTReadLen = 0;
    		}
    	}
    	
    	return true;
    }

    public boolean UART_ReceiveDataAck(short p_wCmd)
    {
    	if (!UART_ReadDataN(m_abyPacket, 0, 6))
    		return false;

    	if (!UART_ReadDataN(m_abyPacket, 6, GetDataLen() + 2))
    		return false;

    	return CheckReceive((short)RCM_DATA_PREFIX_CODE, p_wCmd);
    }

    public boolean UART_SendDataPacket(short p_wCmd)
    {
    	int		w_nSendCnt = 0;

    	if (m_nConnected == 1)
    	{
	    	w_nSendCnt = m_uartDriver.WriteData(m_abyPacket, GetDataLen() + 8);
	    	if(w_nSendCnt < 0)
	    		return false;
    	}
    	else if (m_nConnected == 3)
    	{
    		int w_nLen = GetDataLen() + 8;
    		byte[] w_pData = new byte[w_nLen];
    		System.arraycopy(m_abyPacket, 0, w_pData, 0, w_nLen);
    		m_SerialPort.send(w_pData);
    	}
    	
    	return UART_ReceiveDataAck(p_wCmd);
    }

    public boolean UART_ReceiveDataPacket(short p_wCmd)
    {
    	return UART_ReceiveDataAck(p_wCmd);
    }

    public boolean UART_ReceiveData(short p_wCmd, int p_nDataLen, byte[] p_pBuffer)
    {
    	int		w_nReceivedCnt;
    	int		w_wPacketDataLen = 0;

    	for (w_nReceivedCnt = 0; w_nReceivedCnt < p_nDataLen; w_nReceivedCnt += w_wPacketDataLen)
    	{
    		w_wPacketDataLen = p_nDataLen - w_nReceivedCnt;
    		if (w_wPacketDataLen > MAX_DATA_LEN) w_wPacketDataLen = MAX_DATA_LEN;
    		if (UART_ReceiveDataPacket(p_wCmd) == false)
    			return false;
    		System.arraycopy(m_abyPacket, 8, p_pBuffer, w_nReceivedCnt, GetDataLen() + 4);
    	}
    	return true;
    }

    boolean UART_ReadDataN(byte[] p_pData, int p_nStart, int p_nLen)
    {
    	int		w_nRecvLen, w_nTotalRecvLen;
    	int 	w_nTmpLen;
    	long	w_nTime;
    	
    	w_nRecvLen = p_nLen;
    	w_nTotalRecvLen = 0;
    	w_nTime = System.currentTimeMillis();
    	
    	while(w_nTotalRecvLen < p_nLen )
    	{
    		if (System.currentTimeMillis() - w_nTime > 10000)
    		{
    			m_nUARTReadLen = 0;
    			return false;
    		}
    		
	    	if (m_nUARTReadLen <= 0)
    			continue;
    		
	    	if (p_nLen - w_nTotalRecvLen < m_nUARTReadLen)
    		{
    			w_nTmpLen = p_nLen - w_nTotalRecvLen;
    			System.arraycopy(m_pUARTReadBuf, 0, p_pData, p_nStart + w_nTotalRecvLen, w_nTmpLen);
    			w_nRecvLen = w_nRecvLen - w_nTmpLen;
	    		w_nTotalRecvLen = w_nTotalRecvLen + w_nTmpLen;
	    		m_nUARTReadLen = m_nUARTReadLen - w_nTmpLen;
	    		System.arraycopy(m_pUARTReadBuf, w_nTmpLen, m_abyPacketTmp, 0, m_nUARTReadLen);
	    		System.arraycopy(m_abyPacketTmp, 0, m_pUARTReadBuf, 0, m_nUARTReadLen);
    		}
    		else
    		{
	    		System.arraycopy(m_pUARTReadBuf, 0, p_pData, p_nStart + w_nTotalRecvLen, m_nUARTReadLen);
	    		w_nRecvLen = w_nRecvLen - m_nUARTReadLen;
	    		w_nTotalRecvLen = w_nTotalRecvLen + m_nUARTReadLen;
	    		m_nUARTReadLen = 0;
    		}
    	}
    	
    	return true;
    }

    public class UART_ReadThread extends Thread {

		public void run() {

			while (true) {
				if (m_nConnected != 1)
					break;
				if (m_nUARTReadLen > 0)
					continue;

				m_nUARTReadLen = m_uartDriver.ReadData(m_pUARTReadBuf, DevComm.MAX_DATA_LEN);
			}
			
		}
	}

    public boolean memcmp(byte[] p1, byte[] p2, int nLen)
    {
    	int		i;
    	
    	for (i = 0; i < nLen; i++)
    	{
    		if (p1[i] != p2[i])
    			return false;
    	}
    	
    	return true;
    }
    
    public void memset(byte[] p1, byte nValue, int nLen)
    {
    	Arrays.fill(p1, 0, nLen, nValue);
    }
    
    public void memcpy(byte[] p1, byte[] p2, int nLen)
    {
        int		i;

        for (i = 0; i < nLen; i++)
        {
            p1[i] = p2[i];
        }
    }
    
    public short MAKEWORD(byte low, byte high)
    {
    	short s;
    	s = (short)((((high & 0x00FF) << 8) & 0x0000FF00) | (low & 0x000000FF));
    	return s;
    }
    
    public byte LOBYTE(short s)
    {
    	return (byte)(s & 0xFF); 
    }
    
    public byte HIBYTE(short s)
    {
    	return (byte)(((s & 0xFF00) >> 8) & 0xFF);
    }

    private class SerialControl extends SerialHelper{

		public SerialControl(){
		}

		@Override
		protected void onDataReceived(final ComBean ComRecData)
		{
			DispQueue.AddQueue(ComRecData);
		}
    }

    private class DispQueueThread extends Thread{
		private Queue<ComBean> QueueList = new LinkedList<ComBean>(); 
		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				int i;
		        while(true)
		        {
		        	final ComBean ComData;
		        	if ((ComData=QueueList.poll())==null)
		        		break;

		        	i = 0;
		        	while (m_bBufferHandle)
		        	{
		        		i++;
		        		if (i > 1000)
		        			break;

                        try {
                            Thread.currentThread().sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
		        	}

		        	while (m_nUARTReadLen + ComData.nSize > MAX_DATA_LEN)
                    {
                        try {
                            Thread.currentThread().sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    m_bBufferHandle = true;
		        	System.arraycopy(ComData.bRec, 0, m_pUARTReadBuf, m_nUARTReadLen, ComData.nSize);
		        	m_nUARTReadLen = m_nUARTReadLen + ComData.nSize;
		        	m_bBufferHandle = false;
//		        	break;
				}
			}
		}

		public synchronized void AddQueue(ComBean ComData){
			QueueList.add(ComData);
		}
	}


    private final IUsbConnState m_IConnectionHandler = new IUsbConnState() {
        @Override
        public void onUsbConnected() {
            if (Run_TestConnection() == (short)ERR_SUCCESS)
            {
                if (Run_GetDeviceInfo() == (short)ERR_SUCCESS)
                {
//        			EnableCtrl(true);
//		            m_btnOpenDevice.setEnabled(false);
//		            m_btnCloseDevice.setEnabled(true);
//		            m_txtStatus.setText("Open Device Success!");
                }
            }
            else
            {
//        		m_txtStatus.setText("Can not connect to device!");
            }
        }

        @Override
        public void onUsbPermissionDenied() {
            Toast.makeText(m_hActivity.getApplicationContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDeviceNotFound() {
            Toast.makeText(m_hActivity.getApplicationContext(), "Can not find usb device!", Toast.LENGTH_SHORT).show();
        }
    };
}
