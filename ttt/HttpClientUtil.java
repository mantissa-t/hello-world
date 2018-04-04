package com.sf.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil {

	private static final Logger logger = LoggerFactory
	        .getLogger(HttpClientUtil.class);

	public String doPost(Map<String, String> map, String charset,
	        HttpClient httpClient, HttpPost httpPost) {

		String result = null;
		try {

			List<NameValuePair> list = new ArrayList<>();
			Iterator<Entry<String, String>> iterator = map.entrySet()
			        .iterator();
			while (iterator.hasNext()) {
				Entry<String, String> elem = iterator.next();
				list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
			}
			if (!list.isEmpty()) {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list,
				        charset);
				httpPost.setEntity(entity);
			}
			HttpResponse response = httpClient.execute(httpPost);
			if (response != null) {
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					result = EntityUtils.toString(resEntity, charset);
				}
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return result;
	}
}