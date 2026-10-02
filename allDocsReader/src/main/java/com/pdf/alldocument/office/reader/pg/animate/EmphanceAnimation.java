package com.pdf.alldocument.office.reader.pg.animate;


public class EmphanceAnimation extends Animation
{
    private static final int ROTATION = 360;
    
    public EmphanceAnimation(ShapeAnimation shapeAnim)
    {
        super(shapeAnim);
        current = new AnimationInformation(null, 0, 0);
    }
    
    public EmphanceAnimation(ShapeAnimation shapeAnim, int duration)
    {
        super(shapeAnim, duration);
        current = new AnimationInformation(null, 0, 0);
    }
    
    public EmphanceAnimation(ShapeAnimation shapeAnim, int duration, int fps)
    {
        super(shapeAnim, duration, fps);
        current = new AnimationInformation(null, 0, 0);
    }
    
    /**
     * 定时器
     */
    public void animation(int frameIndex)
    {
        if(shapeAnim != null && shapeAnim.getAnimationType() == ShapeAnimation.SA_EMPH)
        {
            emphance(frameIndex * delay);
        }        
    }   
    /**
     * 
     */
    public void start()
    {
        super.start();
        current.setAlpha(255);
        current.setAngle(0);
        current.setProgress(0f);
    }
    
    /**
     * stop animation 
     */
    public void stop()
    {
        super.stop();
        
        if(current != null)
        {
            current.setAngle(0);
            current.setAlpha(255);
            current.setProgress(1f);
        }
    }
    
    private void emphance(int time)
    {
        if(current != null)
        {
            if(time < duration)
            {
                float progress = time / duration;
                current.setProgress(progress);
                current.setAngle((int)(ROTATION * progress));
            }
            else
            {
                status = Animation.AnimStatus_End;
                current.setProgress(1f);
                current.setAngle(0);
            }
        }
    }
}
