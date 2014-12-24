package com.sinovoice.open.voicewake;

import java.io.File;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.open.sys.AccountInfo;
import com.sinovoice.open.sys.HciCloudSysHelper;

/**
 * Asr¼����
 * 
 * @author sinovoice
 */
public class HciCloudAsrRecorderActivity extends Activity {
	private static final String TAG = HciCloudAsrRecorderActivity.class.getSimpleName();

	
	/**
	 * HciCloud�����࣬���������ϵͳ��ʼ�����ͷŲ�����	 
	 */
	private HciCloudSysHelper mHciCloudSysHelper;
	
	/**
	 * AsrRecorder�����࣬ �����TTS�����ĳ�ʼ������ʼ�ϳɣ��ͷŲ����� 
	 */
	private HciCloudAsrRecorderHelper mHciCloudAsrRecorderHelper;
	
	/**
	 * �������˻���Ϣ
	 */
	private AccountInfo mAccountInfo;
	
	/**
	 * �洢·��
	 */
	public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	public static final String FOLDER_SEP = File.separator;
	public static final String COMPANY_FOLDER = "HciCloud";
	public static final String APP_FOLDER = "HciCloudVoiceWake";
	public static final String LOG_FOLDER = "log";
	public static final String ASR_FOLDER = "asr";

	/**
	 * ��ʾ���
	 */
	private TextView mResult; //��ʾ���
	private TextView mState; //��ʾ״̬
	private TextView mError; //��ʾ����
	private TextView mThresholdTextView; //��ʾ��ֵ
	
	/**
	 * ������ť
	 */
	private Button mBtnRecogRealTimeMode;
	
	/**
	 * ��ֵ����
	 */
	private SeekBar mSeekBar;
	
	/**
	 * ��ֵ��С
	 */
	private int mShreshold;
	
	/**
	 * ����¼�����
	 */
	private boolean mIsRecording;
	
	/**
	 * UI��Ϣ������
	 */
	private static Handler mUIHandler = null;
	
	private static class WeakRefHandler extends Handler {
		private WeakReference<HciCloudAsrRecorderActivity> ref = null;

		public WeakRefHandler(HciCloudAsrRecorderActivity activity) {
			ref = new WeakReference<HciCloudAsrRecorderActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			if (ref.get() != null) {
				switch (msg.what) {
				case HciCloudAsrRecorderHelper.RECORD_STATE:
					if (!msg.obj.toString().equalsIgnoreCase(""))
						ref.get().mState.setText(msg.obj.toString());
					break;
				case HciCloudAsrRecorderHelper.RECORD_RESULT:
					if (!msg.obj.toString().equalsIgnoreCase(""))
						ref.get().mResult.setText(msg.obj.toString());
					break;
				case HciCloudAsrRecorderHelper.RECORD_ERROR:
					if (!msg.obj.toString().equalsIgnoreCase(""))
						ref.get().mError.setText(msg.obj.toString());
					break;
				default:
					break;
				}
			}
		}
	}	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
	    mAccountInfo = AccountInfo.getInstance();
        boolean loadResult = mAccountInfo.loadAccountInfo(this);
        if (loadResult) {
            // ������Ϣ�ɹ�����������
        	Log.i(TAG, "���������˺ųɹ�");
        } else {
            // ������Ϣʧ�ܣ���ʾʧ�ܽ���
        	Log.e(TAG, "���������˺�ʧ�ܣ�����assets/AccountInfo.txt�ļ�����д��ȷ�������˻���Ϣ���˻���Ҫ��www.hcicloud.com������������ע�����롣");
        	
            return;
        }
		
		mHciCloudSysHelper = HciCloudSysHelper.getInstance();
		
		// �˷������߳������ģ����ҽ����н�����زŻ��������ִ�С�
        // �˴�ֻ����ʾ�ϳ������÷���û�жԺ�ʱ�������д�����Ҫ�����߷����̨�߳̽��г�ʼ������
        // �������ȵ���HciCloudSys�ĳ�ʼ������
        int sysInitResult = mHciCloudSysHelper.init(this);
        if (sysInitResult != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "hci init error, error code = " + sysInitResult);
            
            return;
        }
		
        //��ȡ¼����������ʵ��
		mHciCloudAsrRecorderHelper = HciCloudAsrRecorderHelper.getInstance();
		
		//����UI��Ϣ��������¼����������ʵ����
		mUIHandler = new WeakRefHandler(this);
		mHciCloudAsrRecorderHelper.setUIHandler(mUIHandler);
		
        //��ʼ���ؼ�
      	initView();
	}
	
	/**
	 * ��ʼ���ؼ�
	 */
	private void initView() {
		mResult = (TextView) findViewById(R.id.resultview);
		mState = (TextView) findViewById(R.id.stateview);
		mError = (TextView) findViewById(R.id.errorview);
		mBtnRecogRealTimeMode = (Button) findViewById(R.id.begin_recog_real_time_mode);
		mBtnRecogRealTimeMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Button btnRecog = (Button)v;
				if (!mIsRecording){
					//��ʼ��������
					initRecorder();
					//��ʼ����
					boolean ok = mHciCloudAsrRecorderHelper.start();
					if (ok) {
						btnRecog.setText("ֹͣ����");
						mIsRecording = true;
					}else {
						Toast.makeText(HciCloudAsrRecorderActivity.this, "¼����δ���ڿ���״̬�����Ե�", Toast.LENGTH_SHORT).show();
					}
				}else {
					mHciCloudAsrRecorderHelper.release();
					btnRecog.setText("��ʼ����");
					mIsRecording = false;
				}
			}
		});
		
		mShreshold = 30;
		mThresholdTextView = (TextView) findViewById(R.id.threshold);
		mThresholdTextView.setText("��ֵ��" + mShreshold);
		
		mSeekBar = (SeekBar) findViewById(R.id.seekBar1);
		mSeekBar.setProgress(mShreshold);
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mShreshold = progress;
				mThresholdTextView.setText("��ֵ��" + mShreshold);
			}
		});		
	}
	
	/**
	 * ��ʼ��������
	 */
	public void initRecorder() {
		int asrInitResult = mHciCloudAsrRecorderHelper.init(this);
        if (asrInitResult != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "asrRecorder init error, error code = " + asrInitResult);
            
            return;
        }
	}
	
	/**
	 * ���ط�ֵ
	 * @return ��ֵ
	 */
	public int getShreshold() {
		return mShreshold;
	}

	@Override
	protected void onDestroy() {		
		//�ͷ�asr
		if (mHciCloudAsrRecorderHelper != null && mIsRecording) {
			mHciCloudAsrRecorderHelper.release();
		}		
		//�ͷ�ϵͳ
		mHciCloudSysHelper.release();
		
		super.onDestroy();
	}
}