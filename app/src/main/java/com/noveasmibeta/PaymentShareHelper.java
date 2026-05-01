package com.noveasmibeta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PaymentShareHelper {

    private static final String TAG = "PaymentShareHelper";
    private static final String QR_FILENAME = "saved_qr.png";
    private static final String SHARE_FILENAME = "pago_movil.png";

    private final Context context;

    public PaymentShareHelper(Context context) {
        this.context = context;
    }

    /**
     * Verifica si ya hay un QR guardado
     */
    public boolean hasQrSaved() {
        File qrFile = new File(context.getFilesDir(), "qr_images/" + QR_FILENAME);
        return qrFile.exists();
    }

    /**
     * Guarda la imagen QR seleccionada por el usuario
     */
    public boolean saveQrImage(Uri imageUri) {
        try {
            File qrDir = new File(context.getFilesDir(), "qr_images");
            if (!qrDir.exists()) qrDir.mkdirs();

            File qrFile = new File(qrDir, QR_FILENAME);

            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return false;

            Bitmap original = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (original == null) return false;

            FileOutputStream fos = new FileOutputStream(qrFile);
            original.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            original.recycle();

            Log.d(TAG, "QR guardado en: " + qrFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error guardando QR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carga el QR guardado
     */
    private Bitmap loadSavedQr() {
        File qrFile = new File(context.getFilesDir(), "qr_images/" + QR_FILENAME);
        if (!qrFile.exists()) return null;
        return BitmapFactory.decodeFile(qrFile.getAbsolutePath());
    }

    /**
     * Genera la imagen compuesta con QR + montos y la comparte por WhatsApp
     */
    public void generateAndShare(String montoTop, String currencyTop,
                                  String montoBottom, String currencyBottom,
                                  double tasa, String tasaLabel) {
        try {
            // Cargar QR guardado
            Bitmap qrBitmap = loadSavedQr();
            if (qrBitmap == null) {
                Toast.makeText(context, "No se encontró imagen QR guardada", Toast.LENGTH_SHORT).show();
                return;
            }

            // Crear imagen compuesta
            Bitmap compositeImage = createCompositeImage(qrBitmap, montoTop, currencyTop,
                    montoBottom, currencyBottom, tasa, tasaLabel);
            qrBitmap.recycle();

            if (compositeImage == null) {
                Toast.makeText(context, "Error generando imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar imagen en caché para compartir
            File shareDir = new File(context.getCacheDir(), "shared_images");
            if (!shareDir.exists()) shareDir.mkdirs();

            File shareFile = new File(shareDir, SHARE_FILENAME);
            FileOutputStream fos = new FileOutputStream(shareFile);
            compositeImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            compositeImage.recycle();

            // Compartir por WhatsApp
            Uri contentUri = FileProvider.getUriForFile(context,
                    "com.noveasmibeta.fileprovider", shareFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setPackage("com.whatsapp");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Verificar si WhatsApp está instalado
            if (shareIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(shareIntent);
            } else {
                // Si no tiene WhatsApp, abrir selector genérico
                shareIntent.setPackage(null);
                context.startActivity(Intent.createChooser(shareIntent, "Compartir pago"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error compartiendo: " + e.getMessage());
            Toast.makeText(context, "Error al compartir: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Crea la imagen compuesta con diseño elegante
     */
    private Bitmap createCompositeImage(Bitmap qrBitmap, String montoTop, String currencyTop,
                                         String montoBottom, String currencyBottom,
                                         double tasa, String tasaLabel) {
        int width = 800;
        int qrSize = 500;
        int padding = 40;

        // Calcular altura dinámica
        int headerHeight = 120;
        int qrSectionHeight = qrSize + 40;
        int amountsHeight = 200;
        int footerHeight = 80;
        int totalHeight = headerHeight + qrSectionHeight + amountsHeight + footerHeight + padding * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo con gradiente oscuro
        Paint bgPaint = new Paint();
        bgPaint.setShader(new LinearGradient(0, 0, 0, totalHeight,
                Color.parseColor("#121226"), Color.parseColor("#1a1a3e"),
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, totalHeight, bgPaint);

        // Borde dorado sutil
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setColor(Color.parseColor("#C9A961"));
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(new RectF(8, 8, width - 8, totalHeight - 8), 20, 20, borderPaint);

        float y = padding;

        // === HEADER ===
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#D4AF37"));
        titlePaint.setTextSize(32);
        titlePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setAntiAlias(true);
        canvas.drawText("DATOS DE PAGO MÓVIL", width / 2f, y + 40, titlePaint);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.parseColor("#A0A0A0"));
        subtitlePaint.setTextSize(18);
        subtitlePaint.setTextAlign(Paint.Align.CENTER);
        subtitlePaint.setAntiAlias(true);
        canvas.drawText("Escanea el QR para pagar", width / 2f, y + 70, subtitlePaint);

        // Línea separadora dorada
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#C9A961"));
        linePaint.setStrokeWidth(1.5f);
        linePaint.setAntiAlias(true);
        y += headerHeight;
        canvas.drawLine(padding, y, width - padding, y, linePaint);

        // === QR IMAGE ===
        y += 20;
        Bitmap scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true);

        // Fondo blanco para el QR
        Paint qrBgPaint = new Paint();
        qrBgPaint.setColor(Color.WHITE);
        qrBgPaint.setAntiAlias(true);
        float qrX = (width - qrSize - 20) / 2f;
        canvas.drawRoundRect(new RectF(qrX, y, qrX + qrSize + 20, y + qrSize + 20), 12, 12, qrBgPaint);
        canvas.drawBitmap(scaledQr, qrX + 10, y + 10, null);
        scaledQr.recycle();

        y += qrSize + 40;

        // Línea separadora
        canvas.drawLine(padding, y, width - padding, y, linePaint);
        y += 20;

        // === MONTOS ===
        Paint amountPaint = new Paint();
        amountPaint.setColor(Color.WHITE);
        amountPaint.setTextSize(36);
        amountPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        amountPaint.setAntiAlias(true);

        // Monto superior
        String tengoText = getCurrencySymbol(currencyTop) + " " + montoTop + " " + getCurrencyCode(currencyTop);
        canvas.drawText(tengoText, padding + 10, y + 40, amountPaint);

        y += 60;

        // Monto inferior
        String quieroText = getCurrencySymbol(currencyBottom) + " " + montoBottom + " " + getCurrencyCode(currencyBottom);
        canvas.drawText(quieroText, padding + 10, y + 40, amountPaint);

        y += 60;

        // Tasa (BCV o Binance)
        if (tasa > 0) {
            Paint tasaPaint = new Paint();
            tasaPaint.setColor(Color.parseColor("#C9A961"));
            tasaPaint.setTextSize(20);
            tasaPaint.setTextAlign(Paint.Align.CENTER);
            tasaPaint.setAntiAlias(true);
            String tasaText = tasaLabel + ": " + String.format("%.2f", tasa) + " Bs/$";
            canvas.drawText(tasaText, width / 2f, y + 20, tasaPaint);
        }

        // === FOOTER ===
        y = totalHeight - 50;
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#505050"));
        footerPaint.setTextSize(16);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        footerPaint.setAntiAlias(true);
        footerPaint.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        canvas.drawText("Don't look at me \uD83D\uDD12", width / 2f, y, footerPaint);

        return bitmap;
    }

    private String getCurrencySymbol(String currency) {
        if (currency.contains("USDT")) return "₮";
        if (currency.contains("USD")) return "$";
        if (currency.contains("EUR")) return "€";
        if (currency.contains("VES")) return "Bs.";
        return "";
    }

    private String getCurrencyCode(String currency) {
        if (currency.contains("USDT")) return "USDT";
        if (currency.contains("USD")) return "USD";
        if (currency.contains("EUR")) return "EUR";
        if (currency.contains("VES")) return "VES";
        return "";
    }
}
