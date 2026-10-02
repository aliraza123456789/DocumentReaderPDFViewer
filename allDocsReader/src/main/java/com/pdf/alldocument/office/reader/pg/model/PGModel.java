/*
 * 文件名称:          PGModel.java
 *  
 * 编译器:            android2.2
 * 时间:              下午5:11:46
 */
package com.pdf.alldocument.office.reader.pg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pdf.alldocument.office.reader.java.awt.Dimension;
import com.pdf.alldocument.office.reader.pg.model.tableStyle.TableStyle;
import com.pdf.alldocument.office.reader.simpletext.model.IDocument;
import com.pdf.alldocument.office.reader.simpletext.model.STDocument;

public class PGModel
{

    public PGModel()
    {
        doc = new STDocument();
        slides = new ArrayList<PGSlide>();
        slideMasters = new ArrayList<PGSlide>();
        slideNumberOffset = 0;
        omitTitleSlide = false;
    }
    
    /**
     * 添加slide
     * @param slide
     */
    public synchronized void appendSlide(PGSlide slide)
    {
        if (slides == null)
        {
            return;
        }
        slides.add(slide);
    }
    
    
    /**
     * 根据给定的index获得slide
     */
    public PGSlide getSlide(int index)
    {
        if (index < 0 || index >= slides.size())
        {
            return null;
        }
        return slides.get(index);
    }
    
    /**
     * 根据给定的slide no获得slide
     */
    public PGSlide getSlideForSlideNo(int slideNo)
    {
        for (PGSlide slide : slides)
        {
            if (slide.getSlideNo() == slideNo)
            {
                return slide;
            }
        }
        return null;
    }
    
    

    public int getRealSlideCount()
    {
       if (slides != null)
       {
           return slides.size();
       }
       return  0;
    }

    public int getSlideCount()
    {
        return total;
    }
    
    /**
     * set slide total number
     * @param total
     */
    public void setSlideCount(int total)
    {
        this.total = total;
    }

    /**
     * @return Returns the doc.
     */
    public IDocument getRenderersDoc()
    {
        return doc;
    }
    
    /**
     * @return Returns the pageSize.
     */
    public Dimension getPageSize()
    {
        return pageSize;
    }

    /**
     * @param pageSize The pageSize to set.
     */
    public void setPageSize(Dimension pageSize)
    {
        this.pageSize = pageSize;
    }
    

    public int appendSlideMaster(PGSlide master)
    {
        int size = slideMasters.size();
        slideMasters.add(master);
        return size;
    }   
    

    public PGSlide getSlideMaster(int index)
    {
        if (index < 0 || index >= slideMasters.size())
        {
            return null;
        }
        return slideMasters.get(index);
    }
    

    public int getSlideMasterCount()
    {
        if (slideMasters == null)
        {
            return 0;
        }
        return slideMasters.size();
    }
    

    public void putTableStyle(String styleID, TableStyle tableStyle)
    {
        if(tableStyleMap == null)
        {
            tableStyleMap = new HashMap<String, TableStyle>();
        }
        
        if(styleID != null && tableStyle != null)
        {
            tableStyleMap.put(styleID, tableStyle);
        }
    }
    

    public TableStyle getTableStyle(String styleID)
    {
        if(tableStyleMap != null && styleID != null)
        {
            return tableStyleMap.get(styleID);
        }
        
        return null;
    }
    

    public int getSlideNumberOffset()
    {
        return slideNumberOffset;
    }
    
    /**
     * 
     * @param slideNumberOffset
     */
    public void setSlideNumberOffset(int slideNumberOffset)
    {
        this.slideNumberOffset = slideNumberOffset;
    }
    
    /***
     * 
     * @return
     */
    public boolean isOmitTitleSlide()
    {
        return omitTitleSlide;
    }
    
    public void setOmitTitleSlide(boolean omitTitleSlide)
    {
        this.omitTitleSlide = omitTitleSlide;
    }
    
    /**
     * 
     */
    public synchronized void dispose()
    {   
        if (doc != null)
        {
            doc.dispose();
            doc = null;
        }
        if (slides != null)
        {
            for (PGSlide slide : slides)
            {
                slide.dispose();
            }
            slides.clear();
            slides = null;
        }
        if (slideMasters != null)
        {
            for (PGSlide master : slideMasters)
            {
                master.dispose();
            }
            slideMasters.clear();
            slideMasters = null;
        }
        
        if(tableStyleMap != null)
        {
            tableStyleMap.clear();
            tableStyleMap = null;
        }
    }

    // 绘制用到的共享复杂文本model
    private IDocument doc;
    // slide
    private List<PGSlide> slides;
    // 纸张大小
    private Dimension pageSize;
    //
    private List<PGSlide> slideMasters;
    // slide total number
    private int total = 0;
    
    private Map<String, TableStyle> tableStyleMap;
    private int slideNumberOffset;
    private boolean omitTitleSlide;
}
