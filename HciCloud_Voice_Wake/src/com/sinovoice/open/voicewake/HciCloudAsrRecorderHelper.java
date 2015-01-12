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
	 * ����asr������ʵ��
	 */
	private static HciCloudAsrRecorderHelper mInstance;
	
	/**
	 * HciCloudAsrRecorderActivity����
	 */
	private Context mContext;
	private HciCloudAsrRecorderActivity mActivity;
	
	/**
	 * ʹ�õ�����
	 */
	private String mCapKey;
	
	/**
	 * UI��Ϣ������
	 */
	private Handler mUIHandler;
	
	/**
	 * ����¼������
	 */
	private ASRRecorder mAsrRecorder;
	
	/**
	 * ����¼������ʼ������
	 */	
	private AsrInitParam mAsrInitParam;
	
	/**
	 * ����¼����ʶ�����
	 */
	private AsrConfig mAsrConfig;
	
	/**
	 * ¼�����������
	 */
	public static final int RECORD_STATE = 1;
	public static final int RECORD_RESULT = 2;
	public static final int RECORD_ERROR = 3;
	
	/**
	 * ʶ���﷨
	 */
	private String mGrammar = null;

	/**
	 * ����ģʽ��˽�й��캯��
	 */
	private HciCloudAsrRecorderHelper() {
		
	}

	/**
	 * ��ȡ��������
	 * @return HciCloudAsrRecorderHelper��ʵ��
	 */
	public static HciCloudAsrRecorderHelper getInstance() {
		if (mInstance == null) {
			mInstance = new HciCloudAsrRecorderHelper();
		}
		return mInstance;
	}

	/**
	 * Asr�����ʼ�� 
	 * �������� : AsrInitParam:�����ʵ��ͨ��addParam(key, value)�ķ�ʽ���Asr��ʼ���Ĳ���,
	 * ��ͨ��getStringConfig() ��ȡ��ʼ��ʱ��Ҫ���ַ������� config ��ʼ������:
	 * HciCloudAsr.hciAsrInit(config)
	 */
	public int init(Context context) {
		this.mCapKey = AccountInfo.getInstance().getCapKey();
		this.mContext = context;
		this.mActivity = (HciCloudAsrRecorderActivity)context;

		// ���ó�ʼ������
		mAsrInitParam = new AsrInitParam();
		String asrDirPath = mContext.getFilesDir().getPath().replace("files", "lib");
		mAsrInitParam.addParam(AsrInitParam.PARAM_KEY_INIT_CAP_KEYS, mCapKey);
		mAsrInitParam.addParam(AsrInitParam.PARAM_KEY_DATA_PATH, asrDirPath);
		mAsrInitParam.addParam(AsrInitParam.PARAM_KEY_FILE_FLAG, "android_so");
		
		// ��ʼ��¼��������
		if (mAsrRecorder == null) {
			mAsrRecorder = new ASRRecorder();
		}
		mAsrRecorder.init(mAsrInitParam.getStringConfig(),
				new ASRResultProcess());
		
		Log.v(TAG, "init parameters:" + mAsrInitParam.getStringConfig());	
		
		// ����ʶ�����
		mAsrConfig = new AsrConfig();
		// PARAM_KEY_CAP_KEY ����ʹ�õ�����
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_CAP_KEY, mCapKey);
		// PARAM_KEY_AUDIO_FORMAT ��Ƶ��ʽ���ݲ�ͬ������ʹ�ò��õ���Ƶ��ʽ
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_AUDIO_FORMAT,
				AsrConfig.HCI_ASR_AUDIO_FORMAT_PCM_16K16BIT);
		// PARAM_KEY_ENCODE ��Ƶ����ѹ����ʽ��ʹ��OPUS������Ч��С��������
		// asrConfig.addParam(AsrConfig.PARAM_KEY_ENCODE, "opus");
		
		//��������¼��
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_CONTINUOUS, "yes");
		mAsrConfig.addParam(AsrConfig.PARAM_KEY_REALTIME, "yes");
		
		// �﷨��ص�����,��ʹ������˵�������Բ������ø���
		if (mCapKey.contains("grammar")) {
			mGrammar = loadGrammar("wordlist_utf8.txt");
			
			// ���ر����﷨��ȡ�﷨ID
			AsrGrammarId id = new AsrGrammarId();
			
			long startTime = System.currentTimeMillis();
			ASRRecorder.loadGrammar("grammarType=wordlist", mGrammar, id);
			long usedTime = System.currentTimeMillis() - startTime;
			
			Log.v(TAG, "loadGrammar usedTime = " + usedTime + "ms");
			
			// PARAM_KEY_GRAMMAR_TYPE �﷨���ͣ�ʹ������˵����ʱ���������´˲���
			mAsrConfig.addParam(AsrConfig.PARAM_KEY_GRAMMAR_TYPE,
					AsrConfig.HCI_ASR_GRAMMAR_TYPE_ID);
			mAsrConfig.addParam(AsrConfig.PARAM_KEY_GRAMMAR_ID,
					"" + id.getGrammarId());
		}

		return HciErrorCode.HCI_ERR_NONE;
	}
	
	/**
	 * �����﷨�ļ�
	 * @param fileName �﷨�ļ���
	 * @return �﷨�ַ���
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
	 * ��ʼ¼��
	 * @return �Ƿ��ܹ�¼��
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
	 * ����UI��Ϣ������
	 * @param handler ��Ϣ������
	 */
	public void setUIHandler(Handler handler) {
		mUIHandler = handler;
	}
	
	/**
	 * ¼����������.
	 * @author sinovoice
	 *����¼�����ķ������
	 */
	private class ASRResultProcess implements ASRRecorderListener {
		private long startTime = 0;
		
		@Override
		public void onRecorderEventError(RecorderEvent arg0, int arg1) {
			String sError = "������Ϊ��" + arg1;
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
				String sState = "ʶ��״̬��ʶ�����";
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
						sResult = result + "\n��ֵ��" + score;		
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
					sResult = "δ����ȷʶ��,����������";
				}
				Message m = mUIHandler.obtainMessage(RECORD_RESULT, sResult);
				mUIHandler.sendMessage(m);
			}
		}

		@Override
		public void onRecorderEventStateChange(RecorderEvent recorderEvent) {
			String sState = "";
			if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECORD) {
				sState = "ʶ��״̬����ʼ¼��";
			} else if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECOGNIZE) {
				sState = "ʶ��״̬����ʼʶ��";
				startTime = System.currentTimeMillis();
			} else if (recorderEvent == RecorderEvent.RECORDER_EVENT_NO_VOICE_INPUT) {
				sState = "ʶ��״̬������Ƶ����";
			}
			Message m = mUIHandler.obtainMessage(RECORD_STATE, sState);
			mUIHandler.sendMessage(m);
		}

		@Override
		public void onRecorderRecording(byte[] volumedata, int volume) {
		}
	}
	
	/**
	 * �ͷ�asr��Դ
	 */
	public void release() {	
		if(mAsrRecorder != null){
			Log.i(TAG, "�ͷ�asr");
			
			mAsrRecorder.release();
		}else {
			Log.e(TAG, "mAsrRecorder Ϊ null");
		}
	}
}
