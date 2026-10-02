package com.pdf.alldocument.office.reader.ss.control;

import android.content.Context;
import android.util.Log;
import android.widget.RelativeLayout;

import com.pdf.alldocument.office.reader.constant.EventConstant;
import com.pdf.alldocument.office.reader.fc.hssf.formula.function.T;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Sheet;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Workbook;
import com.pdf.alldocument.office.reader.ss.view.SheetView;
import com.pdf.alldocument.office.reader.system.IControl;

public class ExcelView extends RelativeLayout {

    private static String TAG = "TEST_TAG_ExcelView";


    private Spreadsheet ss;


    private IControl control;


    public ExcelView(Context context, String filepath, Workbook book, IControl control) {
        super(context);
        this.control = control;
        ss = new Spreadsheet(context, filepath, book, control, this);
        addView(ss, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }


    public void init() {
        ss.init();
    }


    public Spreadsheet getSpreadsheet() {
        return ss;
    }


    public void showSheet(int sheetIndex) {
        ss.showSheet(sheetIndex);
    }


    public void showSheet(String sheetName) {
        ss.showSheet(sheetName);
    }


    public SheetView getSheetView() {
        return ss.getSheetView();
    }


    public int getBottomBarHeight() {
        return control.getMainFrame().getBottomBarHeight();

    }


    public int getCurrentViewIndex() {
        return ss.getCurrentSheetNumber();
    }


    public void dispose() {
        control = null;
        if (ss != null) {
            ss.dispose();
        }
    }

}
