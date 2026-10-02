/*
 * 文件名称:          ViewContainer.java
 *
 * 编译器:            android2.2
 * 时间:              下午2:03:24
 */
package com.pdf.alldocument.office.reader.simpletext.view;

import android.util.Log;

import com.pdf.alldocument.office.reader.constant.wp.WPModelConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * paragraph view container
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2012-11-20
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class ViewContainer {


    private String TAG = "TEST_TAG_ViewContainer";

    public ViewContainer() {
        paras = new ArrayList<IView>();
    }

    /**
     *
     */
    public void add(IView para) {

        Log.d(TAG, "add: Method called , check it");
        if (para != null && para.getEndOffset(null) < WPModelConstant.HEADER) {
            paras.add(para);
        }
    }

    /**
     *
     */
    public void sort() {
        try {
            Collections.sort(paras, (prePara, nextPara) -> prePara.getEndOffset(null) <= nextPara.getStartOffset(null) ? -1 : 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public IView getParagraph(long offset, boolean isBack) {
        int size = paras.size();
        if (size == 0 || offset < 0 || offset >= paras.get(size - 1).getEndOffset(null)) {
            return null;
        }
        int max = size;
        int min = 0;
        IView view;
        long start;
        long end;
        int mid;
        while (true) {
            mid = (max + min) / 2;
            view = paras.get(mid);
            start = view.getStartOffset(null);
            end = view.getEndOffset(null);
            if (offset >= start && offset < end) {
                break;
            } else if (start > offset) {
                max = mid - 1;
            } else if (end <= offset) {
                min = mid + 1;
            }
        }
        return view;
    }

    /**
     *
     */
    public void clear() {
        if (paras != null) {
            paras.clear();
        }
    }

    /**
     *
     */
    public void dispose() {
        if (paras != null) {
            paras.clear();
            paras = null;
        }
    }

    // 
    private List<IView> paras;
}
