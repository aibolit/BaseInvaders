/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objects;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 *
 * @author Aleks
 */
public interface GameMap {

    public JSONObject toJSONObject();

    public long getTicks();

    public Collection<Mine> getMines();

    public long getUserScore(String user);

    public Map<String, List<String>> getUserUpdates();

    public Map<String, Set<Mine>> getUserMines();

    public Player getPlayer(String user);

    public long getPlayerScore(String user);

    public Set<Bomb> getBombs();

    public Collection<Player> getPlayers();

    public boolean isRunning();
}
