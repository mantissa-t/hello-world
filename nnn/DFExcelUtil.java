package com.jme.dynamicForm.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme.dynamicForm.constant.FormTemplateConstant;
import com.jme.exception.JmeException;
import com.jme.gwt.common.ConstantGWT;
import com.jme.help.CommonTools;

public class DFExcelUtil
{
    private static Logger logger = LoggerFactory.getLogger(DFExcelUtil.class);

    /**
     * 将excel文件中的数据解析成一个由数据map组成的list
     * 
     * <p>
     * excel文件格式要求：<br/>
     * 1.第一行必须由字段名组成，即每一列数据具体属于数据库中的哪一字段 <br/>
     * 2.表格某一行第一列的内容为字符的"dataBody",然后此行往下的所有行直到表格末尾都是数据行 <br/>
     * 即第一行到"dataBody"行中间的任何数据都将被忽略 <br/>
     * 3.若存在校验参数，则在第一行和"dataBody"行中，必有一行为校验字符，否则抛出异常 <br/>
     * <br/>
     * 返回的list中的每一个map: <br/>
     * key为该数据值所在列的第一行中的值（即表头字段名） <br/>
     * value为数据值的字符串形式（其中日期类型的数据将只会被转换成yyyy-MM-dd的形式）<br/>
     * </p>
     * @param excelFile 要解析的文件
     * @param validStr 校验模板字符串 此参数为null或""-不进行校验 ||
     *        当此参数不为空-在模板文件中某一行的第一列必存在此值，否则抛出异常
     * @return
     */
    public static List<Map<String, String>> parseDataListFromExcelFile(File excelFile, String validStr)
    {
        if (excelFile == null || !excelFile.exists())
        {
            throw new JmeException("要解析的excel文件为空或不存在");
        }
        try
        {
            Workbook wb = WorkbookFactory.create(excelFile);
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            /*
             * 仅处理第一个sheet
             */
            Sheet sheet = wb.getSheetAt(0);
            /**
             * 校验模板是否正确，即validStr是否存在于excelFile中,若不存在将抛出异常
             */
            if (!CommonTools.isNull(validStr))
            {
                validation(sheet, validStr);
            }
            /*
             * 处理sheet第一行，第一行用于放置表头字段信息
             */
            ArrayList<String> headList = getHeadFieldList(sheet);
            /*
             * 获取 dataBody行后第一行的行号，此行开始获取数据
             */
            Integer dataRowIndex = getDataStartRowIndex(sheet);

            ArrayList<Map<String, String>> dataList = new ArrayList<Map<String, String>>();
            int lastRowNum = sheet.getLastRowNum();
            for (int i = dataRowIndex; i <= lastRowNum; i++)
            {
                Row row = sheet.getRow(i);
                boolean rowHasData = false;
                /*
                 * 将行数据转为Map，存入dataList中
                 */
                if (row != null)
                {
                    Map<String, String> data = new HashMap<String, String>();
                    for (Cell cell : row)
                    {
                        int columnIndex = cell.getColumnIndex();
                        if (columnIndex > headList.size())
                        {
                            continue;
                        }
                        String headField = headList.get(columnIndex);
                        if (headField == null)
                        {
                            continue;
                        }
                        String value = null;

                        switch (cell.getCellType())
                        {
                            case Cell.CELL_TYPE_STRING:
                                value = formatter.formatCellValue(cell);
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell))
                                {
                                    value = CommonTools.format(cell.getDateCellValue(), ConstantGWT.TIMEFORMAT_SHORTDATE);
                                }
                                else
                                {
                                    value = formatter.formatCellValue(cell, evaluator);
                                }
                                break;
                            case Cell.CELL_TYPE_FORMULA:
                                value = formatter.formatCellValue(cell, evaluator);
                                break;
                            default:
                                value = "";
                                break;
                        }
                        if (value != null)
                        {// 处理太多空格和回车的情况
                            value = value.replaceAll(" +", " ");
                            value = value.replaceAll("[\n\r]+", "\n\r");
                        }
                        data.put(headField, value);
                        if (!rowHasData && !CommonTools.isNull(value))
                        {
                            rowHasData = true;
                        }
                    }
                    if (rowHasData)
                    {
                        dataList.add(data);
                    }
                }
            }
            return dataList;
        }
        catch (InvalidFormatException e)
        {
            logger.error("解析excel数据发生文件异常", e);
            throw new JmeException("文件格式不支持：" + e.getMessage());
        }
        catch (IOException e)
        {
            logger.error("解析excel数据发生IO异常", e);
            throw new JmeException("解析excel数据发生IO异常：" + e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("解析excel数据发生异常", e);
            throw new JmeException("解析excel数据发生异常：" + e.getMessage());
        }
    }

    /**
     * 将excel文件中的数据解析成一个由数据map组成的list
     * 
     * <p>
     * excel文件格式要求：<br/>
     * 1.第一行必须由字段名组成，即每一列数据具体属于数据库中的哪一字段 <br/>
     * 2.表格某一行第一列的内容为字符的"dataBody",然后此行往下的所有行直到表格末尾都是数据行 <br/>
     * 即第一行到"dataBody"行中间的任何数据都将被忽略 <br/>
     * <br/>
     * 返回的list中的每一个map: <br/>
     * key为该数据值所在列的第一行中的值（即表头字段名） <br/>
     * value为数据值的字符串形式（其中日期类型的数据将只会被转换成yyyy-MM-dd的形式）<br/>
     * </p>
     * @param excelFile 要解析的文件
     * @return
     */
    public static List<Map<String, String>> parseDataListFromExcelFile(File excelFile)
    {
        return parseDataListFromExcelFile(excelFile, null);
    }

    /**
     * 获取模板sheet中"dataBody"行的行号，因为此行以下用于存放数据
     * @param sheet
     * @return
     */
    private static Integer getDataStartRowIndex(Sheet sheet)
    {
        Integer dataRowIndex = null;
        for (Row row : sheet)
        {
            Cell firstCell = row.getCell(0);
            if (firstCell != null && firstCell.getCellType() == Cell.CELL_TYPE_STRING && "dataBody".equalsIgnoreCase(firstCell.getStringCellValue()))
            {
                dataRowIndex = row.getRowNum() + 1;
                break;
            }
        }
        if (dataRowIndex == null)
        {
            throw new JmeException("要解析的excel模板文件格式不正确，请联系管理员");
        }
        return dataRowIndex;
    }

    /**
     * 获取模板sheet中"dataBody"行的行号，因为此行以下用于存放数据
     * @param sheet
     * @return
     */
    private static void validation(Sheet sheet, String validStr)
    {
        for (Row row : sheet)
        {
            Cell firstCell = row.getCell(0);
            if (firstCell != null && firstCell.getCellType() == Cell.CELL_TYPE_STRING && validStr.equalsIgnoreCase(firstCell.getStringCellValue()))
            {
                return;
            }
        }
        throw new JmeException("请核对确认上传的模板是否正确，或重新下载模板文件");
    }

    /**
     * 获取sheet的第一行中的数据，组成表头字段集合 (单元格必须是字符格式的，否则为""，存于list对应index)
     * @param sheet
     * @return
     */
    private static ArrayList<String> getHeadFieldList(Sheet sheet)
    {
        /*
         * 处理第一行，第一行用于放置表字段信息
         */
        ArrayList<String> headList = new ArrayList<String>();
        Row headRow = sheet.getRow(0);
        for (Cell cell : headRow)
        {
            int columnIndex = cell.getColumnIndex();
            String value = null;

            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
            {
                value = cell.getStringCellValue();
            }
            else
            {
                value = "";
            }
            headList.add(columnIndex, value);
        }
        return headList;
    }

    /**
     * 根据模板xls文件及导入失败的数据，生成失败数据的xls文件
     * 
     * <p>
     * 模板excel文件格式要求：<br/>
     * 1.第一行必须由字段名组成，即每一列数据具体属于数据库中的哪一字段 <br/>
     * 2.如果对于此列有特殊设置，如此列为日期，请确保本行有设置单元格格式
     * </p>
     * @param templateExcelFile 模板文件
     * @param outputFile [空的]xls文件，数据生成到此file中(将会被改写，注意不要保存有需要用的信息)
     * @param failDatas 导入失败的数据list
     */
    public static void createFailDataXls(File templateInputFile, File outputFile, List<Map<String, String>> failDatas)
    {
        if (templateInputFile == null || !templateInputFile.exists() || outputFile == null || failDatas == null || failDatas.size() == 0)
        {
            throw new JmeException("生成失败数据xls文件，参数为空或不存在");
        }
        String outputFileName = outputFile.getName().toLowerCase();
        if (!(outputFileName.endsWith(".xls") || outputFileName.endsWith(".xlsx")))
        {
            throw new JmeException("生成失败数据xls文件，导出文件格式不正确：" + outputFileName);
        }
        FileOutputStream out = null;
        try
        {
            Workbook wb = WorkbookFactory.create(templateInputFile);

            // 仅处理第一个sheet
            Sheet sheet = wb.getSheetAt(0);
            Row fieldRow = sheet.getRow(sheet.getFirstRowNum());

            // 为添加错误信息注释准备的helper和drawing
            CreationHelper creationHelper = wb.getCreationHelper();
            Drawing drawing = sheet.createDrawingPatriarch();

            // 获取表头字段集合
            ArrayList<String> headFieldList = getHeadFieldList(sheet);

            // 获取 dataBody行后第一行的行号，此行开始生成数据
            Integer dataRowIndex = getDataStartRowIndex(sheet);

            for (Map<String, String> failData : failDatas)
            {
                if (failData.size() > 0)
                {
                    Row newRow = sheet.createRow(dataRowIndex++);

                    for (int i = 0; i < headFieldList.size(); i++)
                    {
                        Cell newCell = newRow.createCell(i, fieldRow.getCell(i).getCellType());
                        newCell.setCellValue(failData.get(headFieldList.get(i)));
                        // 首cell添加错误信息注释
                        String failInfo = failData.get(FormTemplateConstant.EXCEL_IMPORT_FAILINFO_FIELD);
                        if (i == 0 && !CommonTools.isNull(failInfo))
                        {
                            ClientAnchor anchor = creationHelper.createClientAnchor();
                            anchor.setCol1(newCell.getColumnIndex());
                            anchor.setCol2(newCell.getColumnIndex() + 4);
                            anchor.setRow1(newCell.getRowIndex());
                            anchor.setRow2(newCell.getRowIndex() + 3);

                            Comment comment = drawing.createCellComment(anchor);
                            RichTextString str = creationHelper.createRichTextString(failInfo);
                            comment.setString(str);

                            newCell.setCellComment(comment);
                        }
                    }
                }
            }
            // failData之后的行全部删除
            for (int l = sheet.getLastRowNum(); dataRowIndex <= l; l--)
            {
                sheet.removeRow(sheet.getRow(l));
            }

            if (outputFile.exists())
            {
                outputFile.delete();
            }
            outputFile.createNewFile();
            // 写入要导出的xls文件中
            out = new FileOutputStream(outputFile);
            wb.write(out);
        }
        catch (InvalidFormatException e)
        {
            logger.error("生成失败数据xls文件发生格式异常", e);
            throw new JmeException("生成失败数据xls文件，发生无效的文件格式异常:" + e.getMessage());
        }
        catch (IOException e)
        {
            logger.error("生成失败数据xls文件发生IO异常", e);
            throw new JmeException("生成失败数据xls文件，发生IO异常:" + e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("生成失败数据xls文件发生异常", e);
            throw new JmeException("生成失败数据xls文件，发生异常:" + e.getMessage());
        }
        finally
        {
            try
            {
                out.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
