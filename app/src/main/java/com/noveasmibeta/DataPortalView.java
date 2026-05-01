package com.noveasmibeta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataPortalView extends View {

    private List<Particle> particles;
    private Paint particlePaint;
    private Paint linePaint;
    private Random random;
    private boolean isAnimating = false;
    private static final int PARTICLE_COUNT = 35;
    private static final float CONNECTION_DISTANCE = 350f;
    private static final float PARTICLE_SPEED = 0.35f;

    public DataPortalView(Context context) {
        super(context);
        init();
    }

    public DataPortalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        random = new Random();
        particles = new ArrayList<>();

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setColor(0x55D5CAA3);
        particlePaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0x30D5CAA3);
        linePaint.setStrokeWidth(1.8f);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initParticles(w, h);
    }

    private void initParticles(int width, int height) {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(
                random.nextFloat() * width,
                random.nextFloat() * height,
                (random.nextFloat() - 0.5f) * PARTICLE_SPEED,
                (random.nextFloat() - 0.5f) * PARTICLE_SPEED,
                3f + random.nextFloat() * 3.5f
            ));
        }
    }

    public void startAnimation() {
        isAnimating = true;
        invalidate();
    }

    public void stopAnimation() {
        isAnimating = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (particles.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        for (Particle p : particles) {
            p.update(width, height);
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
        }

        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);
                float distance = (float) Math.sqrt(
                    Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2)
                );
                if (distance < CONNECTION_DISTANCE) {
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint);
                }
            }
        }

        if (isAnimating) {
            postInvalidateDelayed(16);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;

        Particle(float x, float y, float vx, float vy, float radius) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
        }

        void update(int width, int height) {
            x += vx;
            y += vy;

            if (x < 0 || x > width) vx = -vx;
            if (y < 0 || y > height) vy = -vy;

            x = Math.max(0, Math.min(width, x));
            y = Math.max(0, Math.min(height, y));
        }
    }
}
