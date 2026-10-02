/*
 * 文件名称:          SSControl.java
 *
 * 编译器:            android2.2
 * 时间:              下午3:49:55
 */
package com.pdf.alldocument.office.reader.ss.control;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.ClipboardManager;
import android.view.View;

import com.pdf.alldocument.office.reader.common.IOfficeToPicture;
import com.pdf.alldocument.office.reader.common.hyperlink.Hyperlink;
import com.pdf.alldocument.office.reader.constant.EventConstant;
import com.pdf.alldocument.office.reader.constant.MainConstant;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Sheet;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Workbook;
import com.pdf.alldocument.office.reader.ss.util.ReferenceUtil;
import com.pdf.alldocument.office.reader.system.AbstractControl;
import com.pdf.alldocument.office.reader.system.IControl;
import com.pdf.alldocument.office.reader.system.IFind;
import com.pdf.alldocument.office.reader.system.IMainFrame;
import com.pdf.alldocument.office.reader.system.SysKit;

import java.util.List;
import java.util.Vector;

public class SSControl extends AbstractControl {

    private static String TAG = "TAG_TEST_SSControl";

    public SSControl(IControl mainControl, Workbook book, String filepath) {
        this.mainControl = mainControl;
        this.excelView = new ExcelView(getMainFrame().getActivity(), filepath, book, this);
        this.spreadSheet = excelView.getSpreadsheet();
    }

    public void layoutView(int x, int y, int w, int h) {
    }

    /**
     * action派发
     *
     * @param actionID 动作ID
     * @param obj      动作ID的Value
     */
    public void actionEvent(int actionID, final Object obj) {
        Intent intent;
        switch (actionID) {
            case EventConstant.SYS_SET_PROGRESS_BAR_ID:
                if (spreadSheet.getParent() != null) {
                    spreadSheet.post(() -> {
                        if (!isDispose) {
                            //getActivity().setProgressBarIndeterminateVisibility((Boolean)obj);
                            mainControl.getMainFrame().showProgressBar((Boolean) obj);
                        }
                    });
                }
                break;

          /*  case EventConstant.SYS_VECTORGRAPH_PROGRESS:
                if (spreadSheet.getParent() != null) {
                    spreadSheet.post(() -> {
                        if (!isDispose) {
                            mainControl.getMainFrame().updateViewImages((List<Integer>) obj);
                        }
                    });
                } else {
                    new Thread() {
                        public void run() {
                            if (!isDispose) {
                                mainControl.getMainFrame().updateViewImages((List<Integer>) obj);
                            }
                        }
                    }.start();
                }
                break;*/

            case EventConstant.SYS_INIT_ID:
                excelView.init();
                break;

            case EventConstant.SS_SHOW_SHEET:
                excelView.showSheet((Integer) obj);
                break;

            case EventConstant.TEST_REPAINT_ID:
                spreadSheet.postInvalidate();
                break;


            case EventConstant.APP_ZOOM_ID:
                int[] params = (int[]) obj;
                spreadSheet.setZoom(params[0] / (float) MainConstant.STANDARD_RATE); //zoom

                break;
/*
            case EventConstant.FILE_COPY_ID:                        //copy
                ClipboardManager clip = (ClipboardManager) getMainFrame().getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setText(spreadSheet.getActiveCellContent());
                break;*/

            case EventConstant.APP_HYPERLINK:                       //hyperlink
                Hyperlink hyperlink = spreadSheet.getActiveCellHyperlink();
                if (hyperlink != null) {
                    try {
                        if (hyperlink.getLinkType() == Hyperlink.LINK_DOCUMENT) {
                            String addr = hyperlink.getAddress();
                            int index = addr.indexOf("!");
                            String sheetName = addr.substring(0, index).replace("'", "");
                            String ref = addr.substring(index + 1, addr.length());

                            int rowIndex = ReferenceUtil.instance().getRowIndex(ref);
                            int columnIndex = ReferenceUtil.instance().getColumnIndex(ref);

                            Sheet sheet = spreadSheet.getWorkbook().getSheet(sheetName);
                            sheet.setActiveCellRowCol(rowIndex, columnIndex);
                            excelView.showSheet(sheetName);

                            rowIndex -= 1;
                            columnIndex -= 1;

                            spreadSheet.getSheetView().goToCell(Math.max(rowIndex, 0), Math.max(columnIndex, 0));

                            spreadSheet.postInvalidate();

                        } else if (hyperlink.getLinkType() == Hyperlink.LINK_EMAIL
                                || hyperlink.getLinkType() == Hyperlink.LINK_URL) {
                            intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(hyperlink.getAddress()));
                            getMainFrame().getActivity().startActivity(intent);
                        } else {
                            //file hyperlink
                            mainControl.actionEvent(EventConstant.SYS_SHOW_TOOLTIP, "not supported hyperlink!");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case EventConstant.APP_INTERNET_SEARCH_ID:              //internet search
                getSysKit().internetSearch(spreadSheet.getActiveCellContent(), getMainFrame().getActivity());
                break;


            case EventConstant.APP_GENERATED_PICTURE_ID:
//                exportImage();
                break;

            case EventConstant.APP_ABORTREADING:
                if (mainControl.getReader() != null) {
                    mainControl.getReader().abortReader();
                }
                break;

            case EventConstant.APP_INIT_CALLOUTVIEW_ID:
                spreadSheet.initCalloutView();
                break;

            default:
                break;
        }
    }

    public Object getActionValue(int actionID, Object obj) {
        switch (actionID) {
            case EventConstant.APP_ZOOM_ID:
                return spreadSheet.getZoom();

            case EventConstant.APP_FIT_ZOOM_ID:
                return spreadSheet.getFitZoom();

            case EventConstant.APP_COUNT_PAGES_ID:
                return spreadSheet.getSheetCount();

            case EventConstant.APP_CURRENT_PAGE_NUMBER_ID:
                return spreadSheet.getCurrentSheetNumber();

            case EventConstant.SS_GET_ALL_SHEET_NAME:
                Vector<String> vec = new Vector<>();
                Workbook book = spreadSheet.getWorkbook();
                int cnt = book.getSheetCount();
                for (int i = 0; i < cnt; i++) {
                    vec.add(book.getSheet(i).getSheetName());
                }
                return vec;

            case EventConstant.SS_GET_SHEET_NAME:
                int number = (Integer) obj;
                if (number == -1) {
                    return null;
                }
                Sheet sheet = spreadSheet.getWorkbook().getSheet(number - 1);
                if (sheet != null) {
                    return sheet.getSheetName();
                }
                return null;

            case EventConstant.APP_THUMBNAIL_ID:
                if (obj instanceof int[]) {
                    int[] paraArr = (int[]) obj;
                    if (paraArr.length == 3) {
                        return spreadSheet.getThumbnail(paraArr[0], paraArr[1], paraArr[2] / (float) MainConstant.STANDARD_RATE);
                    }
                }

            case EventConstant.APP_GET_SNAPSHOT_ID:
                /*
    Update 39
    Lines usage must be remove */
                if (spreadSheet != null) {
                    return spreadSheet.getSnapshot((Bitmap) obj);
                }
                break;

            default:
                break;
        }
        return null;
    }


    /*
    Update 39
    Method usage is commented */
    private void exportImage() {
        spreadSheet.post(new Runnable() {

            @Override
            public void run() {
                if (!isDispose) {
                    spreadSheet.createPicture();
                }
            }
        });
    }

    public int getCurrentViewIndex() {
        return excelView.getCurrentViewIndex();
    }


    public View getView() {
        return excelView;
    }


    public Dialog getDialog(Activity activity, int id) {
        return null;
    }


    public IMainFrame getMainFrame() {
        return mainControl.getMainFrame();
    }


    public Activity getActivity() {
        return mainControl.getMainFrame().getActivity();
    }


    public IFind getFind() {
        return spreadSheet;
    }


    public IOfficeToPicture getOfficeToPicture() {
        return mainControl.getOfficeToPicture();
    }


    public byte getApplicationType() {
        return MainConstant.APPLICATION_TYPE_SS;
    }


    public SysKit getSysKit() {
        return mainControl.getSysKit();
    }

    public void dispose() {
        isDispose = true;
        mainControl = null;
        spreadSheet = null;
        if (excelView != null) {
            excelView.dispose();
            excelView = null;
        }
    }


    private IControl mainControl;

    private boolean isDispose;

    private Spreadsheet spreadSheet;

    private ExcelView excelView;
}
