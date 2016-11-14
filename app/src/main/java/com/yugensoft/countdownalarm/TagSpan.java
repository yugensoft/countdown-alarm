package com.yugensoft.countdownalarm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.text.style.ReplacementSpan;

import static android.R.attr.textColor;

/**
 * Special TagSpan, based off RoundedRectangleSpan, which stores associated Tag
 */
public class TagSpan extends ReplacementSpan {
    private final int mPadding = 15;
    private int mBackgroundColor;
    private int mTextColor;
    private Tag mTag;

    private String mDrawnText;

    public TagSpan(Tag tag, Context context) {
        super();

        // set colors according to tag type
        switch(tag.getTagType()){
            case UNKNOWN:
                mBackgroundColor = Color.BLACK;
                mTextColor = Color.WHITE;
                break;
            case COUNTDOWN:
                mBackgroundColor = ContextCompat.getColor(context, R.color.countdown_tag_background_color);
                mTextColor = ContextCompat.getColor(context, R.color.countdown_tag_foreground_color);
                break;
            case COUNTUP:
                mBackgroundColor = ContextCompat.getColor(context, R.color.countup_tag_background_color);
                mTextColor = ContextCompat.getColor(context, R.color.countup_tag_foreground_color);
                break;
            case TODAYS_DATE:
                mBackgroundColor = ContextCompat.getColor(context, R.color.date_tag_background_color);
                mTextColor = ContextCompat.getColor(context, R.color.date_tag_foreground_color);
                break;
            default:
                throw new RuntimeException();
        }
        mTag = tag;
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


    public Tag getTag() {
        return mTag;
    }
}
