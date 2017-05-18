package Objects;

import baseinvaders.Configurations;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final Set<WormHole> wormHoles = new CopyOnWriteArraySet<>();
    private final Set<Wall> walls = new CopyOnWriteArraySet<>();
    private final Map<String, Long> userLastScan = new ConcurrentHashMap<>();
    private long ticks = 0, downtimeTicks = 0;

    private volatile boolean isRunning = true;
    private volatile boolean isReady = true;

    public GameMapImpl() throws BaseInvadersException {
        Configurations.getUsers().forEach((player) -> {
            players.put(player, new Player(player, new Point(Configurations.getMapWidth() / 2, Configurations.getMapHeight() / 2)));
        });
        reset();
    }

    // For aesthetic reasons and to guarantee no enclosed parts of the grid,
    // the walls can only be placed in fixed locations.
    public List<Wall> getPossibleWalls() {
        List<Wall> walls = new ArrayList<Wall>();
        int mapWidth =  Configurations.getMapWidth();
        int mapHeight = Configurations.getMapHeight();
        double screenDivisions = 4;
        double xDistance = mapWidth / screenDivisions;
        double yDistance = mapHeight / screenDivisions;
        for (double i = xDistance / 2; i < mapWidth; i += xDistance) {
            for (double j = yDistance / 2; j < mapHeight; j += yDistance) {
                double length = Math.random() * (Configurations.getMaxWallLength()
                        - Configurations.getMinWallLength())
                        + Configurations.getMinWallLength();
                walls.add(new Wall(new Point(i, j), length, Math.random() > .5));
            }
        }
        return walls;
    }

    // For aesthetic reasons, the wormholes can only
    // be placed in fixed locations.
    public List<WormHole> getPossibleWormHoles() {
        List<WormHole> wormHoles = new ArrayList<WormHole>();
        int mapWidth =  Configurations.getMapWidth();
        int mapHeight = Configurations.getMapHeight();
        double screenDivisions = 4;
        double xDistance = mapWidth / screenDivisions;
        double yDistance = mapHeight / screenDivisions;
        for (double i = xDistance / 2; i < mapWidth; i += xDistance) {
            for (double j = yDistance / 2; j < mapHeight; j += yDistance) {
                double radius = Math.random() * (Configurations.getMaxWormHoleRadius()
                        - Configurations.getMinWormHoleRadius())
                        + Configurations.getMinWormHoleRadius();
                Point destination;
                while (true) {
                    destination = new Point(
                            Math.random() * Configurations.getMapWidth(),
                            Math.random() * Configurations.getMapHeight());
                    boolean valid = true;
                    for (WormHole w: wormHoles) {
                        if (w.getPosition().distanceTo(destination) < w.getRadius()) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) break;
                }
                double xOffset = xDistance / (Math.random() > .5 ? 4 : -4);
                double yOffset = yDistance / (Math.random() > .5 ? 4 : -4);
                Point center = new Point(i + xOffset, j + yOffset);
                wormHoles.add(new WormHole(center, radius, destination));
            }
        }
        return wormHoles;
    }

    private synchronized void reset() {
        ticks = 0;

        // Add mines
        mines.clear();
        double minMineSpeed = Configurations.getMinMineSpeed();
        double maxMineSpeed = Configurations.getMaxMineSpeed();
        for (int i = 0; i < Configurations.getMineCount(); i++) {
            double speed = Math.random() * (maxMineSpeed - minMineSpeed) + minMineSpeed;
            double angle = Math.random() * Math.PI * 2;
            Point velocity = new Point(speed * Math.cos(angle), speed * Math.sin(angle));
            Point position = new Point(Math.random() * Configurations.getMapWidth(), Math.random() * Configurations.getMapHeight());
            mines.add(new Mine(position, velocity));
        }

        // Add wormholes
        wormHoles.clear();
        List<WormHole> availableWormHoles = getPossibleWormHoles();
        Collections.shuffle(availableWormHoles);
        for (int i = 0; i < Configurations.getWormHoleCount(); i++) {
            if (i >= availableWormHoles.size()) break;
            WormHole w = availableWormHoles.get(i);
            wormHoles.add(w);
        }

        // Add walls
        walls.clear();
        List<Wall> availableWalls = getPossibleWalls();
        Collections.shuffle(availableWalls);
        for (int i = 0; i < Configurations.getWallCount(); i++) {
            if (i >= availableWalls.size()) break;
            Wall w = availableWalls.get(i);
            walls.add(w);
        }

        userScores.clear();
        userBombs.clear();
        bombs.clear();
        
        Configurations.getUsers().stream().forEach(player -> {
            userScores.put(player, 0L);
            userBombs.put(player, new CopyOnWriteArraySet<>());
        });

        players.values().stream().forEach(player-> {
            player.getPosition().setX(Configurations.getMapWidth() / 2).setY(Configurations.getMapHeight() / 2);
            player.getVelocity().setX(0).setY(0);
        });

        userAcceleration.clear();
        userAngle.clear();
        userBrakes.clear();

        userLastScan.clear();
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void setRunning(boolean run) {
        if (Configurations.getAutoRestart()) {
            this.isRunning = run;
        }
        else {
            if (run) {
                if (isReady) {
                    this.isReady = false;
                    this.isRunning = true;
                }
            }
            else {
                this.isRunning = false;
                this.isReady = false;
            }
        }
    }

    private synchronized void nextRound() {
        if (!isRunning) {
            return;
        }

        players.entrySet().stream().parallel().forEach(entry -> {
            String user = entry.getKey();
            Player player = entry.getValue();
            Point p = player.getPosition();
            Point v =  player.getVelocity();
            double xOld = p.getX(), yOld = p.getY();
            if (userBrakes.containsKey(user) && userBrakes.get(user)) {
                v.multiply(Configurations.getBrakeFriction());
                p.add(v).setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth()).setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());
            } else {
                double acceleration = 0;
                // Do not apply userAcceleration if user is disabled
                if (!player.isDisabled()) {
                    acceleration = Configurations.getSpeed() * (userAngle.containsKey(user) && userAcceleration.containsKey(user) ? userAcceleration.get(user) : 0);
                }
                double angle = userAngle.containsKey(user) ? userAngle.get(user) : 0;
                double x = acceleration * Math.cos(angle);
                double y = acceleration * Math.sin(angle);
                v.add(x * .5, y * .5);
                p.add(v).setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth()).setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());
                v.add(x * .5, y * .5);
            }

            double xNew = p.getX(), yNew = p.getY();
            Line path = new Line(xOld, yOld, xNew, yNew);

            // Handle walls
            if (path.getLength() > Configurations.getMapWidth() / 2) {
                // Ship is crossing to other end of map... or is going so fast
                // that walls aren't relevant anyway
            } else {
                for (Wall wall: walls) {
                    if (wall.getLine().intersects(path)) {

                        // Get the new angle of the ship and apply it
                        double oldAngle = Math.atan2(v.getY(), v.getX());
                        double newAngle = wall.getLine().getReflectedAngle(oldAngle);
                        double speed = v.getX() / Math.cos(oldAngle);
                        v.setX(speed * Math.cos(newAngle));
                        v.setY(speed * Math.sin(newAngle));

                        // Adjust the location of the ship
                        // NOTE: This is not as accurate as it could be. Ideally, we should:
                        // 1) Find the exact point of intersection between the wall and the path
                        // 2) Calculate the percentage P of the path that occured after collision
                        // 3) Set the ship's location to the point of intersection plus (P * v)
                        p.add(v).setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth()).setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());
                    }
                }
            }

            player.getVelocity().multiply(Configurations.getFriction());
            player.nextRound();
        });

        mines.stream().parallel().forEach((Mine mine) -> {

            Player[] ps = players.values().stream().filter(player -> !player.isDisabled() && player.distanceTo(mine) < Configurations.getCaptureRadius()).toArray(Player[]::new);
            if (ps.length == 1) {
                mine.setOwner(ps[0]);
            }

            // Update mine location
            Point p = mine.getPosition();
            Point v = mine.getVelocity();
            p.add(v).setX((p.getX() + Configurations.getMapWidth()) % Configurations.getMapWidth()).setY((p.getY() + Configurations.getMapHeight()) % Configurations.getMapHeight());
        });

        // Check if players are caught in worm holes
        players.entrySet().stream().parallel().forEach(playerEntry -> {
            Player player = playerEntry.getValue();
            WormHole currentWormHole = null;
            for (WormHole wormHole: wormHoles) {
                if (wormHole.getPosition().distanceTo(player.getPosition())
                        < wormHole.getRadius()) {
                    currentWormHole = wormHole;
                    break;
                }
            };
            if (currentWormHole != null) {
                player.setDisabled(true);

                // Set velocity towards center of worm hole
                if (player.getPosition().distanceTo(currentWormHole.getPosition()) > Configurations.getWormHoleCenterRadius()) {
                    double direction = player.getPosition().directionTo(currentWormHole.getPosition());
                    double gravity = Configurations.getWormHoleGravity();
                    double x = gravity * Math.cos(direction);
                    double y = gravity * Math.sin(direction);
                    player.getVelocity().add(x, y);

                // Transport players in center of worm hole
                } else {
                    Point destination = currentWormHole.getDestination();
                    player.getPosition().setX(destination.getX());
                    player.getPosition().setY(destination.getY());
                    player.getVelocity().setX(0).setY(0);
                }
            } else {
                player.setDisabled(false);
            }
        });

        mloop:
        for (Mine mine : mines) {
            Player closest = null;
            for (Player player : players.values()) {
                if (mine.getPosition().distanceTo(player.getPosition()) < Configurations.getCaptureRadius()) {
                    if (closest == null) {
                        closest = player;
                    } else {
                        continue mloop;
                    }
                }
            }
            if (closest != null) {
                mine.setOwner(closest);
            }
        }
        mines.stream().filter((mine) -> (mine.getOwner() != null)).forEach((mine) -> {
            Player player = mine.getOwner();
            String name = player.getName();
            userScores.put(name, 1 + userScores.get(name));
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

    public synchronized boolean isUserDisabled(String user) {
        Player player = players.get(user);
        return player.isDisabled();
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
    public synchronized Collection<WormHole> getWormHoles() {
        return wormHoles;
    }

    @Override
    public synchronized Collection<Wall> getWalls() {
        return walls;
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
        if (Configurations.getAutoRestart()) {
            isRunning = true;
        }
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
                        isReady = false;
                        downtimeTicks = 0;
                    }
                } else {
                    if (Configurations.getAutoRestart()) {
                        if (Configurations.getDowntimeTicks() != null && downtimeTicks < Configurations.getDowntimeTicks()) {
                            downtimeTicks++;
                        } else {
                            ticks = 0;
                            reset();
                            isRunning = true;
                        }
                    }
                    else if (!isReady) {
                        ticks = 0;
                        reset();
                        isReady = true;
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
        private final Set<WormHole> wormHoles;
        private final Set<Wall> walls;
        private final long ticks, downtimeTicks;
        private final boolean isRunning;
        private final boolean isReady;

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
        public Collection<WormHole> getWormHoles() {
            return wormHoles;
        }

        @Override
        public Collection<Wall> getWalls() {
            return walls;
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

        public GameMapMessage(Map<String, Player> players, Map<String, List<String>> userUpdates, Set<Mine> mines, Map<String, Long> userScores, Set<Bomb> bombs, Set<WormHole> wormHoles, Set<Wall> walls, long ticks, long downtimeTicks, boolean isRunning, boolean isReady) {
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
            this.wormHoles = new HashSet<>();
            wormHoles.stream().forEach((wormHole) -> {
                this.wormHoles.add(new WormHole(wormHole));
            });
            this.walls = new HashSet<>();
            walls.stream().forEach((wall) -> {
                this.walls.add(new Wall(wall));
            });

            this.userScores = new HashMap<>(userScores);
            this.ticks = ticks;
            this.downtimeTicks = downtimeTicks;
            this.isRunning = isRunning;
            this.isReady = isReady;

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
        lastMap = new GameMapMessage(players, userUpdates, mines, userScores, bombs, wormHoles, walls, ticks, downtimeTicks, isRunning, isReady);
        lastMapTick = ticks;
        return lastMap;
    }
}
