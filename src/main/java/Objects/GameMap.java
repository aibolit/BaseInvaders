/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Aleks
 */
public interface GameMap {

    public long getTicks();

    public Collection<Mine> getMines();

    public Collection<WormHole> getWormHoles();

    public Collection<Wall> getWalls();

    public long getUserScore(String user);

    public Map<String, Set<Mine>> getUserMines();

    public Player getPlayer(String user);

    public long getPlayerScore(String user);

    public Set<Bomb> getBombs();

    public Collection<Player> getPlayers();

    public boolean isRunning();
}
