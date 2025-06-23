package com.example.mbus.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class MarkerUtils {

    private static int darkenColor(int color, float factor) {
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.rgb(Math.max(r, 0), Math.max(g, 0), Math.max(b, 0));
    }

    public static Bitmap createBusMarkerIcon(Context context, int busNumber, int sizePx, int baseColor, int textColor) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Círculo com a cor escurecida
        int darkerColor = darkenColor(baseColor, 0.8f); // 80% da luminosidade
        paint.setColor(darkerColor);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 4, paint); // círculo interior com margem para borda

        // Borda preta
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f); // espessura da borda
        paint.setColor(Color.BLACK);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 4, paint); // borda

        // Número do autocarro (texto)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sizePx * 0.5f);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textY = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(String.valueOf(busNumber), sizePx / 2f, textY, paint);

        return bitmap;
    }

}
