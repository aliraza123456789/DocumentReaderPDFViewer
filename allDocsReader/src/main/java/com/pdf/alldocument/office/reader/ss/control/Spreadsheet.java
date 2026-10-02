package com.pdf.alldocument.office.reader.ss.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;

import com.pdf.alldocument.office.reader.common.IOfficeToPicture;
import com.pdf.alldocument.office.reader.common.hyperlink.Hyperlink;
import com.pdf.alldocument.office.reader.common.picture.PictureKit;
import com.pdf.alldocument.office.reader.constant.EventConstant;
import com.pdf.alldocument.office.reader.constant.MainConstant;
import com.pdf.alldocument.office.reader.constant.SSConstant;
import com.pdf.alldocument.office.reader.simpletext.control.IWord;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Cell;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Sheet;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Workbook;
import com.pdf.alldocument.office.reader.ss.model.interfacePart.IReaderListener;
import com.pdf.alldocument.office.reader.ss.util.ModelUtil;
import com.pdf.alldocument.office.reader.ss.view.SheetView;
import com.pdf.alldocument.office.reader.system.IControl;
import com.pdf.alldocument.office.reader.system.IFind;
import com.pdf.alldocument.office.reader.system.ReaderHandler;
import com.pdf.alldocument.office.reader.system.beans.AEventManage;
import com.pdf.alldocument.office.reader.system.beans.CalloutView.CalloutView;
import com.pdf.alldocument.office.reader.system.beans.CalloutView.IExportListener;

import java.io.File;

@SuppressLint("ViewConstructor")
public class Spreadsheet extends LinearLayout implements IFind, IReaderListener, IExportListener {

    private String TAG = "TEST_TAG_Spreadsheet";

    public Spreadsheet(Context context, String filepath, Workbook book, IControl control, ExcelView parent) {
        super(context);
        this.parent = parent;
        fileName = filepath;
        setBackgroundColor(Color.WHITE);
        this.workbook = book;
        this.control = control;
        eventManage = new SSEventManage(this, control);
        this.editor = new SSEditor(this);
        setOnTouchListener(eventManage);
        setLongClickable(true);
    }

    public CalloutView getCalloutView() {
        return callouts;
    }

    public void initCalloutView() {
        if (callouts == null) {
            callouts = new CalloutView(this.getContext(), control, this);
            callouts.setIndex(currentSheetIndex);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.leftMargin = SSConstant.DEFAULT_ROW_HEADER_WIDTH;
            params.topMargin = SSConstant.DEFAULT_COLUMN_HEADER_HEIGHT;
            addView(callouts, params);
        }
    }

    /*
   Update 39
   Method usage should be remove */
    @Override
    public void exportImage() {
        control.actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null);
    }


    public void init() {

        if (sheetview == null) {
            sheetview = new SheetView(this, workbook.getSheet(0));
        }

        initFinish = true;
        short state = workbook.getSheet(0).getState();
        if (state != Sheet.State_Accomplished) {
            workbook.getSheet(0).setReaderListener(this);
            control.actionEvent(EventConstant.SYS_SET_PROGRESS_BAR_ID, true);
        }


           /*
    Update 39
    Method usage should be remove */
        if (state == Sheet.State_Accomplished) {
            post(() -> control.actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null));
        }
    }


    public int getSheetCount() {
        return workbook.getSheetCount();
    }

    public void showSheet(String sheetName) {
        if (currentSheetName != null && currentSheetName.equals(sheetName)) {
            return;
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            return;
        }
        currentSheetName = sheetName;
        currentSheetIndex = workbook.getSheetIndex(sheet);

        showSheet(sheet);
    }


    public void showSheet(int sheetIndex) {
        if (currentSheetIndex == sheetIndex
                || sheetIndex >= getSheetCount()) {
            return;
        }

        Sheet sheet = workbook.getSheet(sheetIndex);
        currentSheetIndex = sheetIndex;
        currentSheetName = sheet.getSheetName();


        if (callouts != null) {
            callouts.setIndex(currentSheetIndex);
        }
        showSheet(sheet);
    }


    private void showSheet(Sheet sheet) {
        try {
            eventManage.stopFling();
            control.getMainFrame().setFindBackForwardState(false);

            sheetview.changeSheet(sheet);

            postInvalidate();

            if (sheet.getState() != Sheet.State_Accomplished) {
                //current sheet has not finished parsing; 
                sheet.setReaderListener(this);
                control.actionEvent(EventConstant.SYS_SET_PROGRESS_BAR_ID, true);
                control.actionEvent(EventConstant.APP_ABORTREADING, null);
            } else {
                control.actionEvent(EventConstant.SYS_SET_PROGRESS_BAR_ID, false);
            }

            //read not accomplished sheet in slide window
            ReaderHandler readerHandler = workbook.getReaderHandler();
            if (readerHandler != null) {
                Message msg = new Message();
                msg.what = MainConstant.HANDLER_MESSAGE_SUCCESS;
                msg.obj = currentSheetIndex;
                readerHandler.handleMessage(msg);
            }
        } catch (Exception e) {
            control.getSysKit().getErrorKit().writerLog(e);
        }
    }


    protected void onDraw(Canvas canvas) {
        if (!initFinish) {
            return;
        }
        try {
            sheetview.drawSheet(canvas);
            if (preShowSheetIndex != currentSheetIndex) {
                control.getMainFrame().changePage();
                preShowSheetIndex = currentSheetIndex;
            }
        } catch (Exception e) {
            control.getSysKit().getErrorKit().writerLog(e);
            Log.d(TAG, "onDraw: Exception" + e);
        }

    }


    /*
   Update 39
   Method usage should be remove */
    public void createPicture() {
        IOfficeToPicture otp = control.getOfficeToPicture();
        if (otp != null && otp.getModeType() == IOfficeToPicture.VIEW_CHANGE_END) {
            try {
                toPicture(otp);
            } catch (Exception e) {
                Log.e(TAG, "createPicture: Exception " + e);
            }
        }
    }

    /*
   Update 39
   Method usage should be remove */
    private void toPicture(IOfficeToPicture otp) {

        Log.d(TAG, "toPicture: Create picture call:");


        boolean b = PictureKit.instance().isDrawPictrue();
        PictureKit.instance().setDrawPictrue(true);

        Bitmap bitmap = otp.getBitmap(getWidth(), getHeight());
        if (bitmap == null) {
            return;
        }
        Canvas picCanvas = new Canvas(bitmap);
        float oldPaintZoom = sheetview.getZoom();
        if (bitmap.getWidth() != getWidth() || bitmap.getHeight() != getHeight()) {
            float zoom = Math.min((float) bitmap.getWidth() / getWidth(), (float) bitmap.getHeight() / getHeight()) * oldPaintZoom;
            sheetview.setZoom(zoom, true);
        }
        picCanvas.drawColor(Color.WHITE);
        sheetview.drawSheet(picCanvas);
        control.getSysKit().getCalloutManager().drawPath(picCanvas, currentSheetIndex, oldPaintZoom);
        otp.callBack(bitmap);
        sheetview.setZoom(oldPaintZoom, true);

        PictureKit.instance().setDrawPictrue(b);
    }



                 /*
    Update 39
  Method usage must be remove */

    /**
     * @param destBitmap
     * @return
     */
    public Bitmap getSnapshot(Bitmap destBitmap) {
        if (destBitmap == null) {
            return null;
        }

        synchronized (sheetview) {
            Log.d(TAG, "getSnapshot: get snapshot synchronized block call:");


            Canvas picCanvas = new Canvas(destBitmap);
            float oldPaintZoom = sheetview.getZoom();
            if (destBitmap.getWidth() != getWidth() || destBitmap.getHeight() != getHeight()) {
                float zoom = Math.min((float) destBitmap.getWidth() / getWidth(), (float) destBitmap.getHeight() / getHeight()) * oldPaintZoom;
                sheetview.setZoom(zoom, true);
            }
            picCanvas.drawColor(Color.WHITE);
            sheetview.drawSheet(picCanvas);

            sheetview.setZoom(oldPaintZoom, true);
            return destBitmap;
        }
    }


    /*
    Update 39
  Method usage must be remove */

    /**
     * get xls thumbnail
     *
     * @param width     thumbnail width when zoomValue is 100
     * @param height    thumbnail height when zoomValue is 100
     * @param zoomValue zoom value
     * @return
     */
    public Bitmap getThumbnail(int width, int height, float zoomValue) {
        Log.d(TAG, "getThumbnail: get thumbnail call:");
        Sheet sheet = workbook.getSheet(0);
        if (sheet == null || sheet.getState() != Sheet.State_Accomplished) {
            return null;
        }

        if (sheetview == null) {
            sheetview = new SheetView(this, workbook.getSheet(0));
        }

        return sheetview.getThumbnail(sheet, width, height, zoomValue);
    }


    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isConfigurationChanged = true;
    }

    /**
     * This is called during layout when the size of this view has changed. If
     * you were just added to the view hierarchy, you're called with the old
     * values of 0.
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isConfigurationChanged) {
            isConfigurationChanged = false;
            // to picture
            post(() -> control.actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null));
        }
    }

    /**
     * 计算流动的位置
     */
    public void computeScroll() {
        eventManage.computeScroll();
    }


    public IControl getControl() {
        return control;
    }


    /**
     * 得到sheetView视图
     */
    public SheetView getSheetView() {
        return sheetview;
    }


    /**
     * @return Returns the workbook.
     */
    public Workbook getWorkbook() {
        return workbook;
    }


    public String getActiveCellContent() {
        if (sheetview.getCurrentSheet().getActiveCell() != null) {
            return ModelUtil.instance().getFormatContents(
                    workbook, sheetview.getCurrentSheet().getActiveCell());
        }

        return "";
    }

    /**
     * active cell hyperlink address
     *
     * @return
     */
    public Hyperlink getActiveCellHyperlink() {
        Cell cell = sheetview.getCurrentSheet().getActiveCell();
        if (cell != null && cell.getHyperLink() != null) {
            return cell.getHyperLink();
        }

        return null;
    }

    /**
     * @param value
     * @return true: finded   false: not finded
     */
    public boolean find(String value) {
        return sheetview.find(value);
    }

    public boolean findBackward() {
        return sheetview.findBackward();
    }

    public boolean findForward() {
        return sheetview.findForward();
    }


    public void resetSearchResult() {
    }


    public int getPageIndex() {
        return -1;
    }


    public float getZoom() {
        if (sheetview == null) {
            sheetview = new SheetView(this, workbook.getSheet(0));
        }
        return sheetview.getZoom();
    }


    public void setZoom(float zoom) {
        if (sheetview == null) {
            sheetview = new SheetView(this, workbook.getSheet(0));
        }
        sheetview.setZoom(zoom); //zoom
    }


    public float getFitZoom() {
        return 1f;
    }


    public AEventManage getEventManage() {
        return this.eventManage;
    }

    /**
     * this function be callde by sheet reader thread,
     * so we need to post to main thread to update UI
     * (non-Javadoc)
     *
     * @see com.pdf.alldocument.office.reader.ss.model.interfacePart.IReaderListener#OnReadingFinished()
     */
    public void OnReadingFinished() {
        if (control != null && control.getMainFrame().getActivity() != null) {
            post(() -> {
                control.actionEvent(EventConstant.SYS_SET_PROGRESS_BAR_ID, false);

                control.actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null);

                postInvalidate();
            });
        }
    }


    public String getFileName() {
        return fileName;
    }

    /**
     * abort current sheet drawing
     */
    public void abortDrawing() {
        abortDrawing = true;
    }


    public void startDrawing() {
        abortDrawing = false;
    }


    public boolean isAbortDrawing() {
        return abortDrawing;
    }


    public int getCurrentSheetNumber() {
        return this.currentSheetIndex + 1;
    }


    public int getBottomBarHeight() {
        return parent.getBottomBarHeight();
    }


    public IWord getEditor() {
        return this.editor;
    }


    public void dispose() {
        parent = null;
        fileName = null;
        control = null;
        workbook = null;

        if (sheetview != null) {
            sheetview.dispose();
            sheetview = null;
        }

        if (eventManage != null) {
            eventManage.dispose();
            eventManage = null;
        }
        if (editor != null) {
            editor.dispose();
            editor = null;
        }
        /*if (bar != null)
        {
            bar.dispose();
            bar = null;
        }*/
    }

    private ExcelView parent;
    //
    private boolean isConfigurationChanged;
    //
    //abort current sheet drawing or not
    private boolean abortDrawing;
    //
    private boolean initFinish;

    public void setInitFinish(boolean initFinish) {
        this.initFinish = initFinish;
    }

    //
    private int preShowSheetIndex = -1;
    //
    private int currentSheetIndex;
    private String currentSheetName;
    //
    //file name
    private String fileName;
    //
    private IControl control;
    // excel model 后期需修改
    private Workbook workbook;
    // 当前Sheet
    private SheetView sheetview;
    // 事件管理器
    private SSEventManage eventManage;
    //
    private SSEditor editor;
    //
    private CalloutView callouts;

}
