package com.snippetmanager.ui;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * A small animated neon panel with click-to-change-color, simple glow, and pause/resume support.
 */
public class NeonPanel extends JPanel {
    private final Timer animator;
    private float phase = 0f;
    private Color color = new Color(0x1D6DE4);
    private boolean running = true;
    private final Random rnd = new Random();

    public NeonPanel() {
        setPreferredSize(new Dimension(160, 48));
        setOpaque(false);

        animator = new Timer(40, e -> {
            phase += 0.04f;
            if (phase > Math.PI * 2) phase -= Math.PI * 2;
            repaint();
        });
        animator.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // randomize hue-ish color on click
                color = new Color(rnd.nextInt(160) + 60, rnd.nextInt(160) + 60, rnd.nextInt(160) + 60);
                // play synthesized click tone asynchronously
                playClickTone(880, 120);
                repaint();
            }
        });
    }

    public void toggleRunning() {
        running = !running;
        if (running) animator.start(); else animator.stop();
        repaint();
    }

    public void setRunning(boolean run) {
        if (this.running == run) return;
        this.running = run;
        if (run) animator.start(); else animator.stop();
        repaint();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // subtle background glass
        GradientPaint gp = new GradientPaint(0, 0, new Color(255,255,255,24), 0, h, new Color(0,0,0,12));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // central neon orb with pulsing glow
        double cx = w * 0.5;
        double cy = h * 0.5;
        double maxR = Math.min(w, h) * 0.36;
        double pulse = 0.85 + 0.15 * Math.sin(phase * 2.0);
        double r = maxR * pulse;

        // draw glow into an offscreen image so we can blur it for a smooth glow
        int iw = Math.max(1, w);
        int ih = Math.max(1, h);
        BufferedImage off = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D og = off.createGraphics();
        og.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 8; i >= 1; i--) {
            float a = (float) (0.08 * (1.0 / i));
            Color c = withAlpha(color, a);
            og.setColor(c);
            double rr = r * (1.0 + 0.16 * i);
            Ellipse2D glow = new Ellipse2D.Double(cx - rr, cy - rr, rr * 2, rr * 2);
            og.fill(glow);
        }
        // draw core on the offscreen as well for a softer edge
        og.setColor(color.brighter());
        Ellipse2D core = new Ellipse2D.Double(cx - r * 0.6, cy - r * 0.6, r * 1.2, r * 1.2);
        og.fill(core);
        og.dispose();

        // apply a small blur kernel (box blur approximation)
        float[] kernel = makeBlurKernel(7);
        try {
            ConvolveOp op = new ConvolveOp(new Kernel(7, 7, kernel), ConvolveOp.EDGE_NO_OP, null);
            BufferedImage blurred = op.filter(off, null);
            g2.drawImage(blurred, 0, 0, null);
        } catch (Exception ex) {
            // fallback to drawing without blur
            g2.drawImage(off, 0, 0, null);
        }

        // small indicator for paused state
        if (!running) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.drawString("PAUSED", 8, h - 8);
        }

        g2.dispose();
    }

    private static float[] makeBlurKernel(int size) {
        int len = size * size;
        float[] k = new float[len];
        float v = 1.0f / len;
        for (int i = 0; i < len; i++) k[i] = v;
        return k;
    }

    private void playClickTone(int freqHz, int ms) {
        new Thread(() -> {
            try {
                float sampleRate = 16_000f;
                byte[] buf = new byte[(int) (ms * sampleRate / 1000) * 2];
                int idx = 0;
                for (int i = 0; i < buf.length / 2; i++) {
                    double t = i / sampleRate;
                    short val = (short) (Math.sin(2 * Math.PI * freqHz * t) * 32767 * 0.25);
                    buf[idx++] = (byte) (val & 0xff);
                    buf[idx++] = (byte) ((val >> 8) & 0xff);
                }

                AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, false);
                try (SourceDataLine sdl = (SourceDataLine) javax.sound.sampled.AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, af))) {
                    sdl.open(af);
                    sdl.start();
                    sdl.write(buf, 0, buf.length);
                    sdl.drain();
                }
            } catch (Exception ignored) {
            }
        }, "neon-click-tone").start();
    }

    private static Color withAlpha(Color base, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
    }
}
