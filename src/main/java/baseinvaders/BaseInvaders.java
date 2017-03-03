/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baseinvaders;

import GUI.GameStatus;
import Objects.BaseInvadersException;
import Objects.GameMapImpl;
import Server.BIUIServer;
import Server.BaseInvadersServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Sasa
 */
public class BaseInvaders {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            try {
                Configurations.readCongfigs("settings.cfg");
            } catch (IOException ex) {
                System.out.println("Warning: Could not read configuration file; reverting to defaults");
            }

            if (args.length > 0 && args[0].equals("ux")) {
                final BIUIServer server = new BIUIServer();

                final GameStatus status = new GameStatus(server);

                new Thread(server, "Server").start();
                try {
                    Thread.sleep(1000);
                    java.awt.EventQueue.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            status.setVisible(true);
                        }
                    });
                } catch (InterruptedException | InvocationTargetException ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
                status.run();

            } else {
                final GameMapImpl gameMap = new GameMapImpl();
                final BaseInvadersServer server = new BaseInvadersServer(gameMap);
                new Thread(gameMap, "Game Map").start();
                new Thread(server, "Server").start();
                if (Configurations.getUseLocalUI()) {
                    final GameStatus status = new GameStatus(server);
                    try {
                        java.awt.EventQueue.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                status.setVisible(true);
                            }
                        });
                    } catch (InterruptedException | InvocationTargetException ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    }
                    status.run();
                }
            }

        } catch (BaseInvadersException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
