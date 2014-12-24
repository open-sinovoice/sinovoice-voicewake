package com.sinovoice.open.sys;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.common.AuthExpireTime;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;

public class HciCloudSysHelper {
    private static final String TAG = HciCloudSysHelper.class.getSimpleName();

    private static HciCloudSysHelper mInstance;

    private HciCloudSysHelper() {
    }

    public static HciCloudSysHelper getInstance() {
        if (mInstance == null) {
            mInstance = new HciCloudSysHelper();
        }
        return mInstance;
    }

    /**
     * HciCloudϵͳ��ʼ��
     * 
     * @return ��ʼ��״̬���ɹ���ʧ��
     */
    public int init(Context context) {
        // ������Ϣ,����InitParam, ������ò������ַ���
        InitParam initParam = getInitParam(context);
        String strConfig = initParam.getStringConfig();
        Log.i(TAG, "strConfig value:" + strConfig);

        // ��ʼ��
        int initResult = HciCloudSys.hciInit(strConfig, context);
        if (initResult != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "hciInit error: " + initResult);

            return initResult;
        } else {
            Log.i(TAG, "hciInit success");
        }

        // ��ȡ��Ȩ/������Ȩ�ļ� :
        initResult = checkAuth();
        if (initResult != HciErrorCode.HCI_ERR_NONE) {
            // ����ϵͳ�Ѿ���ʼ���ɹ�,�ڽ���ǰ��Ҫ���÷���hciRelease()����ϵͳ�ķ���ʼ��
            HciCloudSys.hciRelease();
            return initResult;
        }

        return HciErrorCode.HCI_ERR_NONE;
    }

    /**
     * ϵͳ����ʼ��
     */
    public void release() {
        int nRet = HciCloudSys.hciRelease();
        Log.i(TAG, "HciCloud release, result = " + nRet);
    }

    /**
     * ���س�ʼ����Ϣ
     * 
     * @param context �������ﾳ
     * @return ϵͳ��ʼ������
     */
    private InitParam getInitParam(Context context) {
        String authDirPath = context.getFilesDir().getAbsolutePath();

        // ǰ����������
        InitParam initparam = new InitParam();

        // ��Ȩ�ļ�����·�����������
        initparam.addParam(InitParam.PARAM_KEY_AUTH_PATH, authDirPath);

        // �Ƿ��Զ���������Ȩ,��� ��ȡ��Ȩ/������Ȩ�ļ���ע��
        initparam.addParam(InitParam.PARAM_KEY_AUTO_CLOUD_AUTH, "no");

        // �����Ʒ���Ľӿڵ�ַ���������
        initparam.addParam(InitParam.PARAM_KEY_CLOUD_URL, AccountInfo
                .getInstance().getCloudUrl());

        // ������Key���������ɽ�ͨ�����ṩ
        initparam.addParam(InitParam.PARAM_KEY_DEVELOPER_KEY, AccountInfo
                .getInstance().getDeveloperKey());

        // Ӧ��Key���������ɽ�ͨ�����ṩ
        initparam.addParam(InitParam.PARAM_KEY_APP_KEY, AccountInfo
                .getInstance().getAppKey());

        // ������־����
        String sdcardState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
            String sdPath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
            String packageName = context.getPackageName();

            String logPath = sdPath + File.separator + "sinovoice"
                    + File.separator + packageName + File.separator + "log"
                    + File.separator;

            // ��־�ļ���ַ
            File fileDir = new File(logPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            // ��־��·������ѡ�������������Ϊ����������־
            initparam.addParam(InitParam.PARAM_KEY_LOG_FILE_PATH, logPath);

            // ��־��Ŀ��Ĭ�ϱ������ٸ���־�ļ��������򸲸���ɵ���־
            initparam.addParam(InitParam.PARAM_KEY_LOG_FILE_COUNT, "5");

            // ��־��С��Ĭ��һ����־�ļ�д��󣬵�λΪK
            initparam.addParam(InitParam.PARAM_KEY_LOG_FILE_SIZE, "1024");

            // ��־�ȼ���0=�ޣ�1=����2=���棬3=��Ϣ��4=ϸ�ڣ�5=���ԣ�SDK�����С�ڵ���logLevel����־��Ϣ
            initparam.addParam(InitParam.PARAM_KEY_LOG_LEVEL, "5");
        }

        return initparam;
    }

    /**
     * ��ȡ��Ȩ
     * 
     * @return true �ɹ�
     */
    private int checkAuth() {
        // ��ȡϵͳ��Ȩ����ʱ��
        int initResult;
        AuthExpireTime objExpireTime = new AuthExpireTime();
        initResult = HciCloudSys.hciGetAuthExpireTime(objExpireTime);
        if (initResult == HciErrorCode.HCI_ERR_NONE) {
            // ��ʾ��Ȩ����,���û�����Ҫ��ע��ֵ,�˴�����ɺ���
            Date date = new Date(objExpireTime.getExpireTime() * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.CHINA);
            Log.i(TAG, "expire time: " + sdf.format(date));

            if (objExpireTime.getExpireTime() * 1000 < System
                    .currentTimeMillis()) {
                // ��ȡ��Ȩ����, ����ֵΪ������
                Log.i(TAG, "expired date");

                initResult = HciCloudSys.hciCheckAuth();
                if (initResult == HciErrorCode.HCI_ERR_NONE) {
                    Log.i(TAG, "checkAuth success");
                    return initResult;
                } else {
                    Log.e(TAG, "checkAuth failed: " + initResult);
                    return initResult;
                }
            } else {
                // �Ѿ��ɹ���ȡ����Ȩ,���Ҿ�����Ȩ�����г����ʱ��(>7��)
                Log.i(TAG, "checkAuth success");
                return initResult;
            }
        } else if (initResult == HciErrorCode.HCI_ERR_SYS_AUTHFILE_INVALID) {
            // �����ȡAuth�ļ�ʧ��(�����һ������,��û����Ȩ�ļ�),��ʼ��ȡ��Ȩ
            Log.i(TAG, "authfile invalid");

            initResult = HciCloudSys.hciCheckAuth();
            if (initResult == HciErrorCode.HCI_ERR_NONE) {
                Log.i(TAG, "checkAuth success");
                return initResult;
            } else {
                Log.e(TAG, "checkAuth failed: " + initResult);
                return initResult;
            }
        } else {
            // ����ʧ��ԭ��,�����SDK�����ĵ���"�����ֶ�ֵ"�еĴ�����ĺ������������
            Log.e(TAG, "getAuthExpireTime Error:" + initResult);
            return initResult;
        }
    }

}
