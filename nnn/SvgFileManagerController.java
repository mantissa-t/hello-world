package com.hnjme.controller.jtechsvg;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.hnjme.core.enumeration.DataRespEnum;
import com.hnjme.core.util.base.DataResp;
import com.hnjme.core.util.constant.ConstantCore;
import com.hnjme.core.util.exception.ExceptionMsgUtil;
import com.hnjme.core.util.exception.JmeException;
import com.hnjme.core.util.pagequery.DataGridPage;
import com.hnjme.core.util.pagequery.PageQuery;
import com.hnjme.pojo.system.Company;
import com.hnjme.pojo.system.SysUser;
import com.hnjme.service.jtechsvg.SvgFileService;
import com.hnjme.service.system.CompanyService;
import com.hnjme.service.system.SysLogService;

/**
 * RTV svg 文件管理 控制器
 * 
 * @author wangyong 2017-3-21
 *
 */
@RequestMapping("rtv")
@Controller
public class SvgFileManagerController {

	/**
	 * 日志记录
	 */
	private static final Logger logger = LoggerFactory.getLogger(SvgFileManagerController.class);

	/**
	 * RTV svg文件业务逻辑处理层接口
	 */
	@Autowired
	private SvgFileService svgFileService;

	/**
	 * 系统操作日志业务逻辑处理层接口
	 */
	@Autowired
	private SysLogService sysLogService;
	
	/**
	 * 公司业务逻辑处理层接口
	 */
	@Autowired
	private CompanyService companyService;

	/**
	 * 跳转到RTV svg文件管理页面
	 * 
	 * @Title: toList
	 * @param svgFolder
	 *            svgFolder 子文件夹名 前后都不需要 /
	 * @return: ModelAndView RTV svg文件管理页面
	 */
	// @RequiresPermissions("/dataPage/toList")
	@RequestMapping(value = "/toList/{svgFolder}", method = RequestMethod.GET)
	public ModelAndView toList(@PathVariable String svgFolder) {
		ModelAndView model = new ModelAndView("rtv/svgFileList");
		Company company = companyService.findByCode(svgFolder);
		if (company != null) {
			model.addObject("stationId", company.getStationId());
		} else {
			model.addObject("stationId", 0);
		}
		model.addObject("svgFolder", svgFolder);
		return model;
	}

	/**
	 * 分页查询RTV svg文件集合
	 * 
	 * @param svgFolder
	 *            svgFolder 子文件夹名 前后都不需要 /
	 * @return RTV svg文件集合分页查询结构
	 */
	@RequestMapping("/pageQuery/{svgFolder}")
	@ResponseBody
	public Object pageQuery(@PathVariable String svgFolder, PageQuery pageQuery) {
		// 获取分页查询相关参数
		String fileNameLike = pageQuery.getSearchMap() != null ? pageQuery.getSearchMap().get("name") : null;
		int count = svgFileService.svgFileCount(svgFolder,fileNameLike);
		
		List<Map<String, Object>> datas = svgFileService.listSvgFileXmlInfos(svgFolder,fileNameLike, pageQuery.getOffset(),
				pageQuery.getOffset() + pageQuery.getLimit());
		// 将查询结果转换成grid要求的数据格式
		DataGridPage<Map<String, Object>> result = new DataGridPage(count, datas);

		return result;
	}

	/**
	 * 跳转到RTV svg文件增加页面
	 * 
	 * @author wangyong
	 * @return RTV svg文件增加页面
	 */
	@RequestMapping(value = "/toAdd/{svgFolder}", method = RequestMethod.GET)
	public ModelAndView toAdd(@PathVariable String svgFolder) {
		ModelAndView model = new ModelAndView("rtv/svgFileEditDlg");

		// model.addObject("report", new HashMap<>());
		model.addObject(ConstantCore.PARAMKEY_OPRTYPE, ConstantCore.OPRTYPE_UPDATE);
		model.addObject("svgFolder", svgFolder);
		return model;
	}

	/**
	 * 跳转到RTV svg文件修改页面
	 * 
	 * @author wangyong
	 * @param svgFolder
	 *            svgFolder 子文件夹名 前后都不需要 /
	 * @param svgFileName
	 *            RTV svg文件name
	 * @return RTV svg文件修改页面
	 */
	@RequestMapping(value = "/toUpt/{svgFolder}", method = RequestMethod.GET)
	public ModelAndView toUpt(HttpServletRequest request, HttpServletResponse response, @PathVariable String svgFolder,
			String svgFileName) {
		ModelAndView model = new ModelAndView("rtv/svgFileEditDlg");
		// DataPage dataPage = dataPageService.find(id);
		model.addObject("name", svgFileName);
		try {
			model.addObject("svgFolder", svgFolder);
			model.addObject("svgFileName", svgFileName);
			// 将XML放到Html里显示需要转义
			model.addObject("svgXml", org.apache.commons.lang.StringEscapeUtils
					.escapeXml(svgFileService.getSvgFromFile(svgFolder, svgFileName)));
		} catch (Exception e) {
		}
		model.addObject(ConstantCore.PARAMKEY_OPRTYPE, ConstantCore.OPRTYPE_UPDATE);
		return model;
	}

	/**
	 * 保存修改RTV svg文件
	 * 
	 * @author wangyong
	 * @param svgFolder
	 *            svgFolder 子文件夹名 前后都不需要 /
	 * @param svgFileName
	 *            待修改RTV svg文件
	 * @param result
	 *            Spring校验返回结果
	 * @return 请求响应体
	 */

	@RequestMapping(value = "/uptData/{svgFolder}", method = RequestMethod.POST)
	@ResponseBody
	public Object uptData(HttpServletRequest request, HttpServletResponse response, @PathVariable String svgFolder,
			String svgFileName, String svgXml) {
	
		// 定义统一的请求响应体
		DataResp resp = new DataResp();
		if (svgFolder != null && svgFolder.length() > 0 && svgFileName != null && svgFileName.length() > 0) {

			try {

				if (svgXml != null && svgXml.length() > 0) {
					// 提交过来的XML文件被转义过了.
					svgXml = org.apache.commons.lang.StringEscapeUtils.unescapeXml(svgXml);
					svgFileService.saveSvgFile(svgFolder, svgFileName, getUsernameFromHttpSession(request.getSession()), svgXml);
				}

			} catch (Exception e) {
			}
			resp.setDataRespEnum(DataRespEnum.SUCCESS);
			sysLogService.insertOperatLog("update", "sys", "修改RTV svg文件[" + svgFolder + "/" + svgFileName + "]信息");
		} else {
			resp.setDataRespEnum(DataRespEnum.OTHER);
			resp.setMsg("RTV svg文件名称不可以为空");
		}

		return resp;
	}

	/**
	 * 删除RTV svg文件信息
	 * 
	 * @param svgFolder
	 *            svgFolder 子文件夹名 前后都不需要 /
	 * @param svgFileName
	 *            RTV svg文件svgFileName
	 * @return: 请求响应体
	 */
	@RequestMapping(value = "/delData/{svgFolder}", method = RequestMethod.POST)
	@ResponseBody
	public Object delete(@PathVariable String svgFolder, String svgFileName) {
		DataResp resp = new DataResp();
		svgFileService.removeSvgFile(svgFolder, svgFileName);
		resp.setDataRespEnum(DataRespEnum.SUCCESS);
		sysLogService.insertOperatLog("delete", "sys", "删除RTV svg文件[" + svgFolder + "/" + svgFileName + "]");
		return resp;
	}

	/**
	 * 指定svgFolder文件夹中的首页为svgFileName
	 * 
	 * @param svgFolder
	 *            svgFolder 子文件夹名 前后都不需要 /
	 * @param svgFileName
	 *            RTV svg文件svgFileName
	 * @return: 请求响应体
	 */
	@RequestMapping(value = "/setFirstPage/{svgFolder}", method = RequestMethod.POST)
	@ResponseBody
	public Object setFirstPage(@PathVariable String svgFolder, String svgFileName) {
		DataResp resp = new DataResp();
		svgFileService.setSetMap(svgFolder, SvgFileService.FIRST_PAGE_SET_KEY, svgFileName);
		resp.setDataRespEnum(DataRespEnum.SUCCESS);
		sysLogService.insertOperatLog("setFirstPage", "sys", "指定首页RTV svg文件[" + svgFolder + "/" + svgFileName + "]");
		return resp;
	}

	/**
	 * 清空RTV svg文件模板相关的内缓存
	 * 
	 * @return: 请求响应体
	 */
	@RequestMapping(value = "/clearSvgFileCache", method = RequestMethod.POST)
	@ResponseBody
	public Object clearSvgFileCache() {
		DataResp resp = new DataResp();
		svgFileService.clearCache();
		resp.setDataRespEnum(DataRespEnum.SUCCESS);
		sysLogService.insertOperatLog("delete", "sys", "清除缓存成功");
		return resp;
	}
	
	
	/**
	 * 清空RTV svg文件模板相关的内缓存
	 * 
	 * @return: 请求响应体
	 */
	@RequestMapping(value = "/clearSvgFiles/{svgFolder}", method = RequestMethod.POST)
	@ResponseBody
	public Object clearSvgFiles(@PathVariable String svgFolder) {
		DataResp resp = new DataResp();
		svgFileService.clearSvgFiles(svgFolder);
		resp.setDataRespEnum(DataRespEnum.SUCCESS);
		sysLogService.insertOperatLog("delete", "sys", "清除缓存成功");
		return resp;
	}
	/**
	 * 跳转到RTV svg文件增加页面
	 * 
	 * @author wangyong
	 * @return RTV svg文件增加页面
	 */
	@RequestMapping(value = "/toUpload/{svgFolder}", method = RequestMethod.GET)
	public ModelAndView toUpload(@PathVariable String svgFolder) {
		ModelAndView model = new ModelAndView("rtv/svgFileUploadDlg");

		// model.addObject("report", new HashMap<>());
		model.addObject(ConstantCore.PARAMKEY_OPRTYPE, ConstantCore.OPRTYPE_UPDATE);
		model.addObject("svgFolder", svgFolder);
		return model;
	}
	@RequestMapping(value = "upload/{svgFolder}", method = RequestMethod.POST)
	public ModelAndView upload(@PathVariable String svgFolder,
			@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		logger.info("upload:" + svgFolder);
		ModelAndView model = new ModelAndView("rtv/svgFileUploaded");
		Map<String,Object> uploadeds = null;
		
		
		if (file != null && !file.isEmpty()) {
			InputStreamReader in = null;
			try {
				String filename = file.getOriginalFilename();
				if (filename.endsWith(".svg")) {
					byte[] fileData = file.getBytes();
					svgFileService.saveSvgFile(svgFolder, filename, getUsernameFromHttpSession(request.getSession()), fileData);
					uploadeds = new HashMap<String,Object>();  
					String iconUrl = svgFolder + File.separator + filename;
					uploadeds.put(filename,iconUrl);  
				} else if (filename.endsWith(".zip")) {
					uploadeds = svgFileService.unZip2SvgFolder(file.getInputStream(), svgFolder);

				} else {
					throw new JmeException("上传的实时画面文件格式不正确，请上传svg或zip文件");
				}
			} catch (JmeException e) {
				logger.error("上传模型json文件出现异常：{}", ExceptionMsgUtil.getMsg(e));
				throw e;
			} catch (Exception e) {
				logger.error("上传模型json文件出现异常：{}", ExceptionMsgUtil.getMsg(e));
				throw new JmeException("上传模型json文件出现异常,请联系管理员");
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		} else {
			logger.error("上传的json文件为空");
			throw new JmeException("上传模型json文件出现异常,请联系管理员");
		}

		
		model.addObject("svgFolder", svgFolder);
		model.addObject("size", uploadeds.size());
		model.addObject("uploadeds", uploadeds);
		return model;
	}
	
	/**
	 * 取用户名
	 * @return 没有取到会返回null
	 */
	private String getUsername() {
		String username = null;
		Subject subject = SecurityUtils.getSubject();
		if (subject != null) {
			username = (String) subject.getPrincipal();
			if (StringUtils.isEmpty(username)) {
				SysUser user = (SysUser) subject.getSession().getAttribute("user");
				
				username = user != null ? user.getUserName() : null;
			}
		}
		
		return username;
	}

  /**
   * 从httpsession中获取user信息
   * 
   * @param session
   * @return
   */
  private String getUsernameFromHttpSession(HttpSession session) {
    String username = null;
    SysUser user = (SysUser) session.getAttribute("user");
    username = user != null ? user.getUserName() : null;
    return username;
  }
}