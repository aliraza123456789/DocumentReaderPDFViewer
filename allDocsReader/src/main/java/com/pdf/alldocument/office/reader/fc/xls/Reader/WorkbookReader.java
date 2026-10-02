/*
 * 文件名称:          WorkbookReader.java
 *
 * 编译器:            android2.2
 * 时间:              下午5:39:01
 */
package com.pdf.alldocument.office.reader.fc.xls.Reader;

import android.os.Message;
import android.util.Log;

import com.pdf.alldocument.office.reader.constant.MainConstant;
import com.pdf.alldocument.office.reader.fc.dom4j.Document;
import com.pdf.alldocument.office.reader.fc.dom4j.Element;
import com.pdf.alldocument.office.reader.fc.dom4j.ElementHandler;
import com.pdf.alldocument.office.reader.fc.dom4j.ElementPath;
import com.pdf.alldocument.office.reader.fc.dom4j.io.SAXReader;
import com.pdf.alldocument.office.reader.fc.openxml4j.opc.PackagePart;
import com.pdf.alldocument.office.reader.fc.openxml4j.opc.PackageRelationship;
import com.pdf.alldocument.office.reader.fc.openxml4j.opc.PackageRelationshipCollection;
import com.pdf.alldocument.office.reader.fc.openxml4j.opc.PackageRelationshipTypes;
import com.pdf.alldocument.office.reader.fc.openxml4j.opc.ZipPackage;
import com.pdf.alldocument.office.reader.fc.xls.SSReader;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Sheet;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Workbook;
import com.pdf.alldocument.office.reader.system.AbortReaderError;
import com.pdf.alldocument.office.reader.system.IControl;
import com.pdf.alldocument.office.reader.system.IReader;
import com.pdf.alldocument.office.reader.system.ReaderHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * TODO: 文件注释
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            jqin
 * <p>
 * 日期:            2012-2-24
 * <p>
 * 负责人:           jqin
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class WorkbookReader {
    private static String TAG = "TEST_TAG_WorkbookReader";

    static class SheetThread extends Thread {
        public SheetThread(IControl control, WorkbookReader reader, int sheetIndex) {
            this.reader = reader;
            this.sheetIndex = sheetIndex;
            this.control = control;
        }

        public void run() {
            try {
                reader.readSheetInSlideWindow(control, sheetIndex);
            } catch (OutOfMemoryError | Exception e) {
                control.getSysKit().getErrorKit().writerLog(e, true);
                reader.dispose();
            } finally {
                reader = null;
            }
        }

        private WorkbookReader reader;
        private final int sheetIndex;
        private final IControl control;
    }

    /**
     * slide window width
     */
    private static final int WINDOWWIDTH = 2;

    private static final WorkbookReader reader = new WorkbookReader();

    /**
     *
     */
    public static WorkbookReader instance() {
        return reader;
    }

    /**
     * @param zipPackage
     * @param packagePart
     * @param book
     * @param iReader
     * @throws Exception
     */
    public void read(ZipPackage zipPackage, PackagePart packagePart,
                     Workbook book, SSReader iReader) throws Exception {
        this.zipPackage = zipPackage;
        this.book = book;
        this.iReader = iReader;

        // get sheets information
        getSheetsProp(packagePart);

        //create sheet
        String id;
        for (int i = 0; i < sheetIndexList.size(); i++) {
            Sheet sheet = new Sheet();
            sheet.setWorkbook(book);

            id = sheetIndexList.get(i);
            sheet.setSheetName(sheetNameList.get(id));

            book.addSheet(i, sheet);
        }

        //read sheet data
        worksheetRelCollection = packagePart.getRelationshipsByType(
                PackageRelationshipTypes.WORKSHEET_PART);

        chartsheetRelCollection = packagePart.getRelationshipsByType(
                PackageRelationshipTypes.CHARTSHEET_PART);


        //read sheet
        class WorkbookReaderHandler extends ReaderHandler {
            public WorkbookReaderHandler(IControl control, WorkbookReader reader) {
                this.reader = reader;
                this.control = control;
            }

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MainConstant.HANDLER_MESSAGE_SUCCESS:
                        new SheetThread(control, reader, (Integer) msg.obj).start();
                        break;

                    case MainConstant.HANDLER_MESSAGE_ERROR:
                    case MainConstant.HANDLER_MESSAGE_DISPOSE: {
                        dispose();
                        reader = null;
                    }
                    break;
                }
            }

            private WorkbookReader reader;
            //
            private final IControl control;
        }

        WorkbookReaderHandler handler = new WorkbookReaderHandler(iReader.getControl(), this);
        book.setReaderHandler(handler);

        Message msg = new Message();
        msg.what = MainConstant.HANDLER_MESSAGE_SUCCESS;
        msg.obj = 0;

        handler.handleMessage(msg);
    }

    /**
     * @param currentsheet
     * @throws Exception
     */
    private void readSheetInSlideWindow(IControl control, int currentsheet) throws Exception {


//        Update 39 Last ANR Detected and Solved , Reason was synchronize blocking
        iReader.abortCurrentReading();
        //other sheets in the slide window
        for (int i = currentsheet - WINDOWWIDTH; i <= currentsheet + WINDOWWIDTH; i++) {
            if (i >= 0 && book.getSheet(i) != null && !book.getSheet(i).isAccomplished()) {
                book.getSheet(i).setState(Sheet.State_Reading);
            }
        }

        //read current sheet first
        if (currentsheet >= 0 && book.getSheet(currentsheet) != null && !book.getSheet(currentsheet).isAccomplished()) {
            readSheet(control, currentsheet);
        }
        //other sheets in the slide window
        for (int i = currentsheet - WINDOWWIDTH; i <= currentsheet + WINDOWWIDTH; i++) {
            if (i >= 0 && book.getSheet(i) != null && !book.getSheet(i).isAccomplished()) {
                readSheet(control, i);
            }

        }

    }

    /**
     * @param sheetsRel
     */
    private void readSheet(IControl control, int index) throws Exception {
        PackageRelationship rel = worksheetRelCollection.getRelationshipByID(sheetIndexList.get(index));
        short sheetType = Sheet.TYPE_WORKSHEET;
        if (rel == null) {
            rel = chartsheetRelCollection.getRelationshipByID(sheetIndexList.get(index));
            sheetType = Sheet.TYPE_CHARTSHEET;
        }
        if (rel == null) {
            return;
        }
        PackagePart sheetPart = zipPackage.getPart(rel.getTargetURI());

        if (sheetPart != null) {
            book.getSheet(index).setSheetType(sheetType);
            SheetReader.instance().getSheet(control, zipPackage, book.getSheet(index), sheetPart, iReader);
        }
    }

    /**
     * get sheet id(key) and name(value)
     *
     * @param documentPart
     * @return
     * @throws Exception
     */
    private void getSheetsProp(PackagePart documentPart) throws Exception {
        if (sheetIndexList != null) {
            sheetIndexList.clear();
        } else {
            sheetIndexList = new HashMap<Integer, String>(5);
        }

        if (sheetNameList != null) {
            sheetNameList.clear();
        } else {
            sheetNameList = new HashMap<String, String>(5);
        }

        tempIndex = 0;

        SAXReader saxreader = new SAXReader();
        try {
            WorkBookSaxHandler handler = new WorkBookSaxHandler();
            saxreader.addHandler("/workbook/workbookPr", handler);
            saxreader.addHandler("/workbook/sheets/sheet", handler);

            InputStream in = documentPart.getInputStream();
            saxreader.read(in);
            in.close();
        } finally {
            saxreader.resetHandlers();
        }
    }

    /**
     * @param zipPackage
     * @param packagePart
     * @param key
     * @return
     * @throws Exception
     */
    public boolean searchContent(ZipPackage zipPackage, IReader iReader, PackagePart packagePart, String key) throws Exception {
        // search sheet name
        if (searchContent_SheetName(packagePart, key)) {
            return true;
        } else {

            this.zipPackage = zipPackage;

            //search sheet data
            worksheetRelCollection = packagePart.getRelationshipsByType(
                    PackageRelationshipTypes.WORKSHEET_PART);
            for (int i = 0; i < worksheetRelCollection.size(); i++) {
                if (searchContent_Sheet(iReader, worksheetRelCollection.getRelationship(i), key)) {
                    dispose();
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @param documentPart
     * @param key
     * @return
     * @throws Exception
     */
    private boolean searchContent_SheetName(PackagePart documentPart, String key) throws Exception {
        SAXReader saxreader = new SAXReader();
        InputStream in = documentPart.getInputStream();
        Document poiXls = saxreader.read(in);
        in.close();


        // get sheets id and name
        Element root = poiXls.getRootElement();
        Element sheetsElement = root.element("sheets");

        @SuppressWarnings("unchecked")
        Iterator<Element> iter = sheetsElement.elementIterator();
        Element ele;
        while (iter.hasNext()) {
            ele = iter.next();
            if (ele.attributeValue("name").toLowerCase().contains(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param sheetsRel
     * @param key
     * @return
     * @throws Exception
     */
    private boolean searchContent_Sheet(IReader iReader, PackageRelationship sheetsRel, String key) throws Exception {
        PackagePart sheetPart = zipPackage.getPart(sheetsRel.getTargetURI());
        if (sheetPart != null) {
            return SheetReader.instance().searchContent(zipPackage, iReader, sheetPart, key);
        }

        return false;
    }

    public void dispose() {
        zipPackage = null;

        book = null;
        iReader = null;

        if (sheetNameList != null) {
            sheetNameList.clear();
            sheetNameList = null;
        }

        if (sheetIndexList != null) {
            sheetIndexList.clear();
            sheetIndexList = null;
        }

        if (worksheetRelCollection != null) {
            worksheetRelCollection.clear();
            worksheetRelCollection = null;
        }

        if (chartsheetRelCollection != null) {
            chartsheetRelCollection.clear();
            chartsheetRelCollection = null;
        }
    }


    /**
     * fix very large XML documents
     */
    class WorkBookSaxHandler implements ElementHandler {

        /**
         *
         */
        public void onStart(ElementPath elementPath) {

        }

        /**
         * @throws Exception
         */
        public void onEnd(ElementPath elementPath) {
            if (iReader.isAborted()) {
                throw new AbortReaderError("abort Reader");
            }

            Element elem = elementPath.getCurrent();
            String name = elem.getName();
            if (name.equals("sheet")) {
                String id;
                String sheetName;
                id = elem.attributeValue("id");
                sheetName = elem.attributeValue("name");

                sheetIndexList.put(tempIndex, id);
                sheetNameList.put(id, sheetName);
                tempIndex++;
            } else if (name.equals("workbookPr")) {
                boolean usingDate1904 = false;
                if (elem.attributeValue("date1904") != null) {
                    String elemAttrValue = "date1904";
                    int pareInInt = Integer.parseInt(elemAttrValue.replaceAll("[\\D]", ""));
                    usingDate1904 = pareInInt > 0;

                }
                book.setUsing1904DateWindowing(usingDate1904);
            }

            elem.detach();
        }

    }


    //
    private ZipPackage zipPackage;

    private Workbook book;
    private SSReader iReader;

    //sheet index and id
    private Map<Integer, String> sheetIndexList;
    //sheet id and name
    private Map<String, String> sheetNameList;
    //
    private int tempIndex;
    //
    private PackageRelationshipCollection worksheetRelCollection, chartsheetRelCollection;
}
