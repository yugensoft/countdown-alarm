package com.yugensoft.countdownalarm;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

/**
 * Created by yugensoft on 28/10/2016.
 */
public class RoundedBackgroundSpan extends ReplacementSpan {
    private final int mPadding = 15;
    private int mBackgroundColor;
    private int mTextColor;

    private String mDrawnText;

    public RoundedBackgroundSpan(int backgroundColor, int textColor) {
        super();
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        String textToDraw = text.subSequence(start, end).toString();
        mDrawnText = textToDraw;
        return (int) (mPadding + paint.measureText(textToDraw) + mPadding);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
    {
        String textToDraw = text.subSequence(start, end).toString();
        float width = paint.measureText(textToDraw);
        RectF rect = new RectF(x, top+mPadding, x + width + 2*mPadding, bottom);
        paint.setColor(mBackgroundColor);
        canvas.drawRoundRect(rect, mPadding, mPadding, paint);
        paint.setColor(mTextColor);
        canvas.drawText(text, start, end, x+mPadding, y, paint);
        mDrawnText = textToDraw;
    }

    public String getDrawnText() {
        return mDrawnText;
    }


}
