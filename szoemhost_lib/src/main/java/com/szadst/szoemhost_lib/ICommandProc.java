package com.szadst.szoemhost_lib;

import android.widget.Spinner;

public abstract  interface ICommandProc {

    public abstract int OpenDevice(String p_szDevice, int p_nBaudrate);
    public abstract int CloseDevice();
    public abstract int GetDeviceList(Spinner p_spDevice);
    public abstract int SetListener(IFPListener.FPCommandListener p_iCmdRet, IFPListener.FPCancelListener p_iCancelRet);
    public abstract int Run_CmdCancel();

    public abstract int Run_CmdEnroll(final int p_nTmpNo, final boolean p_bForce);
    public abstract int Run_CmdVerify(final int p_nTmpNo, final boolean p_bForce);
    public abstract int Run_CmdIdentify(final boolean p_bForce);
    public abstract int Run_CmdIdentifyFree(final boolean p_bForce);
    public abstract int Run_CmdGetEmptyID(final boolean p_bForce);
    public abstract int Run_CmdGetUserCount(final boolean p_bForcet);
    public abstract int Run_CmdDeleteID(final int p_nTmpNo, final boolean p_bForce);
    public abstract int Run_CmdDeleteAll(final boolean p_bForce);
    public abstract int Run_CmdReadTemplate(final int p_nTmpNo, final boolean p_bForce);
    public abstract int Run_CmdWriteTemplate(final int p_nTmpNo, final byte[] p_pTemplate, final int p_nSize, final boolean p_bForce);
    public abstract int Run_CmdUpImage(final boolean p_bForce);
    public abstract int Run_CmdGetFwVersion(final boolean p_bForce);
    public abstract int Run_CmdSetDevPass(final String p_szPassword, final boolean p_bForce);
    public abstract int Run_CmdVerifyPass(final String p_szPassword, final boolean p_bForce);
    public abstract int Run_CmdGetFeatureOfCapturedFP(final boolean p_bForce);
    public abstract int Run_CmdVerifyWithImage(final byte[] p_pImgBuf, final int p_nImgSize, final int p_nTmpNo, final boolean p_bForce);
    public abstract int Run_CmdIdentifyWithImage(final byte[] p_pImgBuf, final int p_nImgSize, final boolean p_bForce);

}
