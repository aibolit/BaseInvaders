package Objects;

import baseinvaders.Configurations;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 * @author Sasa
 */
public class GameMapImpl implements Runnable, Serializable, GameMap {

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userUpdates = new ConcurrentHashMap<>();
    private final Map<String, Double> userAcceleration = new ConcurrentHashMap<>();
    private final Map<String, Double> userAngle = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userBrakes = new ConcurrentHashMap<>();
    private final Set<Mine> mines = new CopyOnWriteArraySet<>();
    private final Map<String, Long> userScores = new ConcurrentHashMap<>();
    private final Set<Bomb> bombs = new CopyOnWriteArraySet<>();
    private final Map<String, Set<Bomb>> userBombs = new ConcurrentHashMap<>();
    private final Map<String, Long> userLastScan = new ConcurrentHashMap<>();
    private long ticks = 0, downtimeTicks = 0;

    private volatile boolean isRunning = true;

    public GameMapImpl() throws BaseInvadersException {
        Configurations.getUsers().forEach((player) -> {
            players.put(player, new Player(player, new Point(Configurations.getMapWidth() / 2, Configurations.getMapHeight() / 2)));
        });
        reset();
    }

    private synchronized void reset() {
        ticks = 0;
        mines.clear();
        for (int i = 0; i < Configurations.getMineCount(); i++) {
            mines.add(new Mine(new Point(Math.random() * Configurations.getMapWidth(), Math.random() * Configurations.getMapHeight())));
        }

        userScores.clear();
        userBombs.clear();
        bombs.clear();

        Configurations.getUsers().stream().forEach(player -> {
            userScores.put(player, 0L);
            userBombs.put(player, new CopyOnWriteArraySet<>());
        });

        players.values().stream().forEach(player -> {
            player.getPosition().setX(Configurations.getMapWidth() / 2).setY(Configurations.getMapHeight() / 2);
            player.getVelocity().setX(0).setY(0);
        });

        userAcceleration.clear();
        userAngle.clear();
        userBrakes.clear();
        userLastScan.clear();

        players.values().stream().forEach(player -> {
            userAcceleration.put(player.getName(), 1.0);
            userAngle.put(player.getName(), Math.random() * Math.PI * 2);
        });
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void setRunning(boolean run) {
        this.isRunning = run;
    }

    private synchronized void nextRound() {
        if (!isRunning) {
            return;
        }

        players.entrySet().stream().parallel().forEach(entry -> {
            String user = entry.getKey();
            Player player = entry.getValue();
            if (userBrakes.containsKey(user) && userBrakes.get(user)) {
                player.getVelocity().multiply(Configurations.getBrakeFriction());
                Point p = player.getPosition();
                p.add(player.getVelocity()).setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth()).setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());

            } else {
                double acceleration = Configurations.getSpeed() * (userAngle.containsKey(user) && userAcceleration.containsKey(user) ? userAcceleration.get(user) : 0);
                double angle = userAngle.containsKey(user) ? userAngle.get(user) : 0;

                double x = acceleration * Math.cos(angle);
                double y = acceleration * Math.sin(angle);

                player.getVelocity().add(x * .5, y * .5);

                Point p = player.getPosition();
                p.add(player.getVelocity()).setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth()).setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());

                player.getVelocity().add(x * .5, y * .5);

            }
            player.getVelocity().multiply(Configurations.getFriction());
            player.nextRound();
        });

        mines.stream().parallel().forEach((Mine mine) -> {

            Player[] ps = players.values().stream().filter(player -> player.distanceTo(mine) < Configurations.getCaptureRadius()).toArray(Player[]::new);
            if (ps.length == 1) {
                mine.setOwner(ps[0]);
            }
        });

        mines.stream().filter((mine) -> (mine.getOwner() != null)).forEach((mine) -> {
            userScores.put(mine.getOwner().getName(), 1 + userScores.get(mine.getOwner().getName()));
        });
        Set<Bomb> removeBombs = new CopyOnWriteArraySet<>();
        bombs.stream().parallel().forEach(bomb -> {
            if (bomb.isExploded()) {
                removeBombs.add(bomb);
                players.values().stream().filter((player) -> (player.distanceTo(bomb) < Configurations.getBombExplosionRadius())).forEach((player) -> {
                    double angle = bomb.directionTo(player);
                    double acceleration = Configurations.getBombPower() * Math.sqrt((Configurations.getBombExplosionRadius() - bomb.distanceTo(player)) / Configurations.getBombExplosionRadius());

                    double x = acceleration * Math.cos(angle);
                    double y = acceleration * Math.sin(angle);

                    player.getVelocity().add(x, y);
                });
            }

            bomb.nextRound();
        });
        removeBombs.stream().forEach(bomb -> {
            bombs.remove(bomb);
            userBombs.get(bomb.getPlayer().getName()).remove(bomb);
        });

        System.gc();
    }

    private synchronized void addSubscriptionUpdate(Map<String, List<String>> data, String user, String update) {
        if (!data.containsKey(user)) {
            data.put(user, new CopyOnWriteArrayList<>());
        }
        data.get(user).add(update);
    }

    public synchronized void setAcceleration(String user, double angle, double acceleration) throws BaseInvadersException {
        if (acceleration < 0 || acceleration > 1) {
            throw new BaseInvadersException("Acceleration must be between 0 and 1");
        }
        userBrakes.put(user, false);
        userAngle.put(user, angle);
        userAcceleration.put(user, acceleration);
    }

    public synchronized void setBrake(String user) {
        userBrakes.put(user, true);
        userAngle.put(user, 0.0);
        userAcceleration.put(user, 0.0);
    }

    public synchronized void placeBomb(String user, double x, double y, long delay) throws BaseInvadersException {
        Point p = new Point(x, y);
        Player player = players.get(user);
        if (player.distanceTo(p) > Configurations.getBombPlacementRadius()) {
            throw new BaseInvadersException("Cannot Place Bomb that far away.");
        }
        if (userBombs.get(user).size() >= Configurations.getMaxBombs()) {
            throw new BaseInvadersException("Unable to place this many bombs at a time");
        }
        if (delay > Configurations.getMaxBombDelay() || delay < Configurations.getMinBombDelay()) {
            throw new BaseInvadersException("Bomb delay is out of allowable range");
        }

        Bomb b = new Bomb(player, p, delay);
        bombs.add(b);
        userBombs.get(user).add(b);
    }

    @Override
    public synchronized Collection<Player> getPlayers() {
        return players.values();
    }

    @Override
    public synchronized long getTicks() {
        return ticks;
    }

    @Override
    public synchronized Collection<Mine> getMines() {
        return mines;
    }

    @Override
    public synchronized long getUserScore(String user) {
        if (!userScores.containsKey(user)) {
            return 0;
        }
        return userScores.get(user);
    }

    @Override
    public synchronized Map<String, Set<Mine>> getUserMines() {
        Map<String, Set<Mine>> userMines = new HashMap<>();
        mines.stream().filter((mine) -> !(mine.getOwner() == null)).map((mine) -> {
            if (!userMines.containsKey(mine.getOwner().getName())) {
                userMines.put(mine.getOwner().getName(), new HashSet<>());
            }
            return mine;
        }).forEach((mine) -> {
            userMines.get(mine.getOwner().getName()).add(mine);
        });
        return userMines;
    }

    @Override
    public synchronized Player getPlayer(String user) {
        return players.get(user);
    }

    @Override
    public synchronized long getPlayerScore(String user) {
        return userScores.get(user);
    }

    @Override
    public synchronized Set<Bomb> getBombs() {
        return bombs;
    }

    public synchronized void doScan(String user, Point p) throws BaseInvadersException {
        if (p.getX() < 0 || p.getX() > Configurations.getMapWidth() || p.getY() < 0 || p.getY() > Configurations.getMapHeight()) {
            throw new BaseInvadersException("Sight location out of range");
        }
        if (userLastScan.containsKey(user) && userLastScan.get(user) + Configurations.getScanDelay() > ticks) {
            throw new BaseInvadersException("Scanning too soon");
        }
        userLastScan.put(user, ticks);
    }

    @Override
    public synchronized String toString() {
        return "GameMap{" + "players=" + players + ", userUpdates=" + userUpdates + ", userAcceleration=" + userAcceleration + ", userAngle=" + userAngle + ", userBrakes=" + userBrakes + ", mines=" + mines + ", userScores=" + userScores + ", isRunning=" + isRunning + '}';
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(Configurations.getTickDelay() - (System.currentTimeMillis() % Configurations.getTickDelay()));
                if (isRunning) {
                    if (Configurations.getTicksRemaining() != null && ticks < Configurations.getTicksRemaining()) {
                        ticks++;
                        try {
                            nextRound();
                        } catch (Exception ex) {
                            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            ex.printStackTrace();
                        }
                    } else {
                        isRunning = false;
                        downtimeTicks = 0;
                    }
                } else {
                    if (Configurations.getDowntimeTicks() != null && downtimeTicks < Configurations.getDowntimeTicks()) {
                        downtimeTicks++;
                    } else {
                        ticks = 0;
                        reset();
                        isRunning = true;
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    class GameMapMessage implements GameMap, Serializable {

        private final Map<String, Player> players;
        private final Map<String, List<String>> userUpdates;
        private final Set<Mine> mines;
        private final Map<String, Long> userScores;
        private final Set<Bomb> bombs;
        private final long ticks, downtimeTicks;
        private final boolean isRunning;

        @Override
        public Collection<Player> getPlayers() {
            return players.values();
        }

        @Override
        public long getTicks() {
            return ticks;
        }

        @Override
        public Collection<Mine> getMines() {
            return mines;
        }

        @Override
        public long getUserScore(String user) {
            if (!userScores.containsKey(user)) {
                return 0;
            }
            return userScores.get(user);
        }

        @Override
        public Map<String, Set<Mine>> getUserMines() {
            Map<String, Set<Mine>> userMines = new HashMap<>();
            mines.stream().filter((mine) -> !(mine.getOwner() == null)).map((mine) -> {
                if (!userMines.containsKey(mine.getOwner().getName())) {
                    userMines.put(mine.getOwner().getName(), new HashSet<>());
                }
                return mine;
            }).forEach((mine) -> {
                userMines.get(mine.getOwner().getName()).add(mine);
            });
            return userMines;
        }

        @Override
        public Player getPlayer(String user) {
            return players.get(user);
        }

        @Override
        public long getPlayerScore(String user) {
            return userScores.get(user);
        }

        @Override
        public Set<Bomb> getBombs() {
            return bombs;
        }

        public GameMapMessage(Map<String, Player> players, Map<String, List<String>> userUpdates, Set<Mine> mines, Map<String, Long> userScores, Set<Bomb> bombs, long ticks, long downtimeTicks, boolean isRunning) {
            this.players = new HashMap<>();
            players.entrySet().stream().forEach((player) -> {
                this.players.put(player.getKey(), new Player(player.getValue()));
            });
            this.userUpdates = new HashMap<>(userUpdates);
            this.mines = new HashSet<>();
            mines.stream().forEach((mine) -> {
                this.mines.add(new Mine(mine));
            });
            this.bombs = new HashSet<>(bombs);

            this.userScores = new HashMap<>(userScores);
            this.ticks = ticks;
            this.downtimeTicks = downtimeTicks;
            this.isRunning = isRunning;

        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }

    }

    private GameMap lastMap = null;
    private long lastMapTick = -1;

    public synchronized GameMap makeCopy() {
        if (lastMapTick == ticks && lastMap != null) {
            return lastMap;
        }
        lastMap = new GameMapMessage(players, userUpdates, mines, userScores, bombs, ticks, downtimeTicks, isRunning);
        lastMapTick = ticks;
        return lastMap;
    }
}
