package com.yugensoft.countdownalarm;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

/**
 * Created by yugensoft on 28/10/2016.
 */
public class RoundedBackgroundSpan extends ReplacementSpan {
    private final int mPadding = 10;
    private int mBackgroundColor;
    private int mTextColor;

    public RoundedBackgroundSpan(int backgroundColor, int textColor) {
        super();
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) (mPadding + paint.measureText(text.subSequence(start, end).toString()) + mPadding);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
    {
        float width = paint.measureText(text.subSequence(start, end).toString());
        RectF rect = new RectF(x, top+mPadding, x + width + 2*mPadding, bottom);
        paint.setColor(mBackgroundColor);
        canvas.drawRoundRect(rect, mPadding, mPadding, paint);
        paint.setColor(mTextColor);
        canvas.drawText(text, start, end, x+mPadding, y, paint);
    }
}
