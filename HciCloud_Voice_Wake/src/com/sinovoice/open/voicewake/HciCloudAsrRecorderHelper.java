package com.sinovoice.open.voicewake;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.sinovoice.hcicloudsdk.android.asr.recorder.ASRRecorder;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.asr.AsrConfig;
import com.sinovoice.hcicloudsdk.common.asr.AsrGrammarId;
import com.sinovoice.hcicloudsdk.common.asr.AsrInitParam;
import com.sinovoice.hcicloudsdk.common.asr.AsrRecogResult;
import com.sinovoice.hcicloudsdk.recorder.ASRRecorderListener;
import com.sinovoice.hcicloudsdk.recorder.RecorderEvent;
import com.sinovoice.open.sys.AccountInfo;

public class HciCloudAsrRecorderHelper {
	private static final String TAG = HciCloudAsrRecorderHelper.class.getSimpleName();
	
	/**
	 * 灵云asr帮助类实例
	 */
	private static HciCloudAsrRecorderHelper mInstance;
	
	/**
	 * HciCloudAsrRecorderActivity环境
	 */
	private Context mContext;
	private HciCloudAsrRecorderActivity mActivity;
	
	/**
	 * 使用的能力
	 */
	private String mCapKey;
	
	/**
	 * UI消息处理器
	 */
	private Handler mUIHandler;
	
	/**
	 * 灵云录音机类
	 */
	private ASRRecorder mAsrRecorder;
	
	/**
	 * 灵云录音机初始化参数
	 */	
	private AsrInitParam mAsrInitParam;
	
	/**
	 * 灵云录音机识别参数
	 */
	private AsrConfig mAsrConfig;
	
	/**
	 * 录音机返回情况
	 */
	public static final int RECORD_STATE = 1;
	public static final int RECORD_RESULT = 2;
	public static final int RECORD_ERROR = 3;
	
	/**
	 * 识别语法
	 */
	private String mGrammar = null;

	/**
	 * 单例模式的私有构造函数
	 */
	private HciCloudAsrRecorderHelper() {
		
	}

	/**
	 * 获取单例对象
	 * @return HciCloudAsrRecorderHelper的实例
	 */
	public static HciCloudAsrRecorderHelper getInstance() {
		if (mInstance == null) {
			mInstance = new HciCloudAsrRecorderHelper();
		}
		return mInstance;
	}

	/**
	 * Asr引擎初始化 
	 * 辅助工具 : AsrInitParam:该类的实例通过addParam(key, value)的方式添加Asr初始化的参数,
	 * 再通过getStringConfig() 获取初始化时需要的字符串参数 config 初始化方法:
	 * HciCloudAsr.hciAsrInit(config)
	 */
	public int init(Context context) {
		this.mCapKey = AccountInfo.getInstance().getCapKey();
		this.mContext = context;
		this.mActivity = (HciCloudAsrRecorderActivity)context;

		// 配置初始化参数
		mAsrInitParam = new AsrInitParam();
		String asrDirPath = mContext.getFilesDir().getPath().replace("files", "lib");
		mAsrInitParam.addParam(AsrInitParam.PARAM_KEY_INIT_CAP_KEYS, mCapKey);
		mAsrInitParam.addParam(AsrInitParam.PARAM_KEY_DATA_PATH, asrDirPath);
		mAsrInitParam.addParam(AsrInitParam.PARAM_KEY_FILE_FLAG, "android_so");
		
		// 初始化录音机参数
		if (mAsrRecorder == null) {
			mAsrRecorder = new ASRRecorder();
		}
		mAsrRecorder.init(mAsrInitParam.getStringConfig(),
				new ASRResultProcess());
		
		Log.v(TAG, "init parameters:" + mAsrInitParam.getStringConfig());	
		
		// 配置识别参数
		mAsrConfig = new AsrConfig();
		// PARAM_KEY_CAP_KEY 设置使用的能力
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_CAP_KEY, mCapKey);
		// PARAM_KEY_AUDIO_FORMAT 音频格式根据不同的能力使用不用的音频格式
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_AUDIO_FORMAT,
				AsrConfig.HCI_ASR_AUDIO_FORMAT_PCM_16K16BIT);
		// PARAM_KEY_ENCODE 音频编码压缩格式，使用OPUS可以有效减小数据流量
		// asrConfig.addParam(AsrConfig.PARAM_KEY_ENCODE, "opus");
		
		//开启持续录音
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_CONTINUOUS, "yes");
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_REALTIME, "yes");
		
		// 语法相关的配置,若使用自由说能力可以不必配置该项
		if (mCapKey.contains("grammar")) {
			mGrammar = loadGrammar("wordlist_utf8.txt");
			
			// 加载本地语法获取语法ID
			AsrGrammarId id = new AsrGrammarId();
			
			long startTime = System.currentTimeMillis();
			ASRRecorder.loadGrammar("grammarType=wordlist", mGrammar, id);
			long usedTime = System.currentTimeMillis() - startTime;
			
			Log.v(TAG, "loadGrammar usedTime = " + usedTime + "ms");
			
			// PARAM_KEY_GRAMMAR_TYPE 语法类型，使用自由说能力时，忽略以下此参数
			mAsrConfig.addParam(AsrConfig.PARAM_KEY_GRAMMAR_TYPE,
					AsrConfig.HCI_ASR_GRAMMAR_TYPE_ID);
			mAsrConfig.addParam(AsrConfig.PARAM_KEY_GRAMMAR_ID,
					"" + id.getGrammarId());
		}

		return HciErrorCode.HCI_ERR_NONE;
	}
	
	/**
	 * 加载语法文件
	 * @param fileName 语法文件名
	 * @return 语法字符串
	 */
	public String loadGrammar(String fileName) {
		String grammar = "";
		try {
			InputStream is = null;
			try {
				is = mContext.getAssets().open(fileName);
				byte[] data = new byte[is.available()];
				is.read(data);
				grammar = new String(data);
			} finally {
				is.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return grammar;
	}
	
	/**
	 * 开始录音
	 * @return 是否能够录音
	 */
	public boolean start() {
		Log.v(TAG, "asr config:" + mAsrConfig.getStringConfig());
		
		if (mAsrRecorder.getRecorderState() != ASRRecorder.RECORDER_STATE_IDLE) {
			return false;
		}
		
		mAsrRecorder.start(mAsrConfig.getStringConfig(), mGrammar);
		
		return true;	
	}

	/**
	 * 设置UI消息处理器
	 * @param handler 消息处理器
	 */
	public void setUIHandler(Handler handler) {
		mUIHandler = handler;
	}
	
	/**
	 * 录音机监听类.
	 * @author sinovoice
	 *监听录音机的返回情况
	 */
	private class ASRResultProcess implements ASRRecorderListener {
		private long startTime = 0;
		
		@Override
		public void onRecorderEventError(RecorderEvent arg0, int arg1) {
			String sError = "错误码为：" + arg1;
			Message m = mUIHandler.obtainMessage(RECORD_ERROR, sError);
			mUIHandler.sendMessage(m);
		}

		@Override
		public void onRecorderEventRecogFinsh(RecorderEvent recorderEvent,
				AsrRecogResult arg1) {
			long endTime = System.currentTimeMillis();
			long usedTime = endTime - startTime;
			Log.v(TAG, "recog used time = " + usedTime + "ms");
			
			if (recorderEvent == RecorderEvent.RECORDER_EVENT_RECOGNIZE_COMPLETE) {
				String sState = "识别状态：识别结束";
				Message m = mUIHandler.obtainMessage(RECORD_STATE,sState);
				mUIHandler.sendMessage(m);
			}
			if (arg1 != null) {
				String sResult = " ";
				if (arg1.getRecogItemList().size() > 0) {
					String result = arg1.getRecogItemList().get(0).getRecogResult();
					int score = arg1.getRecogItemList().get(0).getScore();
					
					int shreshold = mActivity.getShreshold();
					if(score >= shreshold){
						sResult = result + "\n分值：" + score;		
						MediaPlayer mp=MediaPlayer.create(mContext, R.raw.alert);
						mp.start();
						mp.setOnCompletionListener(new OnCompletionListener() {
							
							@Override
							public void onCompletion(MediaPlayer mp) {
								mp.release();
								mp = null;
							}
						});
					}
				
				} else {
					sResult = "未能正确识别,请重新输入";
				}
				Message m = mUIHandler.obtainMessage(RECORD_RESULT, sResult);
				mUIHandler.sendMessage(m);
			}
		}

		@Override
		public void onRecorderEventStateChange(RecorderEvent recorderEvent) {
			String sState = "";
			if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECORD) {
				sState = "识别状态：开始录音";
			} else if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECOGNIZE) {
				sState = "识别状态：开始识别";
				startTime = System.currentTimeMillis();
			} else if (recorderEvent == RecorderEvent.RECORDER_EVENT_NO_VOICE_INPUT) {
				sState = "识别状态：无音频输入";
			}
			Message m = mUIHandler.obtainMessage(RECORD_STATE, sState);
			mUIHandler.sendMessage(m);
		}

		@Override
		public void onRecorderRecording(byte[] volumedata, int volume) {
		}
	}
	
	/**
	 * 释放asr资源
	 */
	public void release() {	
		if(mAsrRecorder != null){
			Log.i(TAG, "释放asr");
			
			mAsrRecorder.release();
		}else {
			Log.e(TAG, "mAsrRecorder 为 null");
		}
	}
}
