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
 * Asr录音机
 * 
 * @author sinovoice
 */
public class HciCloudAsrRecorderActivity extends Activity {
	private static final String TAG = HciCloudAsrRecorderActivity.class.getSimpleName();

	
	/**
	 * HciCloud帮助类，可完成灵云系统初始化，释放操作。	 
	 */
	private HciCloudSysHelper mHciCloudSysHelper;
	
	/**
	 * AsrRecorder帮助类， 可完成TTS能力的初始化，开始合成，释放操作。 
	 */
	private HciCloudAsrRecorderHelper mHciCloudAsrRecorderHelper;
	
	/**
	 * 开发者账户信息
	 */
	private AccountInfo mAccountInfo;
	
	/**
	 * 存储路径
	 */
	public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	public static final String FOLDER_SEP = File.separator;
	public static final String COMPANY_FOLDER = "HciCloud";
	public static final String APP_FOLDER = "HciCloudVoiceWake";
	public static final String LOG_FOLDER = "log";
	public static final String ASR_FOLDER = "asr";

	/**
	 * 显示面板
	 */
	private TextView mResult; //显示结果
	private TextView mState; //显示状态
	private TextView mError; //显示错误
	private TextView mThresholdTextView; //显示阀值
	
	/**
	 * 启动按钮
	 */
	private Button mBtnRecogRealTimeMode;
	
	/**
	 * 阀值调节
	 */
	private SeekBar mSeekBar;
	
	/**
	 * 阀值大小
	 */
	private int mShreshold;
	
	/**
	 * 正在录音标记
	 */
	private boolean mIsRecording;
	
	/**
	 * UI消息处理器
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
            // 加载信息成功进入主界面
        	Log.i(TAG, "加载灵云账号成功");
        } else {
            // 加载信息失败，显示失败界面
        	Log.e(TAG, "加载灵云账号失败！请在assets/AccountInfo.txt文件中填写正确的灵云账户信息，账户需要从www.hcicloud.com开发者社区上注册申请。");
        	
            return;
        }
		
		mHciCloudSysHelper = HciCloudSysHelper.getInstance();
		
		// 此方法是线程阻塞的，当且仅当有结果返回才会继续向下执行。
        // 此处只是演示合成能力用法，没有对耗时操作进行处理。需要开发者放入后台线程进行初始化操作
        // 必须首先调用HciCloudSys的初始化方法
        int sysInitResult = mHciCloudSysHelper.init(this);
        if (sysInitResult != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "hci init error, error code = " + sysInitResult);
            
            return;
        }
		
        //获取录音机帮助类实例
		mHciCloudAsrRecorderHelper = HciCloudAsrRecorderHelper.getInstance();
		
		//传递UI消息处理器到录音机帮助类实例中
		mUIHandler = new WeakRefHandler(this);
		mHciCloudAsrRecorderHelper.setUIHandler(mUIHandler);
		
        //初始化控件
      	initView();
	}
	
	/**
	 * 初始化控件
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
					//初始化播放器
					initRecorder();
					//开始播放
					boolean ok = mHciCloudAsrRecorderHelper.start();
					if (ok) {
						btnRecog.setText("停止唤醒");
						mIsRecording = true;
					}else {
						Toast.makeText(HciCloudAsrRecorderActivity.this, "录音机未处于空闲状态，请稍等", Toast.LENGTH_SHORT).show();
					}
				}else {
					mHciCloudAsrRecorderHelper.release();
					btnRecog.setText("开始唤醒");
					mIsRecording = false;
				}
			}
		});
		
		mShreshold = 30;
		mThresholdTextView = (TextView) findViewById(R.id.threshold);
		mThresholdTextView.setText("阀值：" + mShreshold);
		
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
				mThresholdTextView.setText("阀值：" + mShreshold);
			}
		});		
	}
	
	/**
	 * 初始化播放器
	 */
	public void initRecorder() {
		int asrInitResult = mHciCloudAsrRecorderHelper.init(this);
        if (asrInitResult != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "asrRecorder init error, error code = " + asrInitResult);
            
            return;
        }
	}
	
	/**
	 * 返回阀值
	 * @return 阀值
	 */
	public int getShreshold() {
		return mShreshold;
	}

	@Override
	protected void onDestroy() {		
		//释放asr
		if (mHciCloudAsrRecorderHelper != null && mIsRecording) {
			mHciCloudAsrRecorderHelper.release();
		}		
		//释放系统
		mHciCloudSysHelper.release();
		
		super.onDestroy();
	}
}