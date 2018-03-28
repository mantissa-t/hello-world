package com.jme.help;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.jme.gwt.common.ConstantGWT;

public class ExportToExcel
{

	public static void transData(String data, OutputStream out)
	{
		// 找到列头和数据体
		String[] rows = data.split("\"" + ConstantGWT.EXPORT_SEPARATOR_ESCAPE + "\"");
		String headString = rows[0];
		String bodyString = rows[1];

		// 分析列头
		String[] headArray = headString.split("\"" + ConstantGWT.EXPORT_SEPARATOR_CELL_ESCAPE + "\"");

		List<String> headList = new ArrayList<String>();
		for (int i = 0; i < headArray.length; i++)
		{
			String head = headArray[i];
			//去掉最前面的"
			if (i == 0 && !CommonTools.isNull(head))
			{
				head = head.substring(1, head.length());
			}
			headList.add(head);
		}
		// 分析行数据
		List<List<String>> listRow = new ArrayList<List<String>>();

		String[] bodyArray = bodyString.split("\"" + ConstantGWT.EXPORT_SEPARATOR_ROW_ESCAPE + "\"");
		// 遍历数据
		for (int i = 0; i < bodyArray.length; i++)
		{
			// 遍历行
			String[] rowArray = bodyArray[i].split("\"" + ConstantGWT.EXPORT_SEPARATOR_CELL_ESCAPE + "\"");
			List<String> listCell = new ArrayList<String>();
			for (int j = 0; j < rowArray.length; j++)
			{
				String value=rowArray[j];
				//去掉最后面的"
				if((i+1==bodyArray.length)&& !CommonTools.isNull(value))
				{
					value=value.substring(0, value.length()-1);
				}
				listCell.add(value);
			}
			listRow.add(listCell);
		}
		exportToExcel(listRow, headList, out);
	}

	public static void exportToExcel(List<List<String>> listRow, List<String> listHead, OutputStream out)
	{
		try
		{
			HSSFWorkbook wb = new HSSFWorkbook();
			HSSFSheet sheet = wb.createSheet("sheet1");
			HSSFRow row = null;
			HSSFCell cell = null;

			// style
			HSSFCellStyle cs = wb.createCellStyle();// create astyle
			HSSFFont littleFont = wb.createFont();// create a Font
			littleFont.setFontName("SimSun");
			littleFont.setFontHeightInPoints((short) 7);
			cs.setFont(littleFont);// set font
			cs.setAlignment(HSSFCellStyle.ALIGN_CENTER);// align center
			cs.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// valign middle
			cs.setBorderBottom(HSSFCellStyle.BORDER_THIN);// bottom border
			cs.setBorderLeft(HSSFCellStyle.BORDER_THIN);// left border
			cs.setBorderRight(HSSFCellStyle.BORDER_THIN);// right border
			cs.setBorderTop(HSSFCellStyle.BORDER_THIN);// top border
			// 遍历数据
			for (int i = 0; i < listRow.size() + 1; i++)
			{
				row = sheet.createRow(i);
				row.setHeight((short) 300);

				for (int j = 0; j < listHead.size(); j++)
				{
					cell = row.createCell(j);
					// 如果是第一行则取列头
					if (i == 0)
					{
						cell.setCellValue(listHead.get(j).toString());
					}
					// 取数据
					else if (i <= listRow.size())
					{
						List<String> tempRow = listRow.get(i - 1);
						cell.setCellValue(tempRow.get(j).toString());
					}
				}
			}

			wb.write(out);
			out.flush();
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}