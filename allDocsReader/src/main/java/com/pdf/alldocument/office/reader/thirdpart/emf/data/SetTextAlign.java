// Copyright 2002, FreeHEP.

package com.pdf.alldocument.office.reader.thirdpart.emf.data;

import com.pdf.alldocument.office.reader.thirdpart.emf.EMFConstants;
import com.pdf.alldocument.office.reader.thirdpart.emf.EMFInputStream;
import com.pdf.alldocument.office.reader.thirdpart.emf.EMFRenderer;
import com.pdf.alldocument.office.reader.thirdpart.emf.EMFTag;

import java.io.IOException;

/**
 * SetTextAlign TAG.
 * 
 * @author Mark Donszelmann
 * @version $Id: SetTextAlign.java 10367 2007-01-22 19:26:48Z duns $
 */
public class SetTextAlign extends EMFTag implements EMFConstants
{

    private int mode;

    public SetTextAlign()
    {
        super(22, 1);
    }

    public SetTextAlign(int mode)
    {
        this();
        this.mode = mode;
    }

    public EMFTag read(int tagID, EMFInputStream emf, int len) throws IOException
    {

        return new SetTextAlign(emf.readDWORD());
    }

    public String toString()
    {
        return super.toString() + "\n  mode: " + mode;
    }

    /**
     * displays the tag using the renderer
     *
     * @param renderer EMFRenderer storing the drawing session data
     */
    public void render(EMFRenderer renderer)
    {
        renderer.setTextAlignMode(mode);
    }
}
