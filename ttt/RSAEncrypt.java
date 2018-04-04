package com.sf.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sf.erui.common.exception.SfopenRuntimeException;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

/**
 * @Description:rsa 加密解密工具类
 * @author:80002271
 * @date:2018年4月2日 下午2:19:06
 */
@SuppressWarnings("restriction")
public class RSAEncrypt {
	private static final Logger LOGGER = LoggerFactory
	        .getLogger(RSAEncrypt.class);
	private static final String RSA = "RSA";
	private static final String FILEPATH = "D:\\rsa\\key";// 自我测试用的公私钥生成文件夹
	private static final String PUBLICKEY = "publicKey.keystore";
	private static final String PRIVATEKEY = "privateKey.keystore";

	/**
	 * Description: 随机生成密钥对
	 * 
	 * @param FILEPATH
	 *            秘钥存储地址
	 * @author 80002271
	 * @date 2018年4月2日 下午2:19:45
	 */
	public static void genKeyPair() {
		// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
		KeyPairGenerator keyPairGen = null;
		try {
			keyPairGen = KeyPairGenerator.getInstance(RSA);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("初始化失败,没有加密算法", e);
			return;
		}
		// 初始化密钥对生成器，密钥大小为96-1024位
		keyPairGen.initialize(1024, new SecureRandom());
		// 生成一个密钥对，保存在keyPair中
		KeyPair keyPair = keyPairGen.generateKeyPair();

		// 得到私钥
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		// 得到公钥
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		try (FileWriter pubfw = new FileWriter(FILEPATH + File.separatorChar
		        + PUBLICKEY);
		        BufferedWriter pubbw = new BufferedWriter(pubfw)) {
			// 得到公钥字符串
			String publicKeyString = Base64.encode(publicKey.getEncoded());
			LOGGER.debug("公钥字符串 : {}", publicKeyString);
			pubbw.write(publicKeyString);
			pubbw.flush();
		} catch (Exception e) {
			LOGGER.error("生成公钥出错", e);
		}
		try (FileWriter prifw = new FileWriter(FILEPATH + File.separatorChar
		        + PRIVATEKEY);
		        BufferedWriter pribw = new BufferedWriter(prifw)) {
			// 得到私钥字符串
			String privateKeyString = Base64.encode(privateKey.getEncoded());
			LOGGER.debug("私钥字符串 : {}", privateKeyString);
			pribw.write(privateKeyString);
			pribw.flush();
		} catch (Exception e) {
			LOGGER.error("生成秘钥出错", e);
		}
	}

	/**
	 * Description: 从文件中加载公钥
	 * 
	 * @param path
	 *            公钥地址
	 * @return 公钥字符串 @ 加载公钥时产生的异常
	 * @author 80002271
	 * @date 2018年4月2日 下午2:24:34
	 */
	private static String loadPublicKeyByFile(String path) {
		try (BufferedReader br = new BufferedReader(new FileReader(path
		        + File.separatorChar + PUBLICKEY))) {
			String readLine = null;
			StringBuilder sb = new StringBuilder();
			while ((readLine = br.readLine()) != null) {
				sb.append(readLine);
			}
			return sb.toString();
		} catch (IOException e) {
			throw new SfopenRuntimeException("公钥数据流读取错误");
		} catch (NullPointerException e) {
			throw new SfopenRuntimeException("公钥输入流为空");
		}
	}

	/**
	 * Description: 从字符串中加载公钥
	 * 
	 * @param publicKeyStr
	 *            公钥数据字符串
	 * @return 公钥 @ 加载公钥时产生的异常
	 * @author 80002271
	 * @date 2018年4月2日 下午2:25:34
	 */
	private static RSAPublicKey loadPublicKeyByStr(String publicKeyStr) {
		try {
			byte[] buffer = Base64.decode(publicKeyStr);
			KeyFactory keyFactory = KeyFactory.getInstance(RSA);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
			return (RSAPublicKey) keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException
		        | NullPointerException | Base64DecodingException e) {
			throw new SfopenRuntimeException(e);
		}
	}

	/**
	 * Description: 从文件中加载私钥
	 * 
	 * @param path
	 *            文件地址
	 * @return 返回私钥字符串 @ 异常
	 * @author 80002271
	 * @date 2018年4月2日 下午2:26:30
	 */
	private static String loadPrivateKeyByFile(String path) {
		try (BufferedReader br = new BufferedReader(new FileReader(path
		        + File.separatorChar + PRIVATEKEY))) {
			String readLine = null;
			StringBuilder sb = new StringBuilder();
			while ((readLine = br.readLine()) != null) {
				sb.append(readLine);
			}
			return sb.toString();
		} catch (IOException e) {
			throw new SfopenRuntimeException("私钥数据读取错误");
		} catch (NullPointerException e) {
			throw new SfopenRuntimeException("私钥输入流为空");
		}
	}

	/**
	 * Description: 私钥加密
	 * 
	 * @param privateKey
	 *            私钥
	 * @param plainTextData
	 *            明文数据
	 * @return @ 加密过程中的异常信息
	 * @author 80002271
	 * @date 2018年4月2日 下午2:27:42
	 */
	public static byte[] encrypt(RSAPrivateKey privateKey, byte[] plainTextData) {
		if (privateKey == null) {
			throw new SfopenRuntimeException("加密私钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			// 使用默认RSA
			cipher = Cipher.getInstance(RSA);
			cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			return cipher.doFinal(plainTextData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
		        | InvalidKeyException | IllegalBlockSizeException
		        | BadPaddingException e) {
			throw new SfopenRuntimeException(e);
		}
	}

	/**
	 * Description: 公钥加密
	 * 
	 * @param publicKey
	 *            公钥
	 * @param plainTextData
	 *            明文数据
	 * @return @ 加密过程中的异常信息
	 * @author 80002271
	 * @date 2018年4月2日 下午2:28:53
	 */
	private static byte[] encrypt(RSAPublicKey publicKey, byte[] plainTextData) {
		if (publicKey == null) {
			throw new SfopenRuntimeException("加密公钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			// 使用默认RSA
			cipher = Cipher.getInstance(RSA);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(plainTextData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
		        | InvalidKeyException | IllegalBlockSizeException
		        | BadPaddingException e) {
			throw new SfopenRuntimeException(e);
		}
	}

	/**
	 * Description: 从字符串中加载私钥
	 * 
	 * @param privateKeyStr
	 *            私钥字符串
	 * @return 私钥 @ 异常
	 * @author 80002271
	 * @date 2018年4月2日 下午2:29:38
	 */
	private static RSAPrivateKey loadPrivateKeyByStr(String privateKeyStr) {
		try {
			byte[] buffer = Base64.decode(privateKeyStr);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
			KeyFactory keyFactory = KeyFactory.getInstance(RSA);
			return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException
		        | NullPointerException | Base64DecodingException e) {
			throw new SfopenRuntimeException(e);
		}
	}

	/**
	 * Description: 公钥解密过程
	 * 
	 * @param publicKey
	 *            公钥
	 * @param cipherData
	 *            密文数据
	 * @return 明文 @ 解密过程中的异常信息
	 * @author 80002271
	 * @date 2018年4月2日 下午2:30:15
	 */
	public static byte[] decrypt(RSAPublicKey publicKey, byte[] cipherData) {
		if (publicKey == null) {
			throw new SfopenRuntimeException("解密公钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			// 使用默认RSA
			cipher = Cipher.getInstance(RSA);
			cipher.init(Cipher.DECRYPT_MODE, publicKey);
			return cipher.doFinal(cipherData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
		        | InvalidKeyException | IllegalBlockSizeException
		        | BadPaddingException e) {
			throw new SfopenRuntimeException(e);
		}
	}

	/**
	 * Description:私钥解密过程
	 * 
	 * @param privateKey
	 *            私钥
	 * @param cipherData
	 *            密文数据
	 * @return 明文 @
	 * @author 80002271
	 * @date 2018年4月2日 下午2:30:51
	 */
	private static byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData) {
		if (privateKey == null) {
			throw new SfopenRuntimeException("解密私钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			// 使用默认RSA
			cipher = Cipher.getInstance(RSA);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(cipherData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
		        | InvalidKeyException | IllegalBlockSizeException
		        | BadPaddingException e) {
			throw new SfopenRuntimeException(e);
		}
	}

	/**
	 * Description: 公钥加密
	 * 
	 * @param dataStr
	 *            明文
	 * @return 公钥加密 密文
	 * @author 80002271
	 * @date 2018年4月2日 下午3:17:37
	 */
	public static String getPublicEncrypt(String dataStr) {
		try {
			RSAPublicKey rsaPublicKey = RSAEncrypt
			        .loadPublicKeyByStr(RSAEncrypt
			                .loadPublicKeyByFile(FILEPATH));
			byte[] cipherData = RSAEncrypt.encrypt(rsaPublicKey,
			        dataStr.getBytes());
			return Base64.encode(cipherData);
		} catch (Exception e) {
			LOGGER.error("加密出错:{}", e);
		}
		return "";
	}

	/**
	 * Description:私钥机密
	 * 
	 * @param cipher
	 *            公钥加密密文
	 * @return 明文
	 * @author 80002271
	 * @date 2018年4月2日 下午3:24:01
	 */
	public static String getPrivateDecrypt(String cipher) {
		try {
			RSAPrivateKey privateKey = RSAEncrypt
			        .loadPrivateKeyByStr(RSAEncrypt
			                .loadPrivateKeyByFile(FILEPATH));
			byte[] res = RSAEncrypt.decrypt(privateKey, Base64.decode(cipher));
			return new String(res);
		} catch (Exception e) {
			LOGGER.error("解密出错:{}", e);
		}
		return "";
	}

	public static void main(String[] args) {
		LOGGER.debug("-----------------公钥公钥生成----------------------");
		LOGGER.debug("--------------公钥加密私钥解密过程-------------------");
		genKeyPair();
		String plainText = "公钥加密私钥解密过程";
		// 公钥加密过程
		String cipher = RSAEncrypt.getPublicEncrypt(plainText);
		// 私钥解密
		String restr = RSAEncrypt.getPrivateDecrypt(cipher);

		LOGGER.debug("原文：{}", plainText);
		LOGGER.debug("加密：{}", cipher);
		LOGGER.debug("解密：{}", restr);
	}

}
