/*
 * 文件名称:          FileReaderThread.java
 *
 * 编译器:            android2.2
 * 时间:              下午8:11:02
 */
package com.pdf.alldocument.office.reader.system;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.pdf.alldocument.office.reader.constant.MainConstant;
import com.pdf.alldocument.office.reader.fc.doc.DOCReader;
import com.pdf.alldocument.office.reader.fc.doc.DOCXReader;
import com.pdf.alldocument.office.reader.fc.doc.TXTReader;
import com.pdf.alldocument.office.reader.fc.ppt.PPTReader;
import com.pdf.alldocument.office.reader.fc.ppt.PPTXReader;
import com.pdf.alldocument.office.reader.fc.xls.XLSReader;
import com.pdf.alldocument.office.reader.fc.xls.XLSXReader;

public class FileReaderThread extends Thread {

    private static final String TAG = "FileReaderThread";

    public FileReaderThread(IControl control, Handler handler, String filePath, String encoding) {
        this.control = control;
        this.handler = handler;
        this.filePath = filePath;
        this.encoding = encoding;
    }

    public void run() {
        // show progress
        Message msg = new Message();
        msg.what = MainConstant.HANDLER_MESSAGE_SHOW_PROGRESS;
        handler.handleMessage(msg);

        msg = new Message();
        msg.what = MainConstant.HANDLER_MESSAGE_DISMISS_PROGRESS;

        try {
            IReader reader;
            String fileName = filePath.toLowerCase();

            // doc
            if (fileName.endsWith(MainConstant.FILE_TYPE_DOC)
                    || fileName.endsWith(MainConstant.FILE_TYPE_DOT)
            ) {
                reader = new DOCReader(control, filePath);
            }
            // docx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_DOCX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_DOTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_DOTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_DOCM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_XPS)
            ) {
                reader = new DOCXReader(control, filePath);
            }
            //
            else if (fileName.endsWith(MainConstant.FILE_TYPE_TXT)) {
                reader = new TXTReader(control, filePath, encoding);
            }
            // xls
            else if (fileName.endsWith(MainConstant.FILE_TYPE_XLS)
                    || fileName.endsWith(MainConstant.FILE_TYPE_XLT)
            ) {
                reader = new XLSReader(control, filePath);
            }
            // xlsx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_XLSX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_XLTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_XLTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_XLSM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_XLAM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_ODS)
            ) {
                reader = new XLSXReader(control, filePath);
            }
            // ppt
            else if (fileName.endsWith(MainConstant.FILE_TYPE_PPT)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POT)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPS)

            ) {

                reader = new PPTReader(control, filePath);
            }
            // pptx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_PPTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPSX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPSM)
            ) {
                reader = new PPTXReader(control, filePath);
            }

            // other
            else {
                reader = new TXTReader(control, filePath, encoding);
            }

            Message mesReader = new Message();
            mesReader.obj = reader;
            mesReader.what = MainConstant.HANDLER_MESSAGE_SEND_READER_INSTANCE;
            handler.handleMessage(mesReader);

            msg.obj = reader.getModel();
            reader.dispose();
            msg.what = MainConstant.HANDLER_MESSAGE_SUCCESS;
//            handler.handleMessage(msg);
        } catch (OutOfMemoryError eee) {
            Log.e(TAG, "FileReaderThread run(): Out of Memory Error" + eee);
            msg.what = MainConstant.HANDLER_MESSAGE_ERROR;
            msg.obj = eee;
        } catch (Exception ee) {
            Log.e(TAG, "FileReaderThread run(): Exception " + ee);
            msg.what = MainConstant.HANDLER_MESSAGE_ERROR;
            msg.obj = ee;
        } catch (AbortReaderError ee) {
            Log.e(TAG, "FileReaderThread run(): AbortReaderError" + ee);
            msg.what = MainConstant.HANDLER_MESSAGE_ERROR;
            msg.obj = ee;
        } finally {
            handler.handleMessage(msg);
            control = null;
            handler = null;
            encoding = null;
            filePath = null;
        }
    }

    private String encoding;

    private String filePath;

    private Handler handler;

    private IControl control;
}
