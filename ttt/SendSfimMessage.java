package com.sf.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SendSfimMessage extends Thread {

	private SfimMessageCenter messageCenter;

	private static final Logger log = LoggerFactory
	        .getLogger(SfimMessageCenter.class);

//	private String url = SysConstants.getConfigValue("SFIM_SEND_MESSAGE");
	private String url = "";

	private String charset = "utf-8";
	private HttpClientUtil httpClientUtil = null;
	private HttpClient httpClient = null;
	private HttpPost httpPost = null;

	public SendSfimMessage(SfimMessageCenter messageCenter) {
		this.messageCenter = messageCenter;
		httpClientUtil = new HttpClientUtil();
		try {
//			httpClient = new SSLClient();
			httpPost = new HttpPost(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		Map<String, String> createMap = new HashMap<String, String>();
		createMap.put("from", "kms"); // 发送者
		createMap.put("to", messageCenter.getReceived()); // 接收者
		createMap.put("sendtype", "0"); // 发送类型

		/** 默认为学习平台发送，学习平台系统，其他微服务也可调用 */
		if (StringUtils.isNotBlank(messageCenter.getMsgkey())
		        && StringUtils.isNotBlank(messageCenter.getPwd())) {
			createMap.put("msgkey", messageCenter.getMsgkey());// 发送账号
			createMap.put("pwd", messageCenter.getPwd()); // 密码
		} else {
			createMap.put("msgkey", "learning");// 发送账号
			createMap.put("pwd", "learning"); // 密码
		}

		createMap.put("title", messageCenter.getTitle()); // 标题
		createMap.put("content", messageCenter.getContent()); // 内容
		createMap.put("msgContent", messageCenter.getMsgContent()); // 消息内容

		if (StringUtils.isNotBlank(messageCenter.getCanJump())) {
			createMap.put("canJump", messageCenter.getCanJump()); // 是否可跳转
			createMap.put("jumpParams", "{\"pc\":{},\"app\":{}}"); // 跳转参数
		} else {
			createMap.put("canJump", "1"); // 是否可跳转
			createMap.put("jumpParams", "{\"pc\":{\"loadUrl\":\""
			        + messageCenter.getPcUrl() + "\"},\"app\":{\"loadUrl\":\""
			        + messageCenter.getAppUrl() + "\"}}"); // 跳转参数
		}

		createMap.put("jumpType", "1"); // 跳转类型

		createMap.put("extdata", "{}"); // 扩展数据

		createMap.put("appkey", "d0f0e645a710e919901fda410b7df7ea");
		createMap.put("appsecret", "63560b42e510");

		try {
			String httpOrgCreateTestRtn = httpClientUtil.doPost(createMap,
			        charset, httpClient, httpPost);
			System.out.println("result:" + "==========" + httpOrgCreateTestRtn);
			log.info("result:" + "==========" + httpOrgCreateTestRtn);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		SfimMessageCenter messageCenter = new SfimMessageCenter(
		        "01116376",
		        "创建考试",
		        "诚邀您参与调研：新消息123456，调研时间：2018-03-22 ",
		        "诚邀您参与调研：新消息123456，调研时间：2018-03-22",
		        "http://kms.sf-express.com/KMS/assessmentquestionnaire/questionnaire_start.action?questionnaireId=11541",
		        "index.html#/diaoyan/11541?scan=true");
		SendSfimMessage sm = new SendSfimMessage(messageCenter);
		sm.run();
	}

}
