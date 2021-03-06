package ru.ifmo.md.lesson1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;


class WhirlView extends SurfaceView implements Runnable {
    final static String TAG = "WhirlView";

    private static final int WIDTH = 240;
    private static final int HEIGHT = 320;

    private float scaleWidth = 1f;
    private float scaleHeight = 1f;
    private int width = WIDTH;
    private int height = HEIGHT;

    private int[][] field = new int[width][height];
    private int[][] tmpField = new int[width][height];
    private int[] pixels = new int[width * height];

    private static final int MAX_COLORS = 10;
    private int[] palette = null;

    private Paint paint = null;
    private long lastFpsUpdate;
    private final static long FPS_UPDATE_INTERVAL = 1000 * 1000 * 1000;
    private float fps;
    private int framesDrawn = 0;

    SurfaceHolder holder;
    Thread thread = null;
    volatile boolean running = false;
    private boolean fieldStabilized = false;

    public WhirlView(Context context) {
        super(context);
        holder = getHolder();
        initField();
        initPalette();
        fieldStabilized = false;
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ignore) {}
    }

    public void run() {
        while (running) {
            if (holder.getSurface().isValid()) {
                updateField();
                recalcFps();
                Canvas canvas = holder.lockCanvas();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.i(TAG, "onSizeChanged: " + w + " " + h + " " + oldW + " " + oldH);
        scaleWidth = (float)w / WIDTH;
        scaleHeight = (float)h / HEIGHT;
    }

    void initPalette() {
        palette = new int[MAX_COLORS];
        float f1 = 1.4f, f2 = 1.4f, f3 = 1.4f;
        int p1 = 0, p2 = 2, p3 = 4;
        int w = 128, center = 127;
        for (int i = 0; i < MAX_COLORS; i++) {
            int red = (int) (Math.sin(f1 * i + p1) * w + center);
            int green = (int) (Math.sin(f2 * i + p2) * w + center);
            int blue = (int) (Math.sin(f3 * i + p3) * w + center);
            Log.i(TAG, red + " " + green + " " + blue);
            palette[i] = Color.rgb(red, green, blue);
        }
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
    }

    void initField() {
        Random rand = new Random();
        int last = -1;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = rand.nextInt(MAX_COLORS);
                while (color == last) {
                    color = rand.nextInt(MAX_COLORS);
                }
                field[x][y] = last = color;
            }
        }
    }

    void updateField() {
        int updatedCells = 0;
        if (!fieldStabilized) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    tmpField[x][y] = field[x][y];
                    changed:
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int x2 = x + dx;
                            int y2 = y + dy;
                            if (x2 < 0) x2 += width;
                            if (y2 < 0) y2 += height;
                            if (x2 >= width) x2 -= width;
                            if (y2 >= height) y2 -= height;
                            int color = field[x][y] + 1;
                            if (color == MAX_COLORS) color = 0;
                            if (color == field[x2][y2]) {
                                tmpField[x][y] = field[x2][y2];
                                updatedCells++;
                                break changed;
                            }
                        }
                    }
                }
            }
            int[][] tmp = field;
            field = tmpField;
            tmpField = tmp;
        } else {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    field[x][y]++;
                    if (field[x][y] == MAX_COLORS) {
                        field[x][y] = 0;
                    }
                }
            }
        }
        if (!fieldStabilized && updatedCells == width * height) {
            fieldStabilized = true;
            Log.d(TAG, "field stabilized");
        }
        int cnt = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[cnt++] = palette[field[x][y]];
            }
        }
    }

    void recalcFps() {
        framesDrawn++;
        long now = System.nanoTime();
        long elapsed = now - lastFpsUpdate;
        if (elapsed > FPS_UPDATE_INTERVAL) {
            fps = (float)framesDrawn * FPS_UPDATE_INTERVAL / elapsed;
            Log.d(TAG, String.format("%.3f", fps));
            framesDrawn = 0;
            lastFpsUpdate = now;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.scale(scaleWidth, scaleHeight);
        canvas.drawBitmap(pixels, 0, width, 0, 0, width, height, false, null);
        canvas.drawText("FPS: " + String.format("%.1f", fps), 10, 25, paint);
    }
}
