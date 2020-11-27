package com.qytech.securitycheck;

import java.util.concurrent.TimeUnit;

public  class DelayExample {
    public static void main() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
