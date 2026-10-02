package com.pdf.alldocument.office.reader.officereader;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;


public class AppFrame extends LinearLayout {

    public AppFrame(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.WHITE);
    }

    public void dispose() {
        removeAllViews();
    }
}
