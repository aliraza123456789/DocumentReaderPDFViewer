package com.pdf.alldocument.office.reader.system;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.pdf.alldocument.office.reader.R;
import com.pdf.alldocument.office.reader.common.IOfficeToPicture;
import com.pdf.alldocument.office.reader.common.ISlideShow;
import com.pdf.alldocument.office.reader.common.picture.PictureKit;
import com.pdf.alldocument.office.reader.constant.EventConstant;
import com.pdf.alldocument.office.reader.constant.MainConstant;
import com.pdf.alldocument.office.reader.fc.doc.TXTKit;
import com.pdf.alldocument.office.reader.pg.control.PGControl;
import com.pdf.alldocument.office.reader.pg.model.PGModel;
import com.pdf.alldocument.office.reader.simpletext.model.IDocument;
import com.pdf.alldocument.office.reader.ss.control.SSControl;
import com.pdf.alldocument.office.reader.ss.model.baseModel.Workbook;
import com.pdf.alldocument.office.reader.wp.control.WPControl;

public class MainControl extends AbstractControl {

    public MainControl(IMainFrame frame) {
        this.frame = frame;
        uncaught = new AUncaughtExceptionHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(uncaught);
        sysKit = new SysKit(this);
        init();
    }

    private void init() {
        // listener
        initListener();
    }

    private void initListener() {
        onKeyListener = (dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.dismiss();
                isCancel = true;
                if (reader != null) {
                    reader.abortReader();
                    reader.dispose();
                }
//                getMainFrame().destroyEngine();
                getActivity().onBackPressed();
                return true;
            }
            return false;
        };

        handler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                if (isCancel) {
                    return;
                }
                final Message message = msg;
                switch (msg.what) {
                    case MainConstant.HANDLER_MESSAGE_SUCCESS:
                        post(() -> {
                            try {
                                if (getMainFrame().isShowProgressBar()) {
                                    dismissProgressDialog();
                                }
                                createApplication(message.obj);
                            } catch (Exception e) {
                                sysKit.getErrorKit().writerLog(e, true);
                            }
                        });
                        break;

                    case MainConstant.HANDLER_MESSAGE_ERROR:
                        post(() -> {
                            dismissProgressDialog();
                            if (message.obj instanceof Throwable) {
                                sysKit.getErrorKit().writerLog((Throwable) message.obj, true);
                            }

                        });
                        break;

                    case MainConstant.HANDLER_MESSAGE_SHOW_PROGRESS:
                        if (getMainFrame().isShowProgressBar()) {
                            post(() -> {
                                progressDialog = new ProgressDialog(getActivity(), R.style.mTheme_DialogDefault);
                                progressDialog.setTitle(progressDialog.getContext().getString(R.string.wait));
                                progressDialog.setMessage(progressDialog.getContext().getString(R.string.msg_rendering_files));
                                progressDialog.setCancelable(false);
                                progressDialog.setIndeterminate(false);
                                progressDialog.setOnCancelListener(null);
                                progressDialog.setOnKeyListener(onKeyListener);
                                progressDialog.show();
                            });
                        }

                        break;

                    case MainConstant.HANDLER_MESSAGE_DISMISS_PROGRESS:
                        post(() -> dismissProgressDialog());
                        break;

                    case MainConstant.HANDLER_MESSAGE_SEND_READER_INSTANCE:
                        reader = (IReader) msg.obj;
                        break;
                }
            }
        };
    }

    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void createApplication(Object obj) throws Exception {
        if (obj == null) {
            throw new Exception("Document with password");
        }
        // word
        if (applicationType == MainConstant.APPLICATION_TYPE_WP) {
            appControl = new WPControl(this, (IDocument) obj, filePath);
        }
        // excel
        else if (applicationType == MainConstant.APPLICATION_TYPE_SS) {
            appControl = new SSControl(this, (Workbook) obj, filePath);
        }
        // powerpoint
        else if (applicationType == MainConstant.APPLICATION_TYPE_PPT) {
            appControl = new PGControl(this, (PGModel) obj, filePath);
        }
        View view = appControl.getView();
        if (view != null) {
            Object bg = frame.getViewBackground();
            if (bg != null) {
                if (bg instanceof Integer) {
                    view.setBackgroundColor((Integer) bg);
                } else if (bg instanceof Drawable) {
                    view.setBackground((Drawable) bg);
                }
            }
        }

        handler.post(() -> frame.openFileFinish());
        PictureKit.instance().setDrawPictrue(true);

        //use a handler as easiest method to post a Runnable Delayed.
        //we cannot check hardware-acceleration directly as it will return reasonable results after attached to Window.
        handler.post(() -> {
            //now lets check for HardwareAcceleration since it is only avaliable since ICS.
            // 14 = ICS_VERSION_CODE
          /*  try {
                View contentView = getView();
                //use reflection to get that Method
                Method isHardwareAccelerated = contentView.getClass().getMethod("isHardwareAccelerated", null);
                Object o = isHardwareAccelerated.invoke(contentView, null);
                if (o instanceof Boolean && (Boolean) o) {
                    //ok we're shure that HardwareAcceleration is on.
                    //Now Try to switch it off:
                    Method setLayerType = contentView.getClass().getMethod("setLayerType", int.class, android.graphics.Paint.class);
                    int LAYER_TYPE_SOFTWARE = contentView.getClass().getField("LAYER_TYPE_SOFTWARE").getInt(null);
                    setLayerType.invoke(contentView, LAYER_TYPE_SOFTWARE, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            actionEvent(EventConstant.SYS_SET_PROGRESS_BAR_ID, false);
            // 初始化数
            actionEvent(EventConstant.SYS_INIT_ID, null);

            getView().postInvalidate();
        });

    }

    /**
     *
     */
    public boolean openFile(String filePath) {
        this.filePath = filePath;
        String fileName = filePath.toLowerCase();
        // word
        if (
                fileName.endsWith(MainConstant.FILE_TYPE_DOC)
                || fileName.endsWith(MainConstant.FILE_TYPE_DOCX)
                || fileName.endsWith(MainConstant.FILE_TYPE_DOCM)
                || fileName.endsWith(MainConstant.FILE_TYPE_TXT)
                || fileName.endsWith(MainConstant.FILE_TYPE_DOT)
                || fileName.endsWith(MainConstant.FILE_TYPE_DOTX)
                || fileName.endsWith(MainConstant.FILE_TYPE_DOTM)
                || fileName.endsWith(MainConstant.FILE_TYPE_WPS)
                || fileName.endsWith(MainConstant.FILE_TYPE_XPS)
                || fileName.endsWith(MainConstant.FILE_TYPE_ODT)

        ) {
            applicationType = MainConstant.APPLICATION_TYPE_WP;
        }
        // excel
        else if (fileName.endsWith(MainConstant.FILE_TYPE_XLS)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLSX)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLSM)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLSB)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLT)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLTX)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLTM)
                || fileName.endsWith(MainConstant.FILE_TYPE_XLAM)

        ) {
            applicationType = MainConstant.APPLICATION_TYPE_SS;
        }
        // PowerPoint
        else if (fileName.endsWith(MainConstant.FILE_TYPE_PPT)
                || fileName.endsWith(MainConstant.FILE_TYPE_PPTX)
                || fileName.endsWith(MainConstant.FILE_TYPE_PPTM)
                || fileName.endsWith(MainConstant.FILE_TYPE_POT)
                || fileName.endsWith(MainConstant.FILE_TYPE_POTX)
                || fileName.endsWith(MainConstant.FILE_TYPE_POTM)
                || fileName.endsWith(MainConstant.FILE_TYPE_PPS)
                || fileName.endsWith(MainConstant.FILE_TYPE_PPSX)
                || fileName.endsWith(MainConstant.FILE_TYPE_PPSM)
        ) {
            applicationType = MainConstant.APPLICATION_TYPE_PPT;
        } else {
            applicationType = MainConstant.APPLICATION_TYPE_WP;
        }

        boolean isSupport = FileKit.instance().isSupport(fileName);
        // txt or no support
        if (fileName.endsWith(MainConstant.FILE_TYPE_TXT) || !isSupport) {
            TXTKit.instance().readText(this, handler, filePath);
        } else {
            new FileReaderThread(this, handler, filePath, null).start();
        }
        return true;

    }

    /**
     * 布局视图
     *
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void layoutView(int x, int y, int w, int h) {
        //appControl.layoutView(x, y, w, h);
    }

    /**
     * action派发
     *
     * @param actionID 动作ID
     * @param obj      动作ID的Value
     */
    public void actionEvent(int actionID, final Object obj) {
        if (actionID == EventConstant.SYS_READER_FINSH_ID) {
            if (reader != null) {
                if (appControl != null) {
                    appControl.actionEvent(actionID, obj);
                }
                reader.dispose();
                reader = null;
            }
        }
        if (frame == null || frame.doActionEvent(actionID, obj)) {
            return;
        }
        switch (actionID) {
            case MainConstant.HANDLER_MESSAGE_SUCCESS: {
                try {
                    Message msg = new Message();
                    msg.obj = obj;
                    reader.dispose();
                    msg.what = MainConstant.HANDLER_MESSAGE_SUCCESS;

                    handler.handleMessage(msg);
                } catch (Throwable e) {
                    sysKit.getErrorKit().writerLog(e);
                }
            }
            break;
            case EventConstant.SYS_SET_PROGRESS_BAR_ID:
              /*  if (handler != null) {
                    handler.post(new Runnable() {

                        public void run() {
                            if (!isDispose) {
                                frame.getActivity().setProgressBarIndeterminateVisibility((Boolean)obj);
                                frame.showProgressBar((Boolean) obj);
                            }
                        }
                    });
                }*/
                break;
            case EventConstant.TEST_REPAINT_ID:
                getView().postInvalidate();
                break;

            case EventConstant.SYS_SHOW_TOOLTIP:
                if (obj instanceof String) {
                   /*toast.setText((String) obj);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();*/
                }
                break;

            case EventConstant.SYS_CLOSE_TOOLTIP:
//                toast.cancel();
                break;

 /*           case EventConstant.APP_CONTENT_SELECTED:  //selected interesting content
                appControl.actionEvent(actionID, obj);
                frame.updateToolsbarStatus();
                break;
*/
            case EventConstant.APP_ABORTREADING:
                if (reader != null) {
                    reader.abortReader();
                }
                break;

            case EventConstant.SYS_START_BACK_READER_ID:
                if (handler != null) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            if (!isDispose) {
//                                frame.getActivity().setProgressBarIndeterminateVisibility(true);
                                frame.showProgressBar(true);
                            }
                        }
                    });
                }
                break;

            case EventConstant.SYS_READER_FINSH_ID:
                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!isDispose) {
//                                frame.getActivity().setProgressBarIndeterminateVisibility(false);
                                frame.showProgressBar(false);
                            }
                        }
                    });
                }
                break;

            case EventConstant.TXT_DIALOG_FINISH_ID:
                TXTKit.instance().reopenFile(this, handler, filePath, (String) obj);
                break;

            case EventConstant.TXT_REOPNE_ID:
                String[] strings = (String[]) obj;
                if (strings.length == 2) {
                    this.filePath = strings[0];
                    applicationType = MainConstant.APPLICATION_TYPE_WP;
                    TXTKit.instance().reopenFile(this, handler, filePath, strings[1]);
                }
                break;

            default:
                if (appControl != null) {
                    appControl.actionEvent(actionID, obj);
                }
        }
    }

    /**
     *
     */
    public IFind getFind() {
        return appControl.getFind();
    }


    /**
     * 得到action的状态
     *
     * @return obj
     */
    public Object getActionValue(int actionID, Object obj) {
        if (actionID == EventConstant.SYS_FILEPAHT_ID) { // 文件路径
            return filePath;
        }
        if (appControl == null) {
            return null;
        }

        Object ret;
        if (actionID == EventConstant.APP_THUMBNAIL_ID
                || actionID == EventConstant.WP_PAGE_TO_IMAGE
                || actionID == EventConstant.APP_PAGEAREA_TO_IMAGE
                || actionID == EventConstant.PG_SLIDE_TO_IMAGE
                || actionID == EventConstant.PG_SLIDESHOW_SLIDESHOWTOIMAGE) {

            boolean b = PictureKit.instance().isDrawPictrue();
            boolean isThumbnail = frame.isThumbnail();
            PictureKit.instance().setDrawPictrue(true);
            if (actionID == EventConstant.APP_THUMBNAIL_ID) {
                frame.setThumbnail(true);
            }
            ret = appControl.getActionValue(actionID, obj);
            if (actionID == EventConstant.APP_THUMBNAIL_ID) {
                frame.setThumbnail(isThumbnail);
            }
            PictureKit.instance().setDrawPictrue(b);
        } else {
            ret = appControl.getActionValue(actionID, obj);
        }

        return ret;
    }


    /**
     * 获取应用组件
     */
    public View getView() {
        if (appControl == null) {
            return null;
        }
        return appControl.getView();
    }


    public Dialog getDialog(Activity activity, int id) {
        return null;
    }


    public IMainFrame getMainFrame() {
        return frame;
    }


    public Activity getActivity() {
        return frame.getActivity();
    }


    public IOfficeToPicture getOfficeToPicture() {
        return officeToPicture;
    }


    public ISlideShow getSlideShow() {
        return slideShow;
    }


    public IReader getReader() {
        return reader;
    }


    public byte getApplicationType() {
        return applicationType;
    }

    /**
     * set OfficeToPicture instance
     */
    public void setOffictToPicture(IOfficeToPicture opt) {
        officeToPicture = opt;
    }


    public void setSlideShow(ISlideShow slideshow) {
        this.slideShow = slideshow;
    }


    public SysKit getSysKit() {
        return this.sysKit;
    }


    public int getCurrentViewIndex() {
        return appControl.getCurrentViewIndex();
    }


    public void dispose() {
        Log.d(TAG, "MainControl dispose()");
        isDispose = true;
        if (appControl != null) {
            appControl.dispose();
            appControl = null;
        }
        if (reader != null) {
            reader.dispose();
            reader = null;
        }
        if (officeToPicture != null) {
            officeToPicture.dispose();
            officeToPicture = null;
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (slideShow != null) {
            slideShow = null;
        }

        frame = null;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (uncaught != null) {
            uncaught.dispose();
            uncaught = null;
        }
        onKeyListener = null;
        filePath = null;
        //disposeStatic();
        System.gc();
        if (sysKit != null) {
            sysKit.dispose();
        }
    }

    /**
     *
     */
    /*private void disposeStatic()
    {
        PictureManage.instance().dispose();
        HyperlinkManage.instance().dispose();
        ListManage.instance().dispose();
        //SysKit.instance().setControl(null);
        PGBulletText.instance().dispose();
        ViewFactory.dispose();
    }*/

    private static String TAG = "TAG_TEST_MAIN_Control";

    private boolean isDispose;

    private boolean isCancel;

    private byte applicationType = -1;

    private String filePath;

    private IMainFrame frame;

    private IOfficeToPicture officeToPicture;

    private ISlideShow slideShow;

    private IReader reader;

    private ProgressDialog progressDialog;

    private DialogInterface.OnKeyListener onKeyListener;

    private Handler handler;

    private IControl appControl;

    public SysKit sysKit;

    private AUncaughtExceptionHandler uncaught;
}
