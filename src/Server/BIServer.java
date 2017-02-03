/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Objects.GameMap;

/**
 *
 * @author Aleks
 */
public interface BIServer extends Runnable {

    public GameMap getGameMap();

    public boolean isRunning();
}
