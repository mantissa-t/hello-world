package com.hnjme.controller.workflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.editor.constants.EditorJsonConstants;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.persistence.entity.ModelEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hnjme.core.enumeration.DataRespEnum;
import com.hnjme.core.util.base.DataResp;
import com.hnjme.core.util.constant.ConstantAppCode;
import com.hnjme.core.util.constant.ConstantSystem;
import com.hnjme.core.util.constant.ConstantWorkFlow;
import com.hnjme.core.util.exception.ExceptionMsgUtil;
import com.hnjme.core.util.exception.JmeException;
import com.hnjme.core.util.filter.BasePathFilter;
import com.hnjme.core.util.pagequery.DataGridPage;
import com.hnjme.core.util.pagequery.PageData;
import com.hnjme.core.util.pagequery.PageQuery;
import com.hnjme.custom.workflow.converter.CustomBpmnXMLConverter;
import com.hnjme.pojo.system.Company;
import com.hnjme.pojo.system.SysApp;
import com.hnjme.pojo.system.SysDicData;
import com.hnjme.service.system.CompanyService;
import com.hnjme.service.system.SysAppService;
import com.hnjme.service.system.SysDicDataService;
import com.hnjme.service.workflow.ActivitiService;

/**
 * 流程模型控制器
 *
 * @author jiaqi
 */
@Controller
@RequestMapping(value = "/model")
public class ModelController {

  protected Logger logger = LoggerFactory.getLogger(ModelController.class);

  /**
   * activiti模型库service接口
   */
  @Autowired
  RepositoryService repositoryService;

  /**
   * 系统应用service接口
   */
  @Autowired
  private SysAppService sysAppService;

  /**
   * 数据字典类型管理业务逻辑层接口
   */
  @Autowired
  private SysDicDataService sysDicDataService;

  /**
   * 公司基础信息业务逻辑处理层接口
   */
  @Autowired
  private CompanyService companyService;

  /**
   * activiti引擎相关业务层接口
   */
  @Autowired
  private ActivitiService activitiService;

  /**
   * 跳转模型管理页面 (模型工作区)
   */
  @RequestMapping(value = "/toManage")
  public ModelAndView listModel() {
    ModelAndView model = new ModelAndView("workflow/modelManage");
    List<SysApp> sysApps = sysAppService.query(new SysApp());
    model.addObject("sysApps", sysApps);
    List<SysDicData> compTypeList =
        sysDicDataService.findByDicCode(ConstantSystem.DIC_TYPE_COMPANY);
    model.addObject("compTypeList", compTypeList);
    return model;

  }

  /**
   * 模型列表分页数据
   */
  @RequestMapping(value = "/pageQuery")
  @ResponseBody
  public Object pageQueryModel(PageQuery pageQuery) {
    PageData<Model> result = activitiService.pageQueryModel(pageQuery);
    // 将查询结果转换成grid要求的数据格式
    DataGridPage<Model> resultList = DataGridPage.create(result);
    return resultList;
  }

  /**
   * 导出model的xml文件（含有业务意见模型）
   *
   * @author jiaqi
   * @param modelId 模型ID
   * @param response
   */
  @RequestMapping(value = "exportXml/{modelId}")
  public void exportXml(@PathVariable("modelId") String modelId, HttpServletResponse response) {
    try {
      Model modelData = repositoryService.getModel(modelId);
      BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
      JsonNode editorNode =
          new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
      ObjectMapper mapper = new ObjectMapper();
      // 从JsonNode数据中拿到 业务意见模型 的json字符串
      JsonNode node = editorNode.get(ConstantWorkFlow.KEY_TASKOPINIONMODELS);
      String taskopinionmodels = mapper.writeValueAsString(node);
      // 这里要用自定义的CustomBpmnXMLConverter，并把taskopinionmodels传进去
      CustomBpmnXMLConverter xmlConverter = new CustomBpmnXMLConverter();
      xmlConverter.setTaskopinionmodels(taskopinionmodels);
      // bpmnModel 中没有业务意见模型的数据，BpmnModel中没提供扩展字段
      BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
      // 用自定义的xml解析器解析得到含有业务意见模型的byte[]
      byte[] bpmnBytes = xmlConverter.convertToXML(bpmnModel);
      // activiti工作流的模型导出时遇到一个问题，平均以10kb大小为界限，
      // 10kb以内的能够正常导出，超过10kb的浏览器会直接在页面预览，不能正常导出
      // response存在一个分块输出的原理 一旦文件过大就会将文件分成一块一块的字节流输出
      // 这里设置 setBufferSize 稍微大点
      response.setBufferSize(1000000);
      ByteArrayInputStream in = new ByteArrayInputStream(bpmnBytes);
      IOUtils.copy(in, response.getOutputStream());
      String filename = bpmnModel.getMainProcess().getId() + ".bpmn20.xml";
      response.setHeader("Content-Disposition", "attachment; filename=" + filename);
      response.flushBuffer();
    } catch (Exception e) {
      logger.error("导出model的xml文件失败：modelId={},异常{}", modelId, e);
      e.printStackTrace();
    }
  }

  /**
   * 导出model的json文件
   *
   * @author jiaqi
   * @param modelId 模型ID
   * @param response
   */
  @RequestMapping(value = "exportJson/{modelId}")
  public void exportJson(@PathVariable("modelId") String modelId, HttpServletResponse response) {
    try {
      Model modelData = repositoryService.getModel(modelId);

      BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
      JsonNode editorNode =
          new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
      BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
      ObjectMapper mapper = new ObjectMapper();
      byte[] bpmnBytes = mapper.writeValueAsBytes(editorNode);
      // activiti工作流的模型导出时遇到一个问题，平均以10kb大小为界限，
      // 10kb以内的能够正常导出，超过10kb的浏览器会直接在页面预览，不能正常导出
      // response存在一个分块输出的原理 一旦文件过大就会将文件分成一块一块的字节流输出
      // 这里设置 setBufferSize 稍微大点
      response.setBufferSize(1000000);
      ByteArrayInputStream in = new ByteArrayInputStream(bpmnBytes);
      IOUtils.copy(in, response.getOutputStream());

      String filename = "process.json";

      // wangyong 2017-2-26 用流程Key做为导出名称
      String key = modelData.getKey();
      if (key != null && key.length() > 0) {
        filename = key + ".json";
      } else {
        Process mainProcess = bpmnModel.getMainProcess();

        // 当流程不为空,且流程id不为空时,设置导出文件名为流程id
        if (mainProcess != null && StringUtils.isEmpty(mainProcess.getId())) {
          filename = mainProcess.getId() + ".json";
        }

      }

      response.setHeader("Content-Disposition", "attachment; filename=" + filename);
      response.flushBuffer();
    } catch (Exception e) {
      logger.error("导出model的xml文件失败：modelId={},异常{}", modelId, ExceptionMsgUtil.getMsg(e));
      throw new JmeException("导出model的xml文件出现异常");
    }
  }

  /**
   * 创建模型
   */
  @RequestMapping(value = "/create")
  public void create(@RequestParam("name") String name, @RequestParam("key") String key,
      @RequestParam("description") String description, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      ObjectNode editorNode = objectMapper.createObjectNode();
      editorNode.put("id", "canvas");
      editorNode.put("resourceId", "canvas");
      ObjectNode stencilSetNode = objectMapper.createObjectNode();
      stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
      editorNode.set("stencilset", stencilSetNode);
      Model modelData = repositoryService.newModel();

      ObjectNode propertiesObjectNode = new ObjectMapper().createObjectNode();
      propertiesObjectNode.put("process_id", key);
      propertiesObjectNode.put("name", name);

      editorNode.set(EditorJsonConstants.EDITOR_SHAPE_PROPERTIES, propertiesObjectNode);

      ObjectNode modelObjectNode = objectMapper.createObjectNode();
      modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
      modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
      description = StringUtils.defaultString(description);
      modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
      modelData.setMetaInfo(modelObjectNode.toString());
      modelData.setName(name);
      modelData.setKey(StringUtils.defaultString(key));
      // 如果发布一次版本会加一次,修改不会改变这个版本号
      modelData.setVersion(0);

      repositoryService.saveModel(modelData);
      repositoryService.addModelEditorSource(modelData.getId(),
          editorNode.toString().getBytes("utf-8"));

      String workflowAppUrl =
          BasePathFilter.getBasePath(request, true) + "/" + ConstantAppCode.APPCODE_WORKFLOW;

      response.sendRedirect(workflowAppUrl + "/modeler.html?modelId=" + modelData.getId());
    } catch (Exception e) {
      logger.error("创建模型失败：{}", ExceptionMsgUtil.getMsg(e));
    }
  }

  /**
   * 根据Model部署流程
   */
  @RequestMapping(value = "/deploy/{modelId}")
  @ResponseBody
  public Object deploy(@PathVariable("modelId") String modelId) {
    DataResp dataResp = new DataResp();
    try {
      Deployment deployment = activitiService.deployModel(modelId);

      dataResp.setDataRespEnum(DataRespEnum.SUCCESS);
      dataResp.setMsg("部署成功，部署ID=" + deployment.getId());
    } catch (Exception e) {
      logger.error("根据模型部署流程失败：modelId={},异常{}", modelId, ExceptionMsgUtil.getMsg(e));
      dataResp.setDataRespEnum(DataRespEnum.JME);
      dataResp.setMsg("根据模型部署流程失败：" + ExceptionMsgUtil.getMsg(e));
    }
    return dataResp;
  }


  /**
   * 根据ModelId删除Model
   */
  @RequestMapping(value = "/delete/{modelId}")
  @ResponseBody
  public Object delete(@PathVariable("modelId") String modelId) {
    DataResp dataResp = new DataResp();
    try {
      activitiService.deleteModel(modelId);

      dataResp.setDataRespEnum(DataRespEnum.SUCCESS);
      dataResp.setMsg("删除模型成功，modelID=" + modelId);
    } catch (Exception e) {
      logger.error("删除模型失败：modelId={},异常信息{}", modelId, ExceptionMsgUtil.getMsg(e));
      dataResp.setDataRespEnum(DataRespEnum.JME);
      dataResp.setMsg("删除模型失败：modelId=" + modelId);
    }
    return dataResp;
  }

  /**
   * 跳转到model绑定公司页面 TODO 换system的公司树
   *
   * @author jiaqi
   * @return model绑定公司页面
   */
  @RequestMapping(value = "toBindCompany/{modelId}", method = RequestMethod.GET)
  public ModelAndView toBindCompany(@PathVariable("modelId") String modelId) {
    ModelAndView view = new ModelAndView("workflow/publicPage/companyList");

    Model model = repositoryService.getModel(modelId);

    String oldCompanyName = "";
    try {
      if (StringUtils.isNotEmpty(model.getTenantId())) {
        String oldCompanyCode = model.getTenantId();
        Company company = companyService.findByCode(oldCompanyCode);
        oldCompanyName = company.getCompanyName() + "(" + oldCompanyCode + ")";
        view.addObject("oldCompanyCode", oldCompanyCode);
      } else {
        oldCompanyName = "未指定";
      }
    } catch (Exception e) {
      logger.error("获取模型所属公司信息失败：modelId={},异常信息{}", modelId, ExceptionMsgUtil.getMsg(e));
    }

    view.addObject("oldCompanyName", oldCompanyName);
    return view;
  }

  /**
   * 修改model的tenantId
   *
   * @author jiaqi
   * @param modelId 模型ID
   * @param companyCode 公司编号
   * @return
   */
  @RequestMapping(value = "/saveModelTenant/{modelId}/{companyCode}")
  @ResponseBody
  public Object saveModelTenant(@PathVariable("modelId") String modelId,
      @PathVariable("companyCode") String companyCode) {
    DataResp dataResp = new DataResp();
    try {
      activitiService.saveModelTenant(modelId, companyCode);
      dataResp.setDataRespEnum(DataRespEnum.SUCCESS);
      dataResp.setMsg("模型绑定公司成功，modelId=" + modelId);
    } catch (Exception e) {
      logger.error("模型绑定公司失败：modelId={},companyId={},异常信息{}", modelId, companyCode,
          ExceptionMsgUtil.getMsg(e));
      dataResp.setDataRespEnum(DataRespEnum.JME);
      dataResp.setMsg("模型绑定公司失败：modelId=" + modelId);
    }
    return dataResp;
  }

  /**
   * 跳转到复制模型页面
   */
  @RequestMapping(value = "/toCopy/{copyModelId}")
  public ModelAndView toCopy(@PathVariable("copyModelId") String copyModelId) {
    ModelAndView view = new ModelAndView("/workflow/modelCopyDlg");
    view.addObject("copyModelId", copyModelId);
    return view;
  }

  /**
   * 复制模型
   *
   * @author jiaqi
   * @param copyModelId 要复制的模型ID
   * @return
   */
  @RequestMapping(value = "/copy/{copyModelId}")
  @ResponseBody
  public Object copy(@PathVariable("copyModelId") String copyModelId,
      @RequestBody ModelEntity model) {
    DataResp dataResp = new DataResp();
    try {
      ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
      String name = model.getName();
      String key = model.getKey();
      // wangyong 2017-2-24不可以有重复的Key
      long count = activitiService.countModelByKey(key);
      if (count > 0) {
        throw new JmeException("请重新设置复制后的KEY 已存在: [" + key + "] 这个模型.");
      }
      Model newModelData = repositoryService.newModel();
      modelObjectNode.put("name", name);
      modelObjectNode.put("key", key);
      modelObjectNode.put("description", model.getMetaInfo());

      newModelData.setMetaInfo(modelObjectNode.toString());
      newModelData.setName(name);

      Model copyModel = repositoryService.getModel(copyModelId);
      newModelData.setCategory(copyModel.getCategory());
      newModelData.setTenantId(copyModel.getTenantId());
      newModelData.setKey(key);
      // 读取原来的Json定义,修改一下里面的process_id 和 name
      byte[] datas = repositoryService.getModelEditorSource(copyModelId);
      JsonNode editorNode = new ObjectMapper().readTree(datas);

      // wangyong 2017-2-24不可以有重复的Key 将名称和key保存到模型定义的Json中去
      if (editorNode.get(EditorJsonConstants.EDITOR_SHAPE_PROPERTIES) != null) {
        JsonNode propertiesNode = editorNode.get(EditorJsonConstants.EDITOR_SHAPE_PROPERTIES);
        ((ObjectNode) propertiesNode).put("process_id", key);
        ((ObjectNode) propertiesNode).put("name", name);
      }
      logger.error(editorNode.toString());
      repositoryService.saveModel(newModelData);
      repositoryService.addModelEditorSource(newModelData.getId(),
          editorNode.toString().getBytes("utf-8"));
      repositoryService.addModelEditorSourceExtra(newModelData.getId(),
          repositoryService.getModelEditorSourceExtra(copyModelId));
      dataResp.setDataRespEnum(DataRespEnum.SUCCESS);
      dataResp.setMsg("复制模型成功");
    } catch (JmeException e) {
      logger.error("复制模型失败出现异常：{}", ExceptionMsgUtil.getMsg(e));
      throw e;
    } catch (Exception e) {
      logger.error("复制模型失败,异常信息{}", ExceptionMsgUtil.getMsg(e));
      dataResp.setDataRespEnum(DataRespEnum.JME);
      dataResp.setMsg("复制模型失败");
    }
    return dataResp;
  }

  /**
   * 跳转到导入模型页面
   */
  @RequestMapping(value = "/toUploadModel")
  public ModelAndView toUploadModel(
      @RequestParam(value = "modalID", required = false) String modalID) {
    ModelAndView rtv = new ModelAndView("/workflow/modelUploadDlg");
    rtv.addObject("modalID", modalID);
    return rtv;
  }

  /**
   * 导入模型json
   * <p>
   * 模型的导入改用json文件，因为用户任务节点生成的xml中没有业务意见模型的内容，
   * <p>
   * 导入xml后编辑的时候业务意见模型无法回显
   *
   * @author jiaqi
   * @param file 上传的json文件
   * @param request
   * @return
   * @throws IOException
   */
  @RequestMapping(value = "uploadModelJson", method = RequestMethod.POST)
  public void uploadModelJson(@RequestParam(value = "modalID", required = false) String modalID,
      @RequestParam(value = "file", required = false) MultipartFile file,
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (file != null && !file.isEmpty()) {
      InputStreamReader in = null;
      try {
        String filename = file.getOriginalFilename();
        if (filename.endsWith(".json")) {
          byte[] fileData = file.getBytes();
          JsonNode editorNode = new ObjectMapper().readTree(fileData);
          activitiService.uploadModelJson(modalID, editorNode);
        } else {
          throw new JmeException("上传模型json文件格式不正确，请上传json格式文件");
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
    // 上传提交后，当前页重定向回模型列表页
    response.sendRedirect(BasePathFilter.getBasePath(request, false) + "model/toManage");
  }

}
