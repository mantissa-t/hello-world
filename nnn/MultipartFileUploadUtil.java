package com.hnjme.core.util.attach;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import com.hnjme.core.util.exception.JmeException;
import com.hnjme.pojo.attach.MultipartFileParam;

/**
 * @description
 * @author Luo Liang
 * @date 2017年04月17日
 * @since Version 1.0
 */
public class MultipartFileUploadUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultipartFileUploadUtil.class);
  private static AtomicLong counter = new AtomicLong(0L);

  /** 断点续传分片大小常量 5M */
  private static long CHUNKSIZE = 5 * 1024 * 1024;


  /**
   * 在HttpServletRequest中获取分段上传文件请求的信息
   * 
   * @param request
   * @return
   */
  public static MultipartFileParam parseRequest(HttpServletRequest request, String tmpDir) {
    String prefix = "附件上传请求队列号【" + counter.incrementAndGet() + "】:";
    MultipartFileParam param = new MultipartFileParam();
    LOGGER.info("MultipartFileUploadUtil.parseRequest(request):开始处理文件上传请求......");
    StandardMultipartHttpServletRequest smRequest = (StandardMultipartHttpServletRequest) request;
    Map<String, String[]> requestParams = smRequest.getParameterMap();
    Iterator<Entry<String, String[]>> itor = requestParams.entrySet().iterator();
    String key = "";
    String value = "";
    HashMap<String, String> otherParam = new HashMap<>();
    while (itor.hasNext()) {
      Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) itor.next();
      key = entry.getKey().toString().toLowerCase();
      value = entry.getValue()[0];
      switch (key) {
        case "id":
          param.setId(value);
          break;
        case "name":
          param.setFileName(value);
          break;
        case "chunks":
          param.setChunked(true);
          param.setChunks(NumberUtils.toInt(value));
          break;
        case "chunk":
          param.setChunked(true);
          param.setChunk(NumberUtils.toInt(value));
          break;
        case "size":
          param.setSize(NumberUtils.toLong(value));
        default:
          otherParam.put(key, value);
          break;
      }
    }
    param.setParam(otherParam);
    try {
      LOGGER.debug(prefix + "本次上传情况为[" + param.getChunk() + "/" + param.getChunks() + "]");
      MultipartFile file = smRequest.getMultiFileMap().getFirst("file");
      File tmpDirFile = new File(request.getServletContext().getRealPath("/") + tmpDir);
      if (!tmpDirFile.exists()) {
        tmpDirFile.mkdirs();
      }
      File targetFile = new File(tmpDirFile, param.getFileName());
      if (param.isChunked()) {
        long chunkSize = CHUNKSIZE;
        File confFile = new File(tmpDir, param.getFileName() + ".conf");
        File tmpFile = new File(tmpDir, param.getFileName() + ".depart");
        RandomAccessFile accessTmpFile = new RandomAccessFile(tmpFile, "rw");
        RandomAccessFile accessConfFile = new RandomAccessFile(confFile, "rw");
        long offset = chunkSize * param.getChunk();
        accessTmpFile.seek(offset);
        accessTmpFile.write(file.getBytes());
        LOGGER.debug(prefix + "文件处理完成！");
        accessConfFile.setLength(param.getChunks());
        accessConfFile.seek(param.getChunk());
        accessConfFile.write(Byte.MAX_VALUE);
        byte[] completeList = FileUtils.readFileToByteArray(confFile);
        byte isComplete = Byte.MAX_VALUE;
        for (int i = 0; i < completeList.length && isComplete == Byte.MAX_VALUE; i++) {
          // 与运算, 如果有部分没有完成则 isComplete 不是 Byte.MAX_VALUE
          isComplete = (byte) (isComplete & completeList[i]);
        }
        accessTmpFile.close();
        accessConfFile.close();
        if (isComplete == Byte.MAX_VALUE) {
          LOGGER.debug("文件上传处理完成！");
          FileUtils.copyFile(tmpFile, targetFile);
          param.setCompleted(isComplete == Byte.MAX_VALUE);
          confFile.delete();
          tmpFile.delete();
        }

      } else {
        file.transferTo(targetFile);
        param.setCompleted(true);
      }
      param.setFilePath(targetFile.getAbsolutePath());
    } catch (FileNotFoundException e) {
      // TODO 自动生成的 catch 块
      LOGGER.error("上传文件失败,失败原因：文件没找到",e);
      throw new JmeException("上传文件失败,失败原因：文件没找到");
    } catch (IllegalStateException e) {
      // TODO 自动生成的 catch 块
      LOGGER.error("上传文件失败,失败原因：文件没找到",e);
      throw new JmeException("上传文件失败,失败原因：文件没找到");
    } catch (IOException e) {
      LOGGER.error("上传文件失败,失败原因：IO错误",e);
      throw new JmeException("上传文件失败,失败原因：IO错误");
    } catch (Exception e) {
      LOGGER.error("上传文件失败",e);
      throw new JmeException("上传文件失败");
    }
    return param;
  }
}
