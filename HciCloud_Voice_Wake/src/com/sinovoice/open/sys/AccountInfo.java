package com.sinovoice.open.sys;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import android.content.Context;

public class AccountInfo {

    private static AccountInfo mInstance;

    private Map<String, String> mAccountMap;

    private AccountInfo() {
        mAccountMap = new HashMap<String, String>();
    }

    public static AccountInfo getInstance() {
        if (mInstance == null) {
            mInstance = new AccountInfo();
        }
        return mInstance;
    }

    public String getCapKey(){
        return mAccountMap.get("capKey");
    }
    public String getDeveloperKey(){
        return mAccountMap.get("developerKey");
    }
    public String getAppKey(){
        return mAccountMap.get("appKey");
    }
    public String getCloudUrl(){
        return mAccountMap.get("cloudUrl");
    }
    
    /**
     * �����û���ע����Ϣ
     * @param context ������
     * @return ���سɹ�Ϊtrue��ʧ��Ϊfalse
     */   
    public boolean loadAccountInfo(Context context) {
        boolean isSuccess = true;
        try {       	
        	InputStream ins = context.getResources().getAssets().open("AccountInfo.txt");
        	Properties props = new Properties();
        	props.load(ins);
        	
        	mAccountMap.put("appKey", props.getProperty("appKey"));
        	mAccountMap.put("developerKey", props.getProperty("developerKey"));
        	mAccountMap.put("cloudUrl", props.getProperty("cloudUrl"));
        	mAccountMap.put("capKey", props.getProperty("capKey"));
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        }
        
        return isSuccess;
    }

}
