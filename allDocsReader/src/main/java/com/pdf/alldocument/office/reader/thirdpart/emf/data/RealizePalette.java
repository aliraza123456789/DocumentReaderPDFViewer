// Copyright 2002, FreeHEP.
package com.pdf.alldocument.office.reader.thirdpart.emf.data;

import com.pdf.alldocument.office.reader.thirdpart.emf.EMFInputStream;
import com.pdf.alldocument.office.reader.thirdpart.emf.EMFTag;

import java.io.IOException;

/**
 * RealizePalette TAG.
 * 
 * @author Mark Donszelmann
 * @version $Id: RealizePalette.java 10140 2006-12-07 07:50:41Z duns $
 */
public class RealizePalette extends EMFTag {

    public RealizePalette() {
        super(52, 1);
    }

    public EMFTag read(int tagID, EMFInputStream emf, int len)
            throws IOException {

        return this;
    }

}
