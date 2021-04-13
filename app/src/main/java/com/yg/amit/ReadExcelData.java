package com.yg.amit;

import android.util.Log;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ReadExcelData {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    private FormulaEvaluator formulaEvaluator;

    public ReadExcelData(String filePath){
        try {
            File inputFile = new File(filePath);
            InputStream stream = new FileInputStream(inputFile);
            workbook = new XSSFWorkbook(stream);
        }catch (FileNotFoundException e){
            Log.e(Utils.TAG,"Excel file not found: "+e.getMessage());
        }catch (IOException e){
            Log.e(Utils.TAG,"Error reading inputStream: "+e.getMessage());
        }
    }

    public void setSheet(String sheetName){
        sheet = workbook.getSheet(sheetName);
    }

    public int getRowCount(){
       return sheet.getPhysicalNumberOfRows();
    }

    public String getCellData(int row , int col){
        return getCellData(row,col);
    }

}
