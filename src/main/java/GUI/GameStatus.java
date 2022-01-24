/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import Objects.BaseInvadersException;
import Objects.GameMapImpl;
import Server.BIServer;
import Server.BaseInvadersServer;
import baseinvaders.Configurations;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.awt.AlphaComposite;
import java.util.HashSet;
import java.util.Set;
/**
 *
 * @author Sasa
 */
public class GameStatus extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
    private final BIServer baseInvadersServer;
    private final int biWidth = 1600, biHeight = 1080;
    private BufferedImage biImage;

    /**
     * Creates new form ExchangeStatus
     *
     * @param baseInvadersServer
     */
    public GameStatus(BIServer baseInvadersServer) {
        this.baseInvadersServer = baseInvadersServer;

        biImage = new BufferedImage(biWidth, biHeight, BufferedImage.TYPE_INT_ARGB);
        initComponents();
    }

    public void run() {
        new Thread() {
            @Override
            public void run() {
                setName("Screen Refresh");
                while (true) {
                    if (baseInvadersServer.isRunning()) {
                        try {
                            final long tickVal = baseInvadersServer.getGameMap().getTicks();
                            java.awt.EventQueue.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    refreshData(tickVal);
                                }
                            });
                        } catch (InterruptedException | InvocationTargetException ex) {
                            ex.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(Configurations.getTickDelay() - ((System.currentTimeMillis() + Configurations.getTickDelay() * 3 / 5) % Configurations.getTickDelay()));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }.start();
    }

    private void refreshData(long tick) {
        class UserScore implements Comparable<UserScore> {

            private final String user;
            private final long score;
            private final long minerals;
            private final long mineCount;

            UserScore(String user, long score, long minerals, long mineCount) {
                this.user = user;
                this.score = score;
                this.minerals = minerals;
                this.mineCount = mineCount;
            }

            public String getUser() {
                return user;
            }

            public long getScore() {
                return score;
            }

            public long getMinerals() {
                return minerals;
            }

            public long getMineCount() {
                return mineCount;
            }

            @Override
            public int compareTo(UserScore t) {
                double rv = t.getScore() - this.getScore();
                return rv == 0 ? this.getUser().compareTo(t.getUser()) : rv > 0 ? 1 : -1;
            }

            @Override
            public String toString() {
                return user + " " + score + " " + minerals + " " + mineCount;
            }
        }

        List<UserScore> scores = new ArrayList<>();
        baseInvadersServer.getGameMap().getPlayers().stream().forEach((player) -> {
            scores.add(new UserScore(player.getName(), baseInvadersServer.getGameMap().getUserScore(player.getName()), baseInvadersServer.getGameMap().getUserMinerals(player.getName()), baseInvadersServer.getGameMap().getUserMineCount(player.getName())));
        });
        Collections.sort(scores);

        if (canvas.getWidth() != biImage.getWidth() || canvas.getHeight() != biImage.getHeight()) {
            biImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D cg = (Graphics2D) biImage.getGraphics();

        cg.scale(1.0 * biImage.getWidth() / biWidth, 1.0 * biImage.getHeight() / biHeight);

        cg.setBackground(Color.BLACK);
        cg.setColor(Color.BLACK);
        cg.fillRect(0, 0, biWidth, biHeight);

        AffineTransform root = cg.getTransform();
        Stroke baseStroke = cg.getStroke();

        cg.transform(AffineTransform.getTranslateInstance(80, 50));
        cg.transform(AffineTransform.getScaleInstance(1.5, 1.5));

        cg.setFont(new Font("SansSerif", Font.BOLD, 44));
        cg.setColor(Color.DARK_GRAY);
        cg.drawString("Bloomberg", 18, 54);
        cg.setColor(Color.WHITE);
        cg.drawString("Bloomberg", 14, 50);
        cg.transform(AffineTransform.getTranslateInstance(0, 60));

        AffineTransform local = cg.getTransform();
        hsbColor += Configurations.getTickDelay() / 1000;
        cg.setColor(Color.getHSBColor((float) ((hsbColor % 360) / 360.0), 1, 1));
        cg.setFont(new Font("Segoe Script", Font.PLAIN, 32));
        cg.transform(AffineTransform.getRotateInstance(Math.PI / -4));
        cg.drawString("Hack", 10, 10);
        cg.setTransform(local);

        cg.setFont(new Font("Segoe Script", Font.PLAIN, 40));
        cg.drawString("@", 75, -50);

        cg.setTransform(root);

        DecimalFormat timeFormat = new DecimalFormat();
        timeFormat.setMaximumIntegerDigits(2);
        timeFormat.setMinimumIntegerDigits(2);
        timeFormat.setMaximumFractionDigits(0);
        cg.setFont(new Font("Monospaced", Font.PLAIN, 32));
        cg.setColor(Color.WHITE);
        long ticksRemaining = Configurations.getTicksRemaining() - tick;
        cg.drawString(timeFormat.format((ticksRemaining / (1000 / Configurations.getTickDelay())) / 3600) + ":" + timeFormat.format(((ticksRemaining / (1000 / Configurations.getTickDelay())) / 60) % 60) + ":" + timeFormat.format((ticksRemaining / (1000 / Configurations.getTickDelay())) % 60), 180, 180);

        cg.setColor(VERY_DARK_GRAY);
        for (int i = 0; i < 21; i++) {
            if (i % 2 == 0) {
                cg.fillRect(20, 240 + i * 40, 500, 40);
            }
        }

        cg.setColor(Color.GRAY);
        cg.setStroke(new BasicStroke(4));
        cg.drawRect(20, 200, 500, 860);
        cg.fillRect(20, 200, 500, 40);

        cg.setStroke(baseStroke);
        cg.setColor(Color.WHITE);
        cg.setFont(new Font("Arial", Font.BOLD, 32));
        cg.drawString("Player", 24, 230);
        cg.drawString("Own", 200, 230);
        cg.drawString("Mins", 320, 230);
        cg.drawString("Score", 420, 230);

        cg.setColor(AMBER);
        DecimalFormat scoreFormat = new DecimalFormat("");
        scoreFormat.setMinimumIntegerDigits(12);

        Font playerFont = new Font("Arial", Font.PLAIN, 36);
        Font scoreFont = new Font("Monospaced", Font.PLAIN, 36);
        for (int i = 0; i < 21 && i < scores.size(); i++) {
            cg.drawImage(Configurations.getPlayerImage(baseInvadersServer.getGameMap().getPlayer(scores.get(i).getUser()).getPlayerId()), 24, 250 + 40 * i, null);
            cg.setFont(playerFont);
            cg.drawString(scores.get(i).getUser(), 50, 270 + 40 * i);
            cg.setFont(scoreFont);
            cg.drawString(String.format("%19s", scores.get(i).getMineCount()), -150, 270 + 40 * i);
            cg.drawString(String.format("%19s", scores.get(i).getMinerals()), -30, 270 + 40 * i);
            cg.drawString(String.format("%19s", scores.get(i).getScore()), 90, 270 + 40 * i);
        }

        cg.setTransform(root);
        cg.translate(560, 40);
        double mapScale = 1000;

        if (baseInvadersServer.getGameMap().isRunning()) {
            
            cg.scale(mapScale / Configurations.getMapWidth(), mapScale / Configurations.getMapHeight());

            cg.setColor(new Color(100, 0, 100, 200));
            //cg.setColor(Color.CYAN);
            baseInvadersServer.getGameMap().getWormHoles().forEach((wormHole) -> {
                AffineTransform tloc = cg.getTransform();

                cg.translate(wormHole.getPosition().getX(), wormHole.getPosition().getY());
                //cg.scale(Configurations.getMapWidth() / mapScale, Configurations.getMapHeight() / mapScale);
                cg.fillOval((int) (-wormHole.getRadius()), (int) (-wormHole.getRadius()), (int) wormHole.getRadius() * 2, (int) wormHole.getRadius() * 2);
                //cg.fillOval(-100, -100, 200, 200);

                cg.setTransform(tloc);
            });

            baseInvadersServer.getGameMap().getPlayers().stream().forEach((player) -> {
                AffineTransform tloc = cg.getTransform();
                cg.translate(player.getPosition().getX(), player.getPosition().getY());
                cg.scale(Configurations.getMapWidth() / mapScale, Configurations.getMapHeight() / mapScale);
                cg.rotate(Math.PI / 2 + Math.atan2(player.getVelocity().getY(), player.getVelocity().getX()));
                cg.drawImage(Configurations.getPlayerImage(player.getPlayerId()), -12, -12, this);
                cg.setTransform(tloc);
            });

            cg.setColor(Color.WHITE);

            baseInvadersServer.getGameMap().getMines().stream().forEach((mine) -> {

                final float percentLeft = (float)mine.getResources() /  (float)Math.max(1, mine.getMaxResources());
                final float alpha = (1.0f - Configurations.getMineDisplayMinAlpha()) * percentLeft + Configurations.getMineDisplayMinAlpha();
                cg.setColor(new Color(1.0f, 1.0f, 1.0f, alpha));

                AffineTransform tloc = cg.getTransform();
                cg.translate(mine.getPosition().getX(), mine.getPosition().getY());
                cg.scale(Configurations.getMapWidth() / mapScale, Configurations.getMapHeight() / mapScale);
                if (mine.getOwner() != null) {
                    cg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    cg.drawImage(Configurations.getPlayerImage(mine.getOwner().getPlayerId()), -10, -10, 20, 20, null);
                }
                cg.drawOval(-5, -5, 10, 10);
                cg.setTransform(tloc);

                // Reset alpha for next pass
                cg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            });

            cg.setColor(Color.GREEN);

            baseInvadersServer.getGameMap().getStations().stream().forEach((station) -> {
                AffineTransform tloc = cg.getTransform();
                cg.translate(station.getPosition().getX(), station.getPosition().getY());
                cg.scale(Configurations.getMapWidth() / mapScale, Configurations.getMapHeight() / mapScale);
                cg.drawOval(-10, -10, 20, 20);
                cg.setTransform(tloc);
            });

            cg.setColor(Color.getHSBColor((float) ((tick % 360) / 360.0), 1, 1));
            baseInvadersServer.getGameMap().getBombs().stream().forEach((bomb) -> {
                AffineTransform tloc = cg.getTransform();
                cg.translate(bomb.getPosition().getX(), bomb.getPosition().getY());
                cg.scale(Configurations.getMapWidth() / mapScale, Configurations.getMapHeight() / mapScale);
                cg.fillOval(-10, -10, 20, 20);
                cg.setTransform(tloc);
            });
        } else {
            cg.setColor(AMBER);
            cg.setFont(new Font("Arial", Font.BOLD, 108));
            int w = cg.getFontMetrics().stringWidth(scores.get(0).getUser());
            cg.drawString(scores.get(0).getUser(), (1000 - w) / 2, 520);
            cg.drawImage(Configurations.getPlayerImage(baseInvadersServer.getGameMap().getPlayer(scores.get(0).getUser()).getPlayerId()), (1000 - w) / 2 - 108, 520 - 108, 108, 108, null);
            cg.setFont(new Font("Arial", Font.BOLD, 48));
            w = cg.getFontMetrics().stringWidth(scores.get(1).getUser());
            cg.drawString(scores.get(1).getUser(), (1000 - w) / 2, 590);
            cg.drawImage(Configurations.getPlayerImage(baseInvadersServer.getGameMap().getPlayer(scores.get(1).getUser()).getPlayerId()), (1000 - w) / 2 - 48, 590 - 48, 48, 48, null);
            cg.setFont(new Font("Arial", Font.BOLD, 24));
            w = cg.getFontMetrics().stringWidth(scores.get(2).getUser());
            cg.drawString(scores.get(2).getUser(), (1000 - w) / 2, 620);
            cg.drawImage(Configurations.getPlayerImage(baseInvadersServer.getGameMap().getPlayer(scores.get(2).getUser()).getPlayerId()), (1000 - w) / 2 - 24, 620 - 24, 24, 24, null);

        }

        cg.setTransform(root);
        cg.setColor(Color.GRAY);
        cg.setStroke(new BasicStroke(4));
        cg.drawRect(540, 20, 1040, 1040);

        canvas.setImage(biImage);
        canvas.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        disclaimerLabel = new javax.swing.JLabel();
        canvas = new GUI.Canvas();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Base Invaders");

        disclaimerLabel.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        disclaimerLabel.setText("Exchange Server Version 1.0.0 -- Developed By Aleks Tamarkin");

        javax.swing.GroupLayout canvasLayout = new javax.swing.GroupLayout(canvas);
        canvas.setLayout(canvasLayout);
        canvasLayout.setHorizontalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1360, Short.MAX_VALUE)
        );
        canvasLayout.setVerticalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 778, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 1138, Short.MAX_VALUE)
                .addComponent(disclaimerLabel))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(canvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(774, Short.MAX_VALUE)
                .addComponent(disclaimerLabel))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(canvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GameStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GameStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GameStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GameStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new GameStatus(new BaseInvadersServer(new GameMapImpl())).setVisible(true);
            } catch (BaseInvadersException ex) {
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private GUI.Canvas canvas;
    private javax.swing.JLabel disclaimerLabel;
    // End of variables declaration//GEN-END:variables
    private static final Color AMBER = new Color(255, 126, 0);
    private static final Color VERY_DARK_GRAY = new Color(24, 24, 24);
    private double hsbColor = 0;
}
