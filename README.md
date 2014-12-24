灵云语音唤醒（Android版）
============================

灵云语音唤醒是北京捷通华声语音技术有限公司自主研发的智能人机交互软件，集成软件后，只需对设备说出制定唤醒词，即可轻松打开应用设备，做到无需触碰，直接开启，随时随地享受智能人机交互技术的快捷与便利。
如果您对灵云智能语音唤醒有什么好的建议或意见，欢迎与我们联系。

 
如何安装
============
1. 首先需要checkout出项目。[https://github.com/open-sinovoice/sinovoice-voicewake.git](https://github.com/open-sinovoice/sinovoice-voicewake.git) 。
2. 访问灵云开放平台&灵云开发者社区：[http://www.hcicloud.com/](http://www.hcicloud.com/)， 注册账号并创建一个应用。获取DeveloperKey，AppKey，CloudUrl值，申请时要点选所需能力（asr.local.grammar.chinese.v4）。
3. 在IDE开发工具中导入HciCloud_Voice_Wake工程，将DeveloperKey，AppKey，CloudUrl的值填写在HciCloud_Voice_Wake工程中assets/AccountInfo.txt文件中对应字段。
4. 在IDE中编译运行，确保调试终端可以正常访问网络，获取到灵云能力的授权，即可在调试终端查看和使用灵云语音唤醒。


如何使用
============

1. 点击开始唤醒按钮，此时设备开始监听音频。
3. 对设备说出定制唤醒词。
4. 设备自动进行语音识别，可以在看到识别结果以及本次识别给出的置信度分值。实际应用可以根据识别结果的分值自定义判断阀值。如果语音识别分值（每次识别均有分值）高于所设定阈值，则设备判定此次识别结果是可信的，否则判定为识别结果是不可信的。推荐使用30为阀值。
5. 点击停止唤醒按钮，此时设备停止监听音频。
5. 用户自己定义唤醒词，直接修改assets/wordlist_utf8.txt文件中的词表即可。



LICENSE
==============
Licensed under the MIT(Massachusetts Institute of Technology)


相关资源
============
1. 灵云开放平台&灵云开发者社区：[http://www.hcicloud.com/](http://www.hcicloud.com/) 。
2. 捷通华声灵云免费提供云端六大能力，包括语音识别ASR、语音合成TTS、手写识别HWR、图像识别OCR、语义理解NLU、机器翻译MT。
3. 登陆灵云开放平台及开发者社区，注册开发者账号，即可免费下载、使用六大能力SDK。

