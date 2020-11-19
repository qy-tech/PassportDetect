package com.szadst.szoemhost_lib;

public interface IFPListener {

    interface FPCommandListener {
        void cmdProcReturn(int p_nCmdCode, int p_nRetCode, int p_nParam1, int p_nParam2);

        void cmdProcReturnData(byte[] p_pData, int p_nSize);

        void cmdProcShowText(String p_szInfo);
    }

    interface FPCancelListener {
        void cancelReturn(int p_nRetCode);
    }

    interface FPCancelWaitListener {
        void runProcReturn(int p_nRetCode);
    }

}
