package com.sf.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLClient extends HttpClientBuilder {
	private static final String HTTP = "http";
	private static final String HTTPS = "https";
	private static SSLConnectionSocketFactory sslsf = null;
	private static PoolingHttpClientConnectionManager cm = null;

	private static final Logger logger = LoggerFactory
	        .getLogger(SSLClient.class);

	public SSLClient() {
		super();
		try {

			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain,
				        String authType) throws CertificateException {
					logger.info("init checkClientTrusted1 ");
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain,
				        String authType) throws CertificateException {
					logger.info("init checkServerTrusted");
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[] {};
				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
			        ctx);
			Registry<ConnectionSocketFactory> registry = RegistryBuilder
			        .<ConnectionSocketFactory> create()
			        .register(HTTP, new PlainConnectionSocketFactory())
			        .register(HTTPS, sslsf).build();
			cm = new PoolingHttpClientConnectionManager(registry);
			cm.setMaxTotal(200);// max connection
			// SSLSocketFactory ssf = new SSLSocketFactory(ctx,
			// SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			// HttpClientConnectionManager ccm
			// ClientConnectionManager ccm = this.getConnectionManager();
			// SchemeRegistry sr = ccm.getSchemeRegistry();
			// sr.register(new Scheme("https", 443, ssf));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}
}
