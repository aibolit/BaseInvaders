/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Objects.GameMap;
import baseinvaders.Configurations;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 *
 * @author Aleks
 */
public class BIUIServer implements BIServer {

    private volatile GameMap gameMap;
    private volatile boolean isRunning = false;

    @Override
    public GameMap getGameMap() {
        return gameMap;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public BIUIServer() {

    }

    @Override
    public void run() {
        final BIUIServer thisBI = this;
        System.out.println("Running");
        System.out.println(Configurations.getHost() + " " + Configurations.getBiUiPort());
        try (
                final Socket socket = new Socket(Configurations.getHost(), Configurations.getBiUiPort());
                final ObjectInputStream is = new ObjectInputStream(socket.getInputStream());) {
            while (true) {
                isRunning = true;
                synchronized (thisBI) {
                    gameMap = (GameMap) is.readObject();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
