package com.jme.dynamicForm.businessForm.manager.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import com.jme.common.Constant;
import com.jme.common.bean.StatisticsInfo;
import com.jme.common.constant.DicConstant;
import com.jme.common.web.framework.base.ApplicationHolder;
import com.jme.dynamicForm.businessForm.manager.DynamicBusinessManager;
import com.jme.dynamicForm.businessForm.mapper.BusinessFormMapper;
import com.jme.dynamicForm.businessForm.pojo.DynamicBusinessObject;
import com.jme.dynamicForm.businessForm.pojo.EntryConfig;
import com.jme.dynamicForm.constant.CommonWorkflowConstant;
import com.jme.dynamicForm.constant.FormItemConstant;
import com.jme.dynamicForm.constant.FormTemplateConstant;
import com.jme.dynamicForm.formItem.pojo.FormItem;
import com.jme.dynamicForm.formItem.service.FormItemService;
import com.jme.dynamicForm.formTemplate.pojo.FormTemplate;
import com.jme.dynamicForm.formTemplate.service.FormTemplateService;
import com.jme.dynamicForm.reportCrossItem.pojo.ReportCrossItem;
import com.jme.dynamicForm.reportCrossItem.service.ReportCrossItemService;
import com.jme.dynamicForm.reportItemCalculator.service.ReportItemCalculatorService;
import com.jme.dynamicForm.tableOperate.service.TableOperateService;
import com.jme.dynamicForm.util.DFExcelUtil;
import com.jme.exception.JmeException;
import com.jme.gwt.client.help.GWTClientUtil;
import com.jme.gwt.common.ConstantGWT;
import com.jme.help.CommonTools;
import com.jme.system.dic.manager.DicDataManager;
import com.jme.system.dic.pojo.DicData;
import com.jme.system.user.pojo.User;

/**
 * 动态表单 业务表 （表数据增删改查操作） 处理层接口实现类
 * 
 * <p>
 * 增删改操作建立在隐含unique字段id的前提上，创建table时添加id列，本实现类负责维护id列
 * </p>
 * 
 * @author wangyong 2015-10-14
 * @see
 * @since 1.0
 */
public class DynamicBusinessManagerImpl implements DynamicBusinessManager {
  /**
   * 动态表单具体业务 持久层
   */
  @Autowired
  private BusinessFormMapper businessFormMapper;

  /**
   * 动态表单模块 表单模板对应数据库table的数据库操作 处理层接口
   */
  @Autowired
  private TableOperateService tableOperateService;

  /**
   * 动态表单项业务层接口
   */
  @Autowired
  private FormItemService formItemService;

  // /**
  // * 工作流任务定义服务接口
  // */
  // @Autowired
  // private TaskDefineService taskDefineService;
  //
  // /**
  // * 模板表单项-流程节点关联关系业务逻辑处理层接口
  // */
  // @Autowired
  // private FormItemBoundFlowService formItemBoundFlowService;
  //
  // /**
  // * 指标数据表
  // */
  // @Autowired
  // private IndicatorDataTableManager indicatorDataTableManager;

  /**
   * 横表项关联指标
   */
  @Autowired
  private ReportCrossItemService reportCrossItemService;

  /**
   * 表单模板信息业务处理层接口
   */
  @Autowired
  private FormTemplateService formTemplateService;

  /**
   * 报表指标结算接口
   */
  @Autowired
  private ReportItemCalculatorService reportItemCalculatorService;

  /**
   * 数据字典数据管理业务逻辑层接口
   */
  @Autowired
  private DicDataManager dicDataManager;

  /**
   * 项目临时文件夹
   */
  @Value("${tempFilePath}")
  private String tempFilePath;

  /**
   * 条件查询多条记录
   * 
   * @param dynamicBusinessObject 表单模板
   * @param condition 查询条件map （字段为key 条件值为value）
   * @return 查询结果集合
   */
  @Override
  public List<DynamicBusinessObject> query(int startRowNum, int endRowNum,
      DynamicBusinessObject dynamicBusinessObject) {
    if (dynamicBusinessObject == null
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.query参数为空");
    }
    Map<String, String> condition = dynamicBusinessObject.getDataMap();

    List<DynamicBusinessObject> returnList = new LinkedList<DynamicBusinessObject>();
    List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
    // 判断是否有id
    String id = CommonTools.isNull(condition.get("id")) ? condition.get("ID") : condition.get("id");
    // 有id以id为条件查询
    if (!CommonTools.isNull(id)) {
      returnList.add(find(dynamicBusinessObject, id));
    }
    // 其他条件查询
    else {
      Map<String, Object> params = new HashMap<String, Object>();
      // 提供表名和字段list作为参数进行查询
      String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
      // String[] fields = new String[condition.size()];
      // condition.keySet().toArray(fields);
      List<String> fields = tableOperateService.getFields(tableName);
      params.put("tableName", tableName);
      params.put("fields", fields);

      params = parseListParams(tableName, condition, params);
      // params.put("fieldValueMap", condition);

      // 只有endRowNum > 0于会查询 用于分页
      if (endRowNum > startRowNum && startRowNum >= 0 && endRowNum > 0) {
        params.put("maxLine", endRowNum);
        params.put("minLine", startRowNum);
        params.put("paging", true);
        // SystemContext.setRowBounds(new RowBounds(startRowNum,
        // endRowNum));
      }
      FormTemplate formTemplate = findFormTemplate(dynamicBusinessObject.getTemplateId(), null);
      String sortString = dynamicBusinessObject.getSortString();
      if (!CommonTools.isNull(sortString)) {
        params.put("defaultSortStr", sortString);
        params.put("defaultSort", true);
      } else {
        String defaultSort = formTemplate.getDefaultSort();
        if (!CommonTools.isNull(defaultSort)) {
          params.put("defaultSortStr", defaultSort);
          params.put("defaultSort", true);
        }
      }

      resultList = businessFormMapper.list(params);

      map2DynamicBusinessObject(resultList, returnList, dynamicBusinessObject, formTemplate);

    }
    return returnList;
  }

  /**
   * 将数据库的查询结果转为DynamicBusinessObject对象
   * 
   * @param resultList 数据库查询结果
   * @param returnList 返回的DynamicBusinessObject对象集
   * @param dynamicBusinessObject 查询条件 也是新对象的模板
   * @param formTemplate 动态表单定义
   */
  private void map2DynamicBusinessObject(List<Map<String, Object>> resultList,
      List<DynamicBusinessObject> returnList, DynamicBusinessObject dynamicBusinessObject,
      FormTemplate formTemplate) {
    if (resultList != null) {
      List<FormItem> formItems = formTemplate != null ? formTemplate.getFormItems() : null;
      for (Map<String, Object> map : resultList) {
        DynamicBusinessObject newCopy = dynamicBusinessObject.newCopy();
        for (Map.Entry<String, Object> e : map.entrySet()) {
          String key = e.getKey().toString();
          String value = e.getValue().toString();
          newCopy.put(key, value);
          String fieldName = findEqualsIgnoreCase(formItems, key);
          // 如果不区分大小写是相等的,区分大小写不相同,则添加一个值
          if (fieldName != null && fieldName.equals(key) == false) {
            newCopy.put(fieldName, value);
          }
        }
        // newCopy.putAll(map);

        returnList.add(newCopy);
      }
    }
  }

  /**
   * 找不区分大小写相等的
   * 
   * @param formItems 表单项定义
   * @param formItemName 字段名
   * @return 找到了返回,没有找到返回 null
   */
  private String findEqualsIgnoreCase(List<FormItem> formItems, String formItemName) {
    if (formItems == null || formItemName == null)
      return null;
    for (FormItem formItem : formItems) {
      if (formItem == null) {
        continue;
      }
      String fieldName = formItem.getFieldName();
      if (formItemName.equalsIgnoreCase(fieldName)) {
        return fieldName;
      }
    }
    return null;
  }

  /**
   * 单条查询查询表数据
   * 
   * @param dynamicBusinessObject 表单模板
   * @param condition 条件
   * @return 单条数据map
   */
  @Override
  public DynamicBusinessObject find(DynamicBusinessObject dynamicBusinessObject) {
    List<DynamicBusinessObject> list = this.query(0, 0, dynamicBusinessObject);
    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() > 1) {
      throw new JmeException("存在重复数据");
    }
    return null;
  }

  /**
   * 查询单条表数据
   * 
   * @param dynamicBusinessObject 表单模板
   * @param bussinessId id
   * @return 查询单条结果数据map
   */
  @Override
  public DynamicBusinessObject find(DynamicBusinessObject dynamicBusinessObject,
      String bussinessId) {
    if (dynamicBusinessObject == null || CommonTools.isNull(bussinessId)
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.find参数为空");
    }
    List<DynamicBusinessObject> returnList = new LinkedList<DynamicBusinessObject>();
    List<Map<String, Object>> resultList = null;
    Map<String, Object> params = new HashMap<String, Object>();
    // 提供表名、字段list和id作为参数进行单条查询
    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
    List<String> fields = tableOperateService.getFields(tableName);
    params.put("tableName", tableName);
    params.put("fields", fields);
    params.put("id", bussinessId);
    resultList = businessFormMapper.query(params);
    if (resultList.size() > 1) {
      throw new JmeException("单条查询返回多条结果");
    }

    FormTemplate formTemplate = findFormTemplate(dynamicBusinessObject.getTemplateId(), null);
    map2DynamicBusinessObject(resultList, returnList, dynamicBusinessObject, formTemplate);

    return returnList.get(0);
  }

  /**
   * 增
   * 
   * @param dynamicBusinessObject 表单模板
   * @param data 数据map (若需保存作者信息，dataMap中需要authorId，companyCode)
   * @return 新增的数据map (key都为大写)
   */
  @Transactional
  @Override
  public DynamicBusinessObject insert(DynamicBusinessObject dynamicBusinessObject) {
    return this.insert(dynamicBusinessObject, true);
  }

  /**
   * 增
   * 
   * @param dynamicBusinessObject 表单模板
   * @param data 数据map (若需保存作者信息，map中需要authorId，companyCode)
   * @param isUptIndicator 是否更新相关指标值
   * @return 新增的数据map (key都为大写)
   */
  @Transactional
  @Override
  public DynamicBusinessObject insert(DynamicBusinessObject dynamicBusinessObject,
      boolean isUptIndicator) {

    if (dynamicBusinessObject == null || CommonTools.isNull(dynamicBusinessObject.getTemplateId())
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.insert参数为空");
    }
    Map<String, String> data = dynamicBusinessObject.getDataMap();

    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();

    // 2015-12-3 取不区分大小写的公司和报表时间 如果有这两个值,则需要判断是不有重复
    String companyCode = getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    String reportTime = getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME);
    /**
     * 增加校验 如果缺少这两个参数则抛出异常 <br/>
     * update by tanquanfang 2016-02-02
     */
    if (CommonTools.isNull(companyCode)) {
      throw new JmeException("缺少公司编号参数");
    }
    if (CommonTools.isNull(reportTime)) {
      throw new JmeException("缺少日期参数");
    }
    /**
     * 校验reporttime的格式
     */
    if (!reportTime.matches("[0-9]{4}-[0-1]{1}[0-9]{1}-[0-3]{1}[0-9]{1}")) {
      throw new JmeException("日期格式不正确，请使用类似如下格式：2015-01-01");
    }
    quertAgoValue(dynamicBusinessObject);

    Map<String, Object> params = new HashMap<String, Object>();
    List<String> fields = new ArrayList<String>();
    Map<String, String> fieldValueMap = new HashMap<String, String>();
    // // 若模板绑定了流程，获取流程首节点id
    // String workFlowId = dynamicBusinessObject.getWorkFlowId();
    // String curFlowNode = null;
    // if (!CommonTools.isNull(workFlowId))
    // {
    // TaskDefine condition = new TaskDefine();
    // condition.setProcessDefineId(workFlowId);
    // condition.setType(EnumTaskType.StartType.getType());
    // TaskDefine taskDefine = taskDefineService.find(condition);
    // curFlowNode = taskDefine.getTaskDefineId();
    // }

    // 根据表单项设置，对data进行数据的必填项校验，若顺利通过校验，将字段及值 存入fields和fieldValueMap中
    getFieldData(data, fields, fieldValueMap, dynamicBusinessObject.getTemplateId(), null);
    // 进行主键值的校验 模板主键不为id字段即自定义主键的情况下才需要校验
    validPk(dynamicBusinessObject, fieldValueMap);
    // 获取下一个序列值，赋值给id
    String seqNextVal = tableOperateService.getSeqNextVal(tableName);
    fields.add("id");
    fieldValueMap.put("id", CommonTools.format(new Date(), "yyyyMMddHHmmss") + seqNextVal);

    if (!fields.contains(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE)) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    }
    fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE, companyCode);
    // 防止数据遗留问题，先判定field中是否已有REPORTTIME
    if (!fields.contains(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME.toUpperCase())) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME.toUpperCase());
    }
    fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME.toUpperCase(), reportTime);
    /**
     * update by tanquanfang 2016-02-02 <br/>
     * 如果data中没有作者信息 session中获取
     */
    String authorId = data.get(CommonWorkflowConstant.COLUMN_AUTHORID);
    if (CommonTools.isNull(authorId)) {
      User user = (User) ApplicationHolder.getSessionMapValue(Constant.SESSION_USER);
      fields.add(CommonWorkflowConstant.COLUMN_AUTHORID);
      fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHORID, user.getId());
    }

    // 报表模块新增字段
    if (!CommonTools.isNull(data.get(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME))) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME);
      fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME,
          data.get(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME));
    }
    String creatTime = data.get(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME);
    if (CommonTools.isNull(creatTime)) {
      creatTime = CommonTools.format(new Date(), ConstantGWT.TIMEFORMAT_SHORTDATETIME);
    }
    fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME);
    fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME, creatTime);

    // 表中有的字段
    List<String> tableFields = tableOperateService.getFields(tableName);
    List<String> thfields = new ArrayList<String>();
    for (String f : fields) {
      if (tableHasField(f, tableFields)) {
        thfields.add(f);
      }
    }

    params.put("tableName", tableName);
    params.put("fields", thfields);
    params.put("fieldValueMap", fieldValueMap);
    businessFormMapper.insert(params);

    /**
     * update by tanquanfang 2016-02-01 将更新指标值的方法临时加入到动态表单的后台代码中，年后再进行其他修改
     */
    if (isUptIndicator) {
      String templateId = dynamicBusinessObject.getTemplateId();
      FormTemplate formTemplate = formTemplateService.findById(templateId);
      reportItemCalculatorService.updateDataToIndicator(formTemplate, data, companyCode);
    }
    return dynamicBusinessObject;

  }

  /**
   * 取不区分key大小写的值
   * 
   * @param data map
   * @param key 查询条件
   * @return 返回值
   */
  private String getIgnoreCaseValue(Map<String, String> data, String key) {
    if (data == null || key == null) {
      return null;
    }
    for (Map.Entry<String, String> e : data.entrySet()) {
      if (key.equalsIgnoreCase(e.getKey())) {
        return e.getValue();
      }
    }
    return null;
  }

  /**
   * 改(暂时只有根据id进行修改)
   * 
   * @param dynamicBusinessObject 表单模板
   * @param data 数据map
   * @return 改后的数据map (key都为大写)
   */
  @Transactional
  @Override
  public DynamicBusinessObject update(DynamicBusinessObject dynamicBusinessObject) {
    if (dynamicBusinessObject == null || CommonTools.isNull(dynamicBusinessObject.getTemplateId())
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.update参数为空");
    }
    Map<String, String> data = dynamicBusinessObject.getDataMap();
    // 判断是否有id
    String id = dynamicBusinessObject.getId();// CommonTools.isNull(data.get("id"))
                                              // ? data.get("ID") :
                                              // data.get("id");
    if (CommonTools.isNull(id)) {
      throw new JmeException("businessFormService.update缺少id参数");
    }

    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
    Map<String, Object> params = new HashMap<String, Object>();
    List<String> fields = new ArrayList<String>();
    Map<String, String> fieldValueMap = new HashMap<String, String>();
    // 先校验该id下的数据是否存在
    params.put("tableName", tableName);
    params.put("id", id);
    fieldValueMap.put("id", id);

    DynamicBusinessObject oldDynamicBusinessObject = find(dynamicBusinessObject, id);
    if (oldDynamicBusinessObject == null) {
      throw new JmeException("操作失败，要修改的数据不存在或已被删除");
    }

    // 2015-12-3 取不区分大小写的公司和报表时间 如果有这两个值,则需要判断是不有重复

    String companyCode = getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    // 如果前端没有传公司代码过来,则看一下原来的数据里有没有
    if (CommonTools.isNull(companyCode)) {
      companyCode = getIgnoreCaseValue(oldDynamicBusinessObject.getDataMap(),
          CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    }
    String reportTime = getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME);

    /**
     * 增加校验 如果缺少这两个参数则抛出异常 <br/>
     * update by tanquanfang 2016-02-02
     */
    if (CommonTools.isNull(companyCode) || CommonTools.isNull(reportTime)) {
      throw new JmeException("缺少日期及公司参数");
    }
    /**
     * 校验reporttime的格式
     */
    if (!reportTime.matches("[0-9]{4}-[0-1]{1}[0-9]{1}-[0-3]{1}[0-9]{1}")) {
      throw new JmeException("日期格式不正确，请使用类似如下格式：2015-01-01");
    }
    quertAgoValue(dynamicBusinessObject);

    // // 获取流程信息，存入隐含字段taskInsId及Name
    // String taskInsId =
    // CommonTools.isNull(data.get(CommonWorkflowConstant.COLUMN_TASKINSID))
    // ? data
    // .get(CommonWorkflowConstant.COLUMN_TASKINSID.toUpperCase()) :
    // data.get(CommonWorkflowConstant.COLUMN_TASKINSID);
    // String taskInsName =
    // CommonTools.isNull(data.get(CommonWorkflowConstant.COLUMN_TASKINSNAME))
    // ? data
    // .get(CommonWorkflowConstant.COLUMN_TASKINSNAME.toUpperCase()) : data
    // .get(CommonWorkflowConstant.COLUMN_TASKINSNAME);
    // String curFlowNode = null;
    // if (!CommonTools.isNull(taskInsId) &&
    // !CommonTools.isNull(taskInsName))
    // {
    // fields.add(CommonWorkflowConstant.COLUMN_TASKINSID);
    // fields.add(CommonWorkflowConstant.COLUMN_TASKINSNAME);
    // fieldValueMap.put(CommonWorkflowConstant.COLUMN_TASKINSID,
    // taskInsId);
    // fieldValueMap.put(CommonWorkflowConstant.COLUMN_TASKINSNAME,
    // taskInsName);
    // // 若模板绑定了流程 获取当前流程节点id
    // curFlowNode = taskInsId;
    // }
    // 根据表单项设置，对data进行数据的必填项校验，若顺利通过校验，将字段及值 存入fields和fieldValueMap中
    getFieldData(data, fields, fieldValueMap, dynamicBusinessObject.getTemplateId(), null);
    // 进行主键值的校验
    validPk(dynamicBusinessObject, fieldValueMap);

    if (!fields.contains(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE)) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    }
    fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE, companyCode);
    // 防止数据遗留问题，先判定field中是否已有REPORTTIME
    if (!fields.contains(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME.toUpperCase())) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME.toUpperCase());
    }
    fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME.toUpperCase(), reportTime);

    // pmis报表模块新增字段
    if (!CommonTools.isNull(data.get(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME))) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME);
      fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME,
          data.get(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYNAME));
    }
    if (!CommonTools.isNull(data.get(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME))) {
      fields.add(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME);
      fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME,
          data.get(CommonWorkflowConstant.COLUMN_AUTHOR_CREATETIME));
    }

    /**
     * update by tanquanfang 2015-12-15 为了T_LOG中有当前用户 将当前修改用户id 作为作者id
     */
    User user = (User) ApplicationHolder.getSessionMapValue(Constant.SESSION_USER);
    fields.add(CommonWorkflowConstant.COLUMN_AUTHORID);
    fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHORID, user.getId());

    // 表中有的字段
    List<String> tableFields = tableOperateService.getFields(tableName);
    List<String> thfields = new ArrayList<String>();
    for (String f : fields) {
      if (tableHasField(f, tableFields)) {
        thfields.add(f);
      }
    }

    params.put("fields", thfields);
    params.put("fieldValueMap", fieldValueMap);
    businessFormMapper.update(params);

    /**
     * update by tanquanfang 2016-02-01 将更新指标值的方法临时加入到动态表单的后台代码中，年后再进行其他修改
     */
    String templateId = dynamicBusinessObject.getTemplateId();
    FormTemplate formTemplate = formTemplateService.findById(templateId);
    reportItemCalculatorService.updateDataToIndicator(formTemplate, data, companyCode);

    return dynamicBusinessObject;
  }

  /**
   * 判断一个字段是否在表中存在 不区分大小写
   * 
   * @param field
   * @param tableFields
   * @return
   */
  private boolean tableHasField(String field, List<String> tableFields) {
    if (tableFields == null || field == null)
      return false;
    for (String tf : tableFields) {
      if (field.equalsIgnoreCase(tf)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 根据data与表单模板下的所有表单项，获取满足必填条件的对应的fields和fieldValueMap
   * 
   * @param data 前台传来的字段--key 值--value的数据Map
   * @param fields 保存所有表单项对应字段中有值的field集合
   * @param fieldValueMap 对应field和值的map
   * @param formTemplateId 当前表单模板id
   * @param curFlowNode 当前流程节点，若没有绑定流程此处为null
   */
  private void getFieldData(Map<String, String> data, List<String> fields,
      Map<String, String> fieldValueMap, String formTemplateId, String curFlowNode) {
    if (data == null || data.size() == 0 || fields == null || fieldValueMap == null
        || CommonTools.isNull(formTemplateId)) {
      throw new JmeException("businessFormService.getFieldData参数为空");
    }
    // 校验必填条件 分为有绑定流程和没绑定流程两种情况
    // 没绑定流程 直接根据表单项的isFill字段判断
    // 有绑定流程 需要根据【当前流程节点所绑定的】表单项的isFill字段判断
    List<String> boundField = null;
    // if (curFlowNode != null)
    // {
    // FormItemBoundFlow condition = new FormItemBoundFlow();
    // condition.setFormTemplateId(formTemplateId);
    // condition.setFlowNodeId(curFlowNode);
    // boundField = formItemBoundFlowService.queryBoundField(condition);
    // }
    FormItem condition = new FormItem();
    condition.setFormTemplateId(formTemplateId);
    List<FormItem> formItemList = formItemService.query(condition);
    for (FormItem e : formItemList) {
      String field = e.getFieldName();
      String fieldUpperCase = field.toUpperCase();
      // 若获取不到值，将field转为大写重新获取一次
      // 使用Object是由于数据库取出的number会转化为bigDecimal，无法直接转为String
      Object tempValue = data.get(field);
      if (tempValue == null) {
        tempValue = data.get(fieldUpperCase);
      }
      // 取不到值 表示前台data没有传递field字段
      if (null == tempValue) {
        continue;
      }
      String fieldValue = tempValue.toString();
      // 替换回车符用于页面查看显示时
      fieldValue = fieldValue.replaceAll("\\n", "<br/>");
      if (!CommonTools.isNull(fieldValue)
          && FormItemConstant.FORMITEM_TYPE_INDICATOR.equals(e.getType())
          || FormItemConstant.WIDGET_TYPE_INT.equals(e.getType())
          || FormItemConstant.WIDGET_TYPE_DECIMAL.equals(e.getType())) {
        try {
          Double.valueOf(fieldValue);
        } catch (Exception ex) {
          throw new JmeException("操作失败，" + e.getName() + ":[" + fieldValue + "]格式不正确，只能为数字");
        }
      }
      // "1"表示字段必填，若必填字段没值 抛异常
      if ((boundField == null || boundField.contains(fieldUpperCase)) && "1".equals(e.getIsFill())
          && CommonTools.isNull(fieldValue)) {
        // 如果有默认值设置默认值 否则抛异常
        String defaultValue = e.getDefaultValue();
        if (CommonTools.isNull(defaultValue)) {
          throw new JmeException("操作失败，必填字段" + e.getName() + "不能为空");
        }
        fieldValueMap.put(fieldUpperCase, defaultValue);
      } else {
        fieldValueMap.put(fieldUpperCase, fieldValue);
      }
      fields.add(fieldUpperCase);
    }
  }

  /**
   * 校验模板主键字段是否符合非空且唯一（模板主键不为id字段即自定义主键的情况下才需要校验）
   * 
   * @param dynamicBusinessObject 表单模板
   * @param fieldValueMap 要insert或update的数据字段值map
   */
  private void validPk(DynamicBusinessObject dynamicBusinessObject,
      Map<String, String> fieldValueMap) {
    if (dynamicBusinessObject == null
        || CommonTools.isNull(dynamicBusinessObject.getTablePrimaryKey())
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())
        || fieldValueMap == null) {
      throw new JmeException("businessFormService.validPk参数为空");
    }
    String tablePrimaryKey = dynamicBusinessObject.getTablePrimaryKey();
    // 当模板主键不为id字段，即为自定义主键时
    if (!"id".equals(tablePrimaryKey)) {
      Map<String, String> countConditon = new HashMap<String, String>();

      String[] pks = tablePrimaryKey.split(",");
      // 获取fieldValueMap中主键字段的值
      for (String pkCol : pks) {
        String pkValue = fieldValueMap.get(pkCol.toUpperCase());
        if (CommonTools.isNull(pkValue)) {
          throw new JmeException("操作失败，" + pkCol + "主键字段不能为空");
        }
        countConditon.put(pkCol, pkValue);
      }
      Map<String, Object> validCondition = new HashMap<String, Object>();
      validCondition.put("fieldMap", countConditon);
      validCondition.put("tableName", dynamicBusinessObject.getTemplateRelevanceTable());
      if (count(dynamicBusinessObject) > 0) {
        throw new JmeException("操作失败，存在数据与" + tablePrimaryKey + "主键字段重复");
      }
    }
  }

  /**
   * 删
   * 
   * @param dynamicBusinessObject 表单模板
   * @param data 数据map
   * @return 删除条数
   */
  @Transactional
  @Override
  public int delete(DynamicBusinessObject dynamicBusinessObject) {
    if (dynamicBusinessObject == null
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.delete参数为空");
    }

    Map<String, String> data = dynamicBusinessObject.getDataMap();

    // 判断是否有id
    String id = dynamicBusinessObject.getId();// CommonTools.isNull(data.get("id"))
                                              // ? data.get("ID") :
                                              // data.get("id");
    // 有id以id为条件删除
    if (!CommonTools.isNull(id)) {
      return delete(dynamicBusinessObject, id);
    }

    // 增强删除条件校验
    if (data == null || data.size() == 0) {
      throw new JmeException("businessFormService.delete删除条件为空");
    }

    // 如果没有id则进行其他条件的删除
    Map<String, Object> params = new HashMap<String, Object>();
    String[] fields = new String[data.size()];
    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
    data.keySet().toArray(fields);
    params.put("tableName", tableName);
    params.put("fields", fields);
    params.put("fieldValueMap", data);
    // 删除前先判断要删除的数据是否存在
    int count = businessFormMapper.count(params);
    if (count == 0) {
      throw new JmeException("要修改的数据不存在或已被删除");
    }
    businessFormMapper.delete(params);
    return count;
  }

  /**
   * 删
   * 
   * @param dynamicBusinessObject 表单模板
   * @param bussinessId id字段
   * @return 删除条数
   */
  @Transactional
  @Override
  public int delete(DynamicBusinessObject dynamicBusinessObject, String id) {
    if (dynamicBusinessObject == null || CommonTools.isNull(id)
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.delete参数为空");
    }
    Map<String, Object> params = new HashMap<String, Object>();
    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
    params.put("tableName", tableName);
    params.put("id", id);
    // 删除前先判断要删除的数据是否存在
    int count = businessFormMapper.count(params);
    if (count == 0) {
      throw new JmeException("要修改的数据不存在或已被删除");
    }
    /**
     * update by tanquanfang 2015-12-17 为了触发器能获取到删除人 将当前用户存入authorId后再删除
     */
    User curUser = (User) ApplicationHolder.getSessionMapValue(Constant.SESSION_USER);
    if (curUser != null) {
      List<String> key = new ArrayList<String>();
      key.add(CommonWorkflowConstant.COLUMN_AUTHORID);
      HashMap<String, String> fieldValueMap = new HashMap<String, String>();
      fieldValueMap.put(CommonWorkflowConstant.COLUMN_AUTHORID, curUser.getId());
      params.put("fields", key);
      params.put("fieldValueMap", fieldValueMap);
      businessFormMapper.update(params);
    }

    businessFormMapper.delete(params);
    return count;
  }

  /**
   * 统计符合条件的数据条数
   * 
   * @param dynamicBusinessObject 表单模板
   * @param condtition 统计条件map：<br/>
   *        id，统计对应id记录是否存在 <br/>
   *        map中的key对应表中字段，value对应条件值，统计符合的条数
   * @return 条数
   */
  @Override
  public int count(DynamicBusinessObject dynamicBusinessObject) {
    if (dynamicBusinessObject == null
        || CommonTools.isNull(dynamicBusinessObject.getTemplateRelevanceTable())) {
      throw new JmeException("businessFormService.count参数为空");
    }
    Map<String, String> condtition = dynamicBusinessObject.getDataMap();
    Map<String, Object> params = new HashMap<String, Object>();
    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
    params.put("tableName", tableName);
    // 判断是否有id
    String id = condtition.get("id");
    id = CommonTools.isNull(id) ? condtition.get("ID") : id;
    // 1.有id以id为条件统计--常用于判断该记录是否存在
    if (!CommonTools.isNull(id)) {
      params.put("id", id);

      return businessFormMapper.count(params);
    }
    // 2.其他条件的查询，统计符合对应值的记录
    // if (condtition.size() > 0)
    // {
    // String[] fields = new String[condtition.size()];
    // condtition.keySet().toArray(fields);
    // params.put("fields", fields);
    params = parseListParams(tableName, condtition, null);
    // }
    return businessFormMapper.listCount(params);
  }

  /**
   * 解析listParams中list和count的公共部分，表名、搜索、树点击、个人页面
   * 
   * @param tableName 数据库表名
   * @param listParams controller传递来的list的参数map：<br/>
   * @param params 存放解析后将被mapper使用的参数map(可为null)
   * @return params
   */
  private Map<String, Object> parseListParams(String tableName, Map<String, String> listParams,
      Map<String, Object> params) {
    if (params == null) {
      params = new HashMap<String, Object>();
    }
    /**
     * 获取表名和表中数据字段
     */
    params.put("tableName", tableName);
    /**
     * 获取搜索参数
     */
    String searchFor = listParams.get("searchFor");
    String searchScope = listParams.get("searchScope");
    if (!CommonTools.isNull(searchFor) && !CommonTools.isNull(searchScope)) {
      String[] searchInFields = searchScope.split(",");
      params.put("searchFor", searchFor.trim());
      params.put("searchInFields", searchInFields);
      params.put("search", true);
    }
    /**
     * 获取tree查询参数
     */
    String treeValue = listParams.get("treeValue");
    String treeType = listParams.get("treeType");
    if (!CommonTools.isNull(treeValue) && !CommonTools.isNull(treeType)) {
      // 自定义树
      if (FormTemplateConstant.TREE_TYPE_CUSTOM.equals(treeType)) {
        Map<String, String> treeValueMap = new HashMap<String, String>();
        List<String> treeRelateFields = new ArrayList<String>();
        // treeValue 由 查询值=字段 组成 并以"," 分隔多条
        String[] values = treeValue.split(",");
        for (String value : values) {
          String[] valueField = value.split("=");
          if (valueField.length == 2) {
            treeRelateFields.add(valueField[1]);
            treeValueMap.put(valueField[1], valueField[0]);
          }
        }
        params.put("treeValueMap", treeValueMap);
        params.put("treeRelateFields", treeRelateFields);
        params.put(FormTemplateConstant.TREE_TYPE_CUSTOM, true);
      }
      // 日期树
      else if (FormTemplateConstant.TREE_TYPE_DATE.equals(treeType)) {
        // treeValue 由 查询值=字段 组成 并以"," 分隔多条
        /*
         * String[] valueField = treeValue.split("="); if (valueField.length == 2) {
         * params.put("treeValue", valueField[0]); params.put("treeRelateField", valueField[1]);
         * params.put("dateLength", valueField[0].length()); }
         */
        params.put("treeValue", treeValue);
        params.put("treeRelateField", listParams.get("treeField"));
        params.put("dateLength", treeValue.length());

        params.put(FormTemplateConstant.TREE_TYPE_DATE, true);
      }
      params.put("treeList", true);
    }
    /**
     * 获取listType参数，若为personal只list当前用户的数据
     */
    String listType = listParams.get(FormItemConstant.LIST_TYPE_KEY);
    String currentUserId = listParams.get("currentUserId");
    String currentUserCompany = listParams.get("currentUserCompany");
    if (!CommonTools.isNull(listType) && listType.equals(FormItemConstant.LIST_TYPE_PERSONAL)) {
      params.put(CommonWorkflowConstant.COLUMN_AUTHORID, currentUserId);
      params.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE, currentUserCompany);
      params.put("personal", true);
    }
    // 尝试获取companyCode参数，若不为空sql中将只查询指定公司code的数据
    // params.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE,
    // listParams.get("companyCode"));
    // 2015-12-3 取不区分大小写的公司和报表时间 如果有这两个值,则需要判断是不有重复
    String companyCodeFind =
        getIgnoreCaseValue(listParams, CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    params.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE, companyCodeFind);
    // 如果指定了时间则添加时间条件
    String reportTimeFind =
        getIgnoreCaseValue(listParams, CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME);
    params.put(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME, reportTimeFind);
    if (listParams.get("validate") != null) {
      params.remove(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME);
      params.put("validate", listParams.get("validate"));
    }

    return params;
  }

  @Override
  public List<StatisticsInfo> queryDateTree(FormTemplate formTemplate, String companyCode) {
    if (formTemplate == null || CommonTools.isNull(formTemplate.getTemplateRelevanceTable())
        || CommonTools.isNull(formTemplate.getUserDefinedTreeRole())) {
      throw new JmeException("businessFormService.queryDateTree参数为空");
    }
    String tableName = formTemplate.getTemplateRelevanceTable();
    String rule = formTemplate.getUserDefinedTreeRole();
    String columnName = rule.substring(rule.indexOf("=") + 1);
    List<StatisticsInfo> treeNodes = new ArrayList<StatisticsInfo>();
    List<StatisticsInfo> years =
        businessFormMapper.queryYearNode(tableName, columnName, companyCode);
    treeNodes.addAll(years);
    List<StatisticsInfo> months =
        businessFormMapper.queryMonthNode(tableName, columnName, companyCode);
    treeNodes.addAll(months);
    // treeNodes.addAll(businessFormMapper.queryDayNode(tableName,
    // columnName));
    return treeNodes;
  }

  /**
   * 查询模板可以组成的日期树节点
   * 
   * @param dynamicBusinessObject 模板（必须有表名，和tree定义规则）
   * @return 返回定义字段组成的日期树节点list
   */
  @Override
  public List<StatisticsInfo> queryDateTree(FormTemplate formTemplate) {
    return this.queryDateTree(formTemplate, null);
  }

  /**
   * 根据业务表统计表中 开始流程且未走完的所有记录
   * 
   * @param tableName 表名
   * @return 未完成流程记录数
   */
  @Override
  public int countUnfinishedProcess(String tableName) {
    if (CommonTools.isNull(tableName)) {
      throw new JmeException("businessFormService.countUnfinishedProcess参数为空");
    }
    return businessFormMapper.countUnfinishedProcess(tableName);
  }

  @Override
  public FormTemplate findFormTemplate(String formTemplateId, String formTemplateCode) {
    User user = (User) ApplicationHolder.getSessionMapValue(Constant.SESSION_USER);

    FormTemplate formTemplate = null;
    // 如果是用ID查询
    if (formTemplateId != null && formTemplateId.length() > 0) {
      formTemplate = formTemplateService.queryFormTemplateData(formTemplateId, user);
    }

    // 如果没有提供ID查询提供了Code
    if (formTemplate == null && formTemplateCode != null && formTemplateCode.length() > 0) {
      formTemplate = formTemplateService.findByCode(formTemplateCode);
      if (formTemplate != null) {
        formTemplate = formTemplateService.queryFormTemplateData(formTemplate.getId(), user);
      }
    }

    return formTemplate;
  }

  /**
   * 获取对应的动态表单业务表入口UI设置
   * 
   * @param type
   * @param companyCode
   * @return
   */
  @Override
  public EntryConfig getEntryConfig(String type, String companyCode) {
    if (CommonTools.isNull(type) || CommonTools.isNull(companyCode)) {
      throw new JmeException("businessFormService.getEntryConfig参数为空");
    }
    return businessFormMapper.getEntryConfig(new EntryConfig(type, companyCode));
  }

  /**
   * 根据公司编码集合 批量获取对应的动态表单业务表入口UI设置
   * 
   * @param type (流程软件中的procname)
   * @param comList
   */
  @Override
  public List<EntryConfig> getEntryConfig(String type, List<String> comList) {
    if (CommonTools.isNull(type) || CommonTools.isNullOrEmpty(comList)) {
      throw new JmeException("businessFormService.getEntryConfig参数为空");
    }
    ArrayList<EntryConfig> result = new ArrayList<EntryConfig>();
    result.addAll(businessFormMapper.getEntryConfigs(type, comList));
    return result;
  }

  /**
   * 使用excel导入数据到模板业务表中
   * 
   * @param dynamicBusinessObject 动态表单业务对象 （用于传递模板信息及用户、公司等数据）
   * @param excelFileName 上传后的excel在项目临时文件夹中的文件名
   * @return 导入失败的数据集合
   */
  @Transactional
  @Override
  public List<Map<String, String>> importDataByExcel(DynamicBusinessObject dynamicBusinessObject,
      String excelFileName) {
    if (CommonTools.isNull(dynamicBusinessObject) || CommonTools.isNull(excelFileName)) {
      throw new JmeException("businessFormService.importDataByExcel参数为空");
    }
    DicData dicData = dicDataManager.find(DicConstant.DIC_XLS_IMPORT_INDICATOR_SETTING);
    if (dicData == null) {
      throw new JmeException("导入缺少更新关联指标的字典配置");
    }
    boolean impXlsUptRelIndicator = Boolean.valueOf(dicData.getValue());
    // 插入数据库失败的dataList
    ArrayList<Map<String, String>> failDataList = new ArrayList<Map<String, String>>();
    /**
     * 将excel解析成数据map
     */
    List<Map<String, String>> dataList = DFExcelUtil.parseDataListFromExcelFile(
        new File(tempFilePath + excelFileName), dynamicBusinessObject.getTemplateCode());
    /**
     * 循环list 调用本方法中的insert方法(为了有校验，也可单独写一个)
     */
    Map<String, String> dataMap = dynamicBusinessObject.getDataMap();
    for (Map<String, String> rowData : dataList) {
      if (rowData.size() > 0) {
        dynamicBusinessObject = dynamicBusinessObject.newDataMap(dynamicBusinessObject);
        dynamicBusinessObject.put(dataMap);
        dynamicBusinessObject.put(rowData);
        try {
          // 数据插入日报表
          this.insert(dynamicBusinessObject, false);
          /**
           * 数据更新到指标表
           */
          Map<String, String> data = dynamicBusinessObject.getDataMap();
          String companyCode =
              getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
          String reportTime =
              getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME);
          Date newTime = CommonTools.parseTime(reportTime, "yyyy-MM-dd");

          ReportCrossItem itemCondition = new ReportCrossItem();
          itemCondition.setFormTemplateId(dynamicBusinessObject.getTemplateId());
          itemCondition.setCompanyCode(companyCode);
          List<ReportCrossItem> reportCrossItems = reportCrossItemService.query(itemCondition);
          HashMap<String, ReportCrossItem> items = new HashMap<String, ReportCrossItem>();
          if (reportCrossItems != null) {
            for (ReportCrossItem item : reportCrossItems) {
              items.put(item.getItem(), item);
            }
          }
          reportItemCalculatorService.updateDataToIndicator(items, data, newTime, companyCode,
              impXlsUptRelIndicator);
        } catch (Exception e) {
          // 将错误信息存入data中，当导出失败数据时作为注释说明
          String message = e.getMessage();
          message.replaceAll("\\s+", "");
          rowData.put(FormTemplateConstant.EXCEL_IMPORT_FAILINFO_FIELD, message);
          failDataList.add(rowData);
        }
      }
    }
    // 完成后将所有错误的map 组成新的excel导出
    return failDataList;
  }

  /**
   * 获取业务表的excel模板 (从项目excelTemplate文件夹中复制到临时文件夹中，便于控制器统一管理下载)
   * 
   * @param code 业务模板code
   * @return 模板文件在项目临时文件夹中的名字
   */
  @Override
  public String getExcelTemplateFileName(String code) {
    if (CommonTools.isNull(code)) {
      throw new JmeException("businessFormService.getExcelTemplateFileName参数为空");
    }
    String webRoot =
        ApplicationHolder.getRequest().getSession().getServletContext().getRealPath("/");
    String fileName = code + ".xls";
    File templateFile = new File(
        webRoot + FormTemplateConstant.EXCEL_TEMPLATE_DIR_NAME + File.separator + fileName);
    File tempFile = new File(tempFilePath + fileName);
    if (templateFile.exists()) {
      // 如果临时文件夹中已存在
      if (tempFile.exists()) {
        // 且修改时间大于源文件时间
        if (tempFile.lastModified() > templateFile.lastModified()) {
          return fileName;
        } else {
          tempFile.delete();
        }
      }
      try {
        FileUtils.copyFile(templateFile, tempFile);
        return fileName;
      } catch (IOException e) {
        throw new JmeException("复制excel模板文件到临时文件夹失败，发生IO异常");
      }
    }
    return null;
  }

  public void setBusinessFormMapper(BusinessFormMapper businessFormMapper) {
    this.businessFormMapper = businessFormMapper;
  }

  public void setTableOperateService(TableOperateService tableOperateService) {
    this.tableOperateService = tableOperateService;
  }

  public void setFormItemService(FormItemService formItemService) {
    this.formItemService = formItemService;
  }

  public void setFormTemplateService(FormTemplateService formTemplateService) {
    this.formTemplateService = formTemplateService;
  }

  public void setReportCrossItemService(ReportCrossItemService reportCrossItemService) {
    this.reportCrossItemService = reportCrossItemService;
  }

  public void setReportItemCalculatorService(
      ReportItemCalculatorService reportItemCalculatorService) {
    this.reportItemCalculatorService = reportItemCalculatorService;
  }

  /**
   * 获取项目临时文件夹路径
   * 
   * @return
   */
  @Override
  public String getTempFilePath() {
    return tempFilePath;
  }

  /**
   * 查询历史值(判断是否存在重复数据)
   * 
   * @author pengjinlin
   * @param dynamicBusinessObject
   */
  public void quertAgoValue(DynamicBusinessObject dynamicBusinessObject) {
    // 获取编辑页面数据
    Map<String, String> data = dynamicBusinessObject.getDataMap();

    // 2015-12-3 取不区分大小写的公司和报表时间 如果有这两个值,则需要判断是不有重复
    String companyCode = getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE);
    String reportTime = getIgnoreCaseValue(data, CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME);

    DynamicBusinessObject condition = dynamicBusinessObject.newCopy();
    condition.clearData();

    // String templateCode = dynamicBusinessObject.getTemplateCode();
    String tableName = dynamicBusinessObject.getTemplateRelevanceTable();
    String id = dynamicBusinessObject.getId();

    // 特殊月度统计表
    if ("_Mon".equals(tableName.substring(tableName.length() - 4, tableName.length()))) {
      String dealName = getIgnoreCaseValue(data, "dealName");
      String recordTime = getIgnoreCaseValue(data, "recordTime");
      if (!GWTClientUtil.isNull(recordTime)) {
        if (!GWTClientUtil.isNull(dealName)) {
          condition.put("validate", "(dealName = '" + dealName + "' and substr(recordTime,0,7) = '"
              + recordTime.substring(0, 7) + "')");
        } else {
          condition.put("validate",
              "(substr(recordTime,0,7) = '" + recordTime.substring(0, 7) + "')");
        }
      }
    }
    condition.put(CommonWorkflowConstant.COLUMN_AUTHOR_REPORTTIME, reportTime);
    condition.put(CommonWorkflowConstant.COLUMN_AUTHOR_COMPANYCODE, companyCode);

    List<DynamicBusinessObject> findeds = query(0, 0, condition);

    // 新增
    if (id == null) {
      if (findeds != null && findeds.isEmpty() == false) {
        throw new JmeException("有重复的数据:" + reportTime);
      }
      // 修改
    } else {
      if (findeds != null) {
        if (findeds.size() == 1) {
          DynamicBusinessObject find = findeds.get(0);
          // 如果改变时间或公司不是原来的数据,则会重复
          if (find != null && !id.equals(find.getId())) {
            throw new JmeException("有重复的数据:" + reportTime);
          }
        } else if (findeds.size() > 1) {
          throw new JmeException("有重复的数据:" + reportTime);
        }
      }
    }
  }

}
