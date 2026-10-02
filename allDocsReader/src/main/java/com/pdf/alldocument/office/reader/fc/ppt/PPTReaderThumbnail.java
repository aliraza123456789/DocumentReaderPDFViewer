/*
 * 文件名称:          PTTReaderThumbnail.java
 *
 * 编译器:            android2.2
 * 时间:              下午4:18:41
 */
package com.pdf.alldocument.office.reader.fc.ppt;

import android.graphics.Bitmap;

import com.pdf.alldocument.office.reader.constant.MainConstant;

public class PPTReaderThumbnail {


    private static PPTReaderThumbnail kit = new PPTReaderThumbnail();

    public static PPTReaderThumbnail instance() {
        return kit;
    }

    public Bitmap getThumbnail(String filePath) {
        try {
            String fileName = filePath.toLowerCase();
          /*  // ppt
            if (fileName.endsWith(MainConstant.FILE_TYPE_PPT) || fileName.endsWith(MainConstant.FILE_TYPE_POT)) {
                return getThumbnailForPPT(filePath);
            }
            // pptx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_PPTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POTM)) {
                return getThumbnailForPPT(filePath);
            }*/
            if (fileName.endsWith(MainConstant.FILE_TYPE_PPT)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POT)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POTX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_POTM)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPS)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPSX)
                    || fileName.endsWith(MainConstant.FILE_TYPE_PPSM)) {
                return getThumbnailForPPT(filePath);
            }

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Bitmap getThumbnailForPPT(String filePath) throws Exception {

        return null;
    }

    private Bitmap getThumbnailForPPTX(String filePath) throws Exception {
        return null;
    }
}
