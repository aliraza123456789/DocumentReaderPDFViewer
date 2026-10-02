package com.pdf.alldocument.office.reader.officereader;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.pdf.alldocument.office.reader.R;
import com.pdf.alldocument.office.reader.common.IOfficeToPicture;
import com.pdf.alldocument.office.reader.constant.DocsReaderConstant;
import com.pdf.alldocument.office.reader.constant.EventConstant;
import com.pdf.alldocument.office.reader.constant.MainConstant;
import com.pdf.alldocument.office.reader.constant.wp.WPViewConstant;
import com.pdf.alldocument.office.reader.res.ResKit;
import com.pdf.alldocument.office.reader.system.IControl;
import com.pdf.alldocument.office.reader.system.IMainFrame;
import com.pdf.alldocument.office.reader.system.MainControl;
import com.pdf.alldocument.office.reader.system.beans.pagelist.IPageListViewListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

public class ActivityFilesReader extends AppCompatActivity implements IMainFrame {
    private static final String TAG = "ActivityFilesReader";

    private boolean isDispose;

    /**
     * True only while the user has explicitly asked for full screen via the
     * toolbar menu. The activity always draws edge-to-edge, so this flag only
     * controls whether the system bar insets are applied as padding.
     */
    private boolean isFullscreen;


    private boolean marked;

    private final int applicationType = -1;

    private MainControl control;

    private AppFrame appFrame;

    private Toast toast;

    //whether write log to temporary file
    private boolean writeLog = true;
    //open file to get thumbnail, or not
    private boolean isThumbnail;
    //view background
    private Object bg = Color.GRAY;

    public static final String KEY_INTENT_SELECTED_FILE_PATH = "KEY_INTENT_SELECTED_FILE_PATH";

    private String tempFilePath;
    private File tempExportedImageFile;
    private String filePath = "";
    private String fileName = "";
    Intent intent;

    MaterialCardView cvContainerNative;
    MaterialToolbar toolbar;

    public static Intent launchDocFromPath(Context context, String filePath) {
        Intent intent = new Intent(context, ActivityFilesReader.class);
        intent.putExtra(KEY_INTENT_SELECTED_FILE_PATH, filePath);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private void initTheme() {
        File tempFile = new File(filePath);
        if (tempFile.exists() && !tempFile.isDirectory()) {
            fileName = tempFile.getName();
            if (!TextUtils.isEmpty(fileName)) {
//                   PowerPoint Presentation
                if (fileName.endsWith(".ppt")
                        || fileName.endsWith(".pptx")
                        || fileName.endsWith(".pptm")
                        || fileName.endsWith(".pot")
                        || fileName.endsWith(".potx")
                        || fileName.endsWith(".potm")
                        || fileName.endsWith(".pps")
                        || fileName.endsWith(".ppsx")
                        || fileName.endsWith(".ppsm")
                ) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_PPT_Files);
                }
//                    Excel Spreadsheet
                else if (fileName.endsWith(".xls")
                        || fileName.endsWith(".xlsx")
                        || fileName.endsWith(".xlsm")
                        || fileName.endsWith(".xlsb")
                        || fileName.endsWith(".xlt")
                        || fileName.endsWith(".xltx")
                        || fileName.endsWith(".xltm")
                        || fileName.endsWith(".xlam")
                ) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_XLX_Files);
                }
//                    Word Document
                else if (fileName.endsWith(".doc")
                        || fileName.endsWith(".docx")
                        || fileName.endsWith(".docm")
                        || fileName.endsWith(".dot")
                        || fileName.endsWith(".dotx")
                        || fileName.endsWith(".dotm")
                ) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_All_Files);
                } else if (fileName.endsWith(".xml")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_XML_Files);
                } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_HTML_Files);
                } else if (fileName.endsWith(".csv")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_CSV_Files);
                } else if (fileName.endsWith(".java")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_JAVA_Files);
                } else if (fileName.endsWith(".json")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_JSON_Files);
                } else if (fileName.endsWith(".log")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_LOG_Files);
                } else if (fileName.endsWith(".txt")) {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_TXT_Files);
                } else {
                    setTheme(R.style.Theme_AllDocReader_Toolbar_All_Files);
                }
            } else {
                setTheme(R.style.Theme_AllDocReader_Toolbar_All_Files);
            }
        }


    }

    /**
     * Pads the root content by the system bar + display cutout insets so the
     * toolbar and the ad card stay reachable, while the window background
     * still draws behind the bars.
     */
    private void applyEdgeToEdgeInsets() {
        View root = findViewById(R.id.content_files_reader);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            if (isFullscreen) {
                view.setPadding(0, 0, 0, 0);
            } else {
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void showSystemUI() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_reader);
        // Draw behind the system bars...
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // ...but keep the bars visible by default and pad the content out of
        // their way. Full screen stays an explicit, opt-in mode.
        applyEdgeToEdgeInsets();
        showSystemUI();
        cvContainerNative = findViewById(R.id.container_native_viewer_all);
        try {
            initFilePath();

            control = new MainControl(this);
            appFrame = new AppFrame(getApplicationContext());

            appFrame.post(() -> {

                try {
                    File file = new File(filePath);
                    if (file.exists() && !file.isDirectory() && file.length() > 0) {
                        iniToolbar();
                        setupToolbar();
                        control.openFile(filePath);
                    } else {
                        dialogError(getString(R.string.title_un_able_open),
                                filePath + " " + getString(R.string.msg_un_able_open));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception:", e);
                    dialogError(getString(R.string.title_un_able_open), "File " + getString(R.string.msg_un_able_open));
                }

            });
            FrameLayout container = findViewById(R.id.container_viewer);
            container.addView(appFrame);

            control.setOffictToPicture(new IOfficeToPicture() {
                public Bitmap getBitmap(int componentWidth, int componentHeight) {
                    if (componentWidth == 0 || componentHeight == 0) {
                        return null;
                    }
                    if (bitmap == null || bitmap.getWidth() != componentWidth || bitmap.getHeight() != componentHeight) {
                        if (bitmap != null) {
                            bitmap.recycle();
                        }
                        bitmap = Bitmap.createBitmap(componentWidth, componentHeight, Config.ARGB_8888);

                    }
                    return bitmap;
                }
                public void callBack(Bitmap bitmap) {
                    new Thread(() -> {
                        if (isDispose) {
                            return;
                        }
                        saveBitmapToFile(bitmap);
                    }).start();
                }
                private Bitmap bitmap;
                @Override
                public void setModeType(byte modeType) {}
                @Override
                public byte getModeType() {
                    return VIEW_CHANGE_END;
                }
                @Override
                public boolean isZoom() {
                    return false;
                }
                @Override
                public void dispose() {}
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception:", e);
            dialogError(getString(R.string.title_un_able_open), "File " + getString(R.string.msg_un_able_open));
        }
    }

    private void dialogError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }

    private void dialogExitFile() {
        finish();
    }

    private void hideSystemUI() {
        isFullscreen = true;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        View root = findViewById(R.id.content_files_reader);
        if (root != null) {
            ViewCompat.requestApplyInsets(root);
        }
    }

    private void exitFullscreen() {
        isFullscreen = false;
        showSystemUI();
        View root = findViewById(R.id.content_files_reader);
        if (root != null) {
            ViewCompat.requestApplyInsets(root);
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.tbAllFiles);
        toolbar.setTitle(fileName);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_reader, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.menu_bt_fullScreen) {
            if (isFullscreen) {
                exitFullscreen();
            } else {
                hideSystemUI();
            }
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }
    public void onBackPressed() {
        dialogExitFile();
    }
    private void iniToolbar() {
        setTitle(fileName);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_default);
        }
        bg = ContextCompat.getColor(this, R.color.black_500);
    }
    private void initFilePath() {
        intent = getIntent();
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(DocsReaderConstant.KEY_INTENT_SELECTED_FILE_PATH)) {
            filePath = intent.getStringExtra(DocsReaderConstant.KEY_INTENT_SELECTED_FILE_PATH);
        }
        initTheme();
    }
    private void saveBitmapToFile(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (tempFilePath == null) {
            tempFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(tempFilePath + File.separatorChar + "tempPic");
            if (!file.exists()) {
                file.mkdir();
            }
            tempFilePath = file.getAbsolutePath();
        }
        tempExportedImageFile = new File(tempFilePath + File.separatorChar + "export_image.jpg");
        try {
            if (tempExportedImageFile.exists()) {
                tempExportedImageFile.delete();
            }
            tempExportedImageFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(tempExportedImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    protected void onDestroy() {
        dispose();
        super.onDestroy();
    }
    public void showProgressBar(boolean visible) {
        setProgressBarIndeterminateVisibility(visible);
    }
    public boolean isShowZoomingMsg() {
        return true;
    }
    public boolean isPopUpErrorDlg() {
        return true;
    }
    public void setFindBackForwardState(boolean state) {}
    public void fileShare() {
        ArrayList<Uri> list = new ArrayList<Uri>();
        File file = new File(filePath);
        list.add(Uri.fromFile(file));
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_STREAM, list);
        intent.setType("application/octet-stream");
        startActivity(Intent.createChooser(intent, getResources().getText(R.string.sys_share_title)));
    }
    private void markFile() {
        marked = !marked;
    }
    public Dialog onCreateDialog(int id) {
        return control.getDialog(this, id);
    }
    public void updateToolsbarStatus() {
        if (appFrame == null || isDispose) {
            return;
        }
        int count = appFrame.getChildCount();
    }
    public IControl getControl() {
        return this.control;
    }
    public int getApplicationType() {
        return this.applicationType;
    }
    public String getFilePath() {
        return filePath;
    }
    public Activity getActivity() {
        return this;
    }
    public boolean doActionEvent(int actionID, Object obj) {
        try {
            switch (actionID) {
                case EventConstant.SYS_RESET_TITLE_ID:
                    setTitle((String) obj);
                    break;

                case EventConstant.SYS_ONBACK_ID:
                    onBackPressed();
                    break;

                case EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS:
                    updateToolsbarStatus();
                    break;

                case EventConstant.SYS_HELP_ID:
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources()
                            .getString(R.string.sys_url_wxiwei)));
                    startActivity(intent);
                    break;

                case EventConstant.APP_FIND_ID:

                    break;

                case EventConstant.APP_SHARE_ID:
                    fileShare();
                    break;

                case EventConstant.FILE_MARK_STAR_ID:
                    markFile();
                    break;

                case EventConstant.APP_FINDING:
                    String content = ((String) obj).trim();
                    if (content.length() > 0 && control.getFind().find(content)) {
                        setFindBackForwardState(true);
                    } else {
                        setFindBackForwardState(false);
                        toast.setText(getLocalString("DIALOG_FIND_NOT_FOUND"));
                        toast.show();
                    }
                    break;

                case EventConstant.APP_FIND_BACKWARD:
                    if (!control.getFind().findBackward()) {
                        toast.setText(getLocalString("DIALOG_FIND_TO_BEGIN"));
                        toast.show();
                    }
                    break;

                case EventConstant.APP_FIND_FORWARD:
                    if (!control.getFind().findForward()) {

                        toast.setText(getLocalString("DIALOG_FIND_TO_END"));
                        toast.show();
                    }
                    break;


                case EventConstant.APP_DRAW_ID:

                    control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_CALLOUTDRAW);
                    appFrame.post(() -> control.actionEvent(EventConstant.APP_INIT_CALLOUTVIEW_ID, null));

                    break;

                case EventConstant.APP_BACK_ID:

                    control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_NORMAL);
                    break;

                case EventConstant.APP_PEN_ID:
                    if ((Boolean) obj) {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_CALLOUTDRAW);
                        appFrame.post(() -> control.actionEvent(EventConstant.APP_INIT_CALLOUTVIEW_ID, null));
                    } else {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_NORMAL);
                    }
                    break;

                case EventConstant.APP_ERASER_ID:
                    if ((Boolean) obj) {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_CALLOUTERASE);
                    } else {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_NORMAL);
                    }
                    break;

                case EventConstant.APP_COLOR_ID:

                    break;

                default:
                    return false;
            }
        } catch (Exception e) {
            control.getSysKit().getErrorKit().writerLog(e);
        }
        return true;
    }
    public void openFileFinish() {
        View gapView = new View(getApplicationContext());
        gapView.setBackgroundColor(Color.GRAY);
        appFrame.addView(gapView, new LayoutParams(LayoutParams.MATCH_PARENT, 1));
        View app = control.getView();
        appFrame.addView(app, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }
    public int getBottomBarHeight() {
        return 0;
    }
    public int getTopBarHeight() {
        return 0;
    }
    public boolean onEventMethod(View v, MotionEvent e1, MotionEvent e2, float xValue, float yValue, byte eventMethodType) {
        return false;
    }
    public void changePage() {}
    public String getAppName() {
        return getString(R.string.app_name);
    }
    public boolean isDrawPageNumber() {
        return true;
    }
    public boolean isTouchZoom() {
        return true;
    }
    public byte getWordDefaultView() {
        return WPViewConstant.PAGE_ROOT;
    }
    public boolean isZoomAfterLayoutForWord() {
        return true;
    }
    public void error(int errorCode) {
        Log.e(TAG, "error: " + errorCode);
    }
    @Override
    public void fullScreen(boolean fullscreen) {

    }
    public void destroyEngine() {
        super.onBackPressed();
    }
    public String getLocalString(String resName) {return ResKit.instance().getLocalString(resName);}
    @Override
    public void changeZoom() {}
    @Override
    public boolean isShowPasswordDlg() {
        return true;
    }
    @Override
    public boolean isShowProgressBar() {
        return true;
    }
    @Override
    public boolean isShowFindDlg() {
        return true;
    }
    @Override
    public boolean isShowTXTEncodeDlg() {
        return true;
    }
    public String getTXTDefaultEncode() {
        return "GBK";
    }
    @Override
    public void completeLayout() {}
    @Override
    public boolean isChangePage() {
        return true;
    }
    public void setWriteLog(boolean saveLog) {
        this.writeLog = saveLog;
    }
    public boolean isWriteLog() {
        return writeLog;
    }
    public void setThumbnail(boolean isThumbnail) {
        this.isThumbnail = isThumbnail;
    }
    public Object getViewBackground() {
        return bg;
    }
    public void setIgnoreOriginalSize(boolean ignoreOriginalSize) {}
    public boolean isIgnoreOriginalSize() {
        return false;
    }
    public byte getPageListViewMovingPosition() {
        return IPageListViewListener.Moving_Horizontal;
    }
    public boolean isThumbnail() {
        return isThumbnail;
    }
    public void updateViewImages(List<Integer> viewList) {}
    public File getTemporaryDirectory() {
        File file = getExternalFilesDir(null);
        if (file != null) {
            return file;
        } else {
            return getFilesDir();
        }
    }
    public void dispose() {
        isDispose = true;
        if (tempExportedImageFile != null && tempExportedImageFile.exists()) {
            tempExportedImageFile.delete();
        }
        if (control != null) {
            control.dispose();
            control = null;
        }
        if (appFrame != null) {
            appFrame = null;
        }
    }
}
