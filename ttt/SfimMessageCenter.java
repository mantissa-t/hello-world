package com.sf.util;

import java.io.Serializable;

/**
 * 顺丰im消息中心 消息实体
 */
public class SfimMessageCenter implements Serializable {

	/**  **/
	private static final long serialVersionUID = 6236092434081041661L;

	/** 发送账号 */
	private String msgkey;

	/** 密码 */
	private String pwd;

	/** 接收人，多个工号以，号分割 */
	private String received;

	/** 消息标题 */
	private String title;

	/** 内容 */
	private String content;

	/** 消息内容 */
	private String msgContent;

	/** pc跳转地址 */
	private String pcUrl;

	/** app跳转地址 */
	private String appUrl;

	/** 是否可跳转 */
	private String canJump;

	public SfimMessageCenter(String received, String title, String content,
	        String msgContent, String pcUrl, String appUrl) {
		super();
		this.received = received;
		this.title = title;
		this.content = content;
		this.msgContent = msgContent;
		this.pcUrl = pcUrl;
		this.appUrl = appUrl;
	}

	public SfimMessageCenter() {
	}

	/**
	 * Gets the 接收人，多个工号以，号分割.
	 *
	 * @return the 接收人，多个工号以，号分割
	 */
	public String getReceived() {
		return received;
	}

	/**
	 * Sets the 接收人，多个工号以，号分割.
	 *
	 * @param received
	 *            the new 接收人，多个工号以，号分割
	 */
	public void setReceived(String received) {
		this.received = received;
	}

	/**
	 * Gets the 消息标题.
	 *
	 * @return the 消息标题
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the 消息标题.
	 *
	 * @param title
	 *            the new 消息标题
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the 内容.
	 *
	 * @return the 内容
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Sets the 内容.
	 *
	 * @param content
	 *            the new 内容
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Gets the 消息内容.
	 *
	 * @return the 消息内容
	 */
	public String getMsgContent() {
		return msgContent;
	}

	/**
	 * Sets the 消息内容.
	 *
	 * @param msgContent
	 *            the new 消息内容
	 */
	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}

	/**
	 * Gets the pc跳转地址.
	 *
	 * @return the pc跳转地址
	 */
	public String getPcUrl() {
		return pcUrl;
	}

	/**
	 * Sets the pc跳转地址.
	 *
	 * @param pcUrl
	 *            the new pc跳转地址
	 */
	public void setPcUrl(String pcUrl) {
		this.pcUrl = pcUrl;
	}

	/**
	 * Gets the app跳转地址.
	 *
	 * @return the app跳转地址
	 */
	public String getAppUrl() {
		return appUrl;
	}

	/**
	 * Sets the app跳转地址.
	 *
	 * @param appUrl
	 *            the new app跳转地址
	 */
	public void setAppUrl(String appUrl) {
		this.appUrl = appUrl;
	}

	/**
	 * Gets the 发送账号.
	 *
	 * @return the 发送账号
	 */
	public String getMsgkey() {
		return msgkey;
	}

	/**
	 * Sets the 发送账号.
	 *
	 * @param msgkey
	 *            the new 发送账号
	 */
	public void setMsgkey(String msgkey) {
		this.msgkey = msgkey;
	}

	/**
	 * Gets the 密码.
	 *
	 * @return the 密码
	 */
	public String getPwd() {
		return pwd;
	}

	/**
	 * Sets the 密码.
	 *
	 * @param pwd
	 *            the new 密码
	 */
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	/**
	 * Gets the 是否可跳转.
	 *
	 * @return the 是否可跳转
	 */
	public String getCanJump() {
		return canJump;
	}

	/**
	 * Sets the 是否可跳转.
	 *
	 * @param canJump
	 *            the new 是否可跳转
	 */
	public void setCanJump(String canJump) {
		this.canJump = canJump;
	}

	/**
	 * 
	 *
	 * @return
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "SfimMessageCenter [msgkey=" + msgkey + ", pwd=" + pwd
		        + ", received=" + received + ", title=" + title + ", content="
		        + content + ", msgContent=" + msgContent + ", pcUrl=" + pcUrl
		        + ", appUrl=" + appUrl + ", canJump=" + canJump + "]";
	}
}
