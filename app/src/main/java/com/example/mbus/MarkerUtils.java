package com.example.mbus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

public class MarkerUtils {

    public static Bitmap createBusMarkerIcon(Context context, int number, int size, int circleColor, int textColor) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(circleColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(size / 2.2f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float x = size / 2f;
        float y = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2;

        canvas.drawText(String.valueOf(number), x, y, textPaint);

        return bitmap;
    }
}
