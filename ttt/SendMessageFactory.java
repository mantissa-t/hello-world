package com.sf.util;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;


public class SendMessageFactory {

//	private static WebApplicationContext context = ContextLoader.getCurrentWebApplicationContext();
//	
//	public static SendMessageBiz getSendMessageInstance(String actionType) {
//		SendMessageBiz sendMessageBiz = null;
//        if(actionType.equals(SendMessageActionEnum.exam.getValue())) {
//        	sendMessageBiz = (SendMessageBiz)context.getBean("sendMessageExamBiz");
//        }else if(actionType.equals(SendMessageActionEnum.clazz.getValue())){
//        	sendMessageBiz = (SendMessageBiz)context.getBean("sendMessageClazzBiz");
//        }else if(actionType.equals(SendMessageActionEnum.level.getValue())){
//        	sendMessageBiz = (SendMessageBiz)context.getBean("sendMessageLevelBiz");
//        }else if(actionType.equals(SendMessageActionEnum.que.getValue())){
//        	sendMessageBiz = (SendMessageBiz)context.getBean("sendMessageQueBiz");
//        }else if(actionType.equals(SendMessageActionEnum.areatotalparty.getValue())){
//        	sendMessageBiz = (SendMessageBiz)context.getBean("sendMessageAreatotalPartyBiz");
//        }
//        
//        return sendMessageBiz;
//	}
}
