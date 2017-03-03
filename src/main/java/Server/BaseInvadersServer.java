/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Objects.BaseInvadersException;
import Objects.Bomb;
import Objects.GameMap;
import Objects.GameMapImpl;
import Objects.Mine;
import Objects.Player;
import Objects.Point;
import baseinvaders.Configurations;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Sasa
 */
public class BaseInvadersServer implements BIServer, Runnable {

    private volatile boolean isRunning = false;
    private final GameMapImpl gameMap;
    private final Map<String, Map<Integer, Socket>> userConnections = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, PrintWriter>> subscribedConnections = new ConcurrentHashMap<>();

    public BaseInvadersServer(GameMapImpl exchange) {
        this.gameMap = exchange;

        Configurations.getUsers().stream().forEach(user -> {
            userConnections.put(user, new ConcurrentHashMap<>());
            subscribedConnections.put(user, new ConcurrentHashMap<>());
        });
    }

    @Override
    public GameMap getGameMap() {
        return gameMap;
    }

    private Integer newConnection(String user, Socket socket) {
        synchronized (userConnections) {
            Map<Integer, Socket> conns = userConnections.get(user);
            for (int i = 0; i < Configurations.getMaxConnectionsPerUser(); i++) {
                if (!conns.containsKey(i)) {
                    conns.put(i, socket);
                    return i;
                }
            }
        }
        return null;
    }

    private void removeConnection(String user, int id) {
        synchronized (userConnections) {
            try {
                Socket socket = userConnections.get(user).get(id);
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {

            }
            userConnections.get(user).remove(id);
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
        if (gameMap != null) {
            gameMap.setRunning(isRunning);
        }
    }

    private String getUIMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(Configurations.getMapWidth()).append(" ").append(Configurations.getMapHeight());
        long lastTick;
        synchronized (gameMap) {
            lastTick = gameMap.getTicks();
            sb.append(" ").append(lastTick);
            sb.append(" ").append(Configurations.getTicksRemaining());
            sb.append(" ").append(Configurations.getTickDelay());
            sb.append(" ").append(Configurations.getBombExplosionRadius());
            sb.append(" ").append(gameMap.getPlayers().size());
            gameMap.getPlayers().forEach(player -> {
                sb.append(" ").append(player.getId()).append(" ").append(player.getName()).append(" ").append(gameMap.getUserScore(player.getName())).append(" ").append(player.getPosition().getX()).append(" ").append(player.getPosition().getY()).append(" ").append(player.getVelocity().getX()).append(" ").append(player.getVelocity().getY());
            });
            sb.append(" ").append(gameMap.getMines().size());
            gameMap.getMines().forEach(mine -> {
                sb.append(" ").append(mine.getOwner() == null ? "--" : mine.getOwner().getName()).append(" ").append(mine.getPosition().getX()).append(" ").append(mine.getPosition().getY());
            });
            sb.append(" ").append(gameMap.getBombs().size());
            gameMap.getBombs().forEach(bomb -> {
                sb.append(" ").append(bomb.getDelay()).append(" ").append(bomb.getLifetime()).append(" ").append(bomb.getPosition().getX()).append(" ").append(bomb.getPosition().getY());
            });
            return sb.toString();
        }
    }

    @Override
    public void run() {
        isRunning = true;
        try {
            new Thread() {
                @Override
                public void run() {
                    setName("Subscriptions Distributor");
                    while (true) {
                        try {
                            synchronized (gameMap) {
                                gameMap.wait(1000);
                            }
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }.start();

            new Thread() {
                @Override
                public void run() {
                    try (ServerSocket serverSocket = new ServerSocket(Configurations.getUiPort());) {
                        setName("BaseInvaders UI server");

                        while (!serverSocket.isClosed()) {
                            final Socket socket = serverSocket.accept();
                            System.out.println("connected");
                            new Thread() {
                                @Override
                                public void run() {
                                    final List<Player> players = new ArrayList<>();
                                    final List<Mine> mines = new ArrayList<>();
                                    try (PrintWriter pw = new PrintWriter(socket.getOutputStream());
                                            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                                        long lastTick;

                                        String msg;
                                        synchronized (gameMap) {
                                            lastTick = gameMap.getTicks();
                                            msg = getUIMessage();
                                        }
                                        pw.println(msg);
                                        pw.flush();
                                        while (!socket.isClosed()) {
                                            String line = br.readLine();
                                            System.out.println(line);
                                            if (Long.parseLong(line) != lastTick) {
                                                socket.close();
                                                break;
                                            }
                                            try {
                                                Thread.sleep(Configurations.getTickDelay() - ((System.currentTimeMillis() + Configurations.getTickDelay() * 3 / 5) % Configurations.getTickDelay()));
                                            } catch (InterruptedException ex) {
                                                ex.printStackTrace();
                                            }
                                            synchronized (gameMap) {
                                                lastTick = gameMap.getTicks();
                                                msg = getUIMessage();
                                            }
                                            pw.println(msg);
                                            pw.flush();
                                        }
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }.start();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();

            ServerSocket serverSocket = new ServerSocket(Configurations.getPort());
            while (!serverSocket.isClosed()) {
                final Socket socket = serverSocket.accept();
                new Thread() {
                    @Override
                    public void run() {
                        setName("Socket Accept Pending");
                        String user = null;
                        Integer connectionId = null;
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter out = new PrintWriter(socket.getOutputStream())) {
                            System.out.println("accept");
                            StringTokenizer st = new StringTokenizer(in.readLine());
                            if (!st.hasMoreTokens()) {
                                out.println("Unknown User");
                                return;
                            }
                            user = st.nextToken();
                            if (!st.hasMoreTokens()) {
                                out.println("Unknown User");
                                return;
                            }
                            String password = st.nextToken();
                            if (!Configurations.getUsers().contains(user) || !Configurations.getUserPassword(user).equals(password)) {
                                out.println("Unknown User");
                                return;
                            }
                            this.setName("Connection-" + user);

                            if ((connectionId = newConnection(user, socket)) == null) {
                                out.println("You have already reached the maximum connection limit");
                                return;
                            }

                            String line;
                            while ((line = in.readLine()) != null && !line.trim().equals("CLOSE_CONNECTION")) {
                                try {
                                    String val;
                                    if (isRunning) {
                                        val = processCommand(user, line, connectionId, out);
                                    } else {
                                        val = "SERVER_NOT_ACTIVE";
                                    }
                                    synchronized (subscribedConnections) {
                                        if (val != null) {
                                            out.println(val);
                                        }
                                        out.flush();
                                    }
                                    Thread.sleep(Configurations.getTimeout());
                                } catch (BaseInvadersException ex) {
                                    synchronized (subscribedConnections) {
                                        out.println("ERROR " + ex.getMessage());
                                        out.flush();
                                    }
                                } catch (InterruptedException ex) {
                                } catch (Exception ex) {
                                    synchronized (subscribedConnections) {
                                        out.println("ERROR uknown error");
                                        ex.printStackTrace();
                                        out.flush();
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            if (user != null && connectionId != null) {
                                removeConnection(user, connectionId);
                            }
                        }
                    }

                }.start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private String processCommand(String user, String line, int connectionId, PrintWriter pout) throws BaseInvadersException {
        StringTokenizer st = new StringTokenizer(line.trim());
        if (!st.hasMoreTokens()) {
            return null;
        }
        String cmd = st.nextToken();
        String out = null;
        switch (cmd) {
            case "ACCELERATE": {
                gameMap.setAcceleration(user, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
                out = "ACCELERATE_OUT DONE";
            }
            break;
            case "BRAKE": {
                gameMap.setBrake(user);
                out = "BRAKE_OUT DONE";
            }
            break;
            case "STATUS": {
                final StringBuilder sb = new StringBuilder("STATUS_OUT ");
                Player p = gameMap.getPlayer(user);
                sb.append(p.getPosition().getX()).append(" ").append(p.getPosition().getY()).append(" ").append(p.getVelocity().getX()).append(" ").append(p.getVelocity().getY()).append(" ");
                sb.append(" MINES ");
                List<Mine> mines = new LinkedList<>();
                gameMap.getMines().stream().filter(mine -> mine.distanceTo(p) < Configurations.getVisionRadius()).forEach(mine -> mines.add(mine));
                sb.append(mines.size()).append(" ");
                mines.stream().forEach((mine) -> {
                    sb.append(mine.getOwner() != null ? mine.getOwner().getName() : "--").append(" ").append(mine.getPosition().getX()).append(" ").append(mine.getPosition().getY()).append(" ");
                });
                sb.append("PLAYERS ");
                List<Player> players = new LinkedList<>();
                gameMap.getPlayers().stream().filter(player -> player.distanceTo(p) < Configurations.getVisionRadius() && !player.equals(p)).forEach(player -> players.add(player));
                sb.append(players.size()).append(" ");
                players.stream().forEach((player) -> {
                    sb.append(player.getPosition().getX()).append(" ").append(player.getPosition().getY()).append(" ").append(player.getVelocity().getX()).append(" ").append(player.getVelocity().getY()).append(" ");
                });
                sb.append("BOMBS ");
                List<Bomb> bombs = new LinkedList<>();
                gameMap.getBombs().stream().filter(bomb -> bomb.distanceTo(p) < Configurations.getVisionRadius()).forEach(bomb -> bombs.add(bomb));
                sb.append(bombs.size()).append(" ");
                bombs.stream().forEach((bomb) -> {
                    sb.append(bomb.getPosition().getX()).append(" ").append(bomb.getPosition().getY()).append(" ").append(bomb.getLifetime()).append(" ");
                });
                out = sb.toString();
            }
            break;
            case "BOMB": {
                double x = Double.parseDouble(st.nextToken()), y = Double.parseDouble(st.nextToken());
                if (st.hasMoreTokens()) {
                    gameMap.placeBomb(user, x, y, Long.parseLong(st.nextToken()));
                } else {
                    gameMap.placeBomb(user, x, y, Configurations.getBombDelay());
                }
                out = "BOMB_OUT DONE";
            }
            break;
            case "SCOREBOARD": {
                Map<String, Set<Mine>> userMines = gameMap.getUserMines();
                StringBuilder sb = new StringBuilder("SCOREBOARD_OUT ");
                gameMap.getPlayers().stream().forEach(player -> sb.append(player.getName()).append(" ").append(gameMap.getPlayerScore(player.getName())).append(" ").append(userMines.containsKey(player.getName()) ? userMines.get(player.getName()).size() : 0).append(" "));
                out = sb.toString();
            }
            break;
            case "SCAN": {
                Point p = new Point(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
                gameMap.doScan(user, p);
                final StringBuilder sb = new StringBuilder("SCAN_OUT ");
                sb.append(" MINES ");
                List<Mine> mines = new LinkedList<>();
                gameMap.getMines().stream().filter(mine -> mine.distanceTo(p) < Configurations.getScanRadius()).forEach(mine -> mines.add(mine));
                sb.append(mines.size()).append(" ");
                mines.stream().forEach((mine) -> {
                    sb.append(mine.getOwner() != null ? mine.getOwner().getName() : "--").append(" ").append(mine.getPosition().getX()).append(" ").append(mine.getPosition().getY()).append(" ");
                });
                sb.append("PLAYERS ");
                List<Player> players = new LinkedList<>();
                gameMap.getPlayers().stream().filter(player -> player.distanceTo(p) < Configurations.getScanRadius()).forEach(player -> players.add(player));
                sb.append(players.size()).append(" ");
                players.stream().forEach((player) -> {
                    sb.append(player.getPosition().getX()).append(" ").append(player.getPosition().getY()).append(" ").append(player.getVelocity().getX()).append(" ").append(player.getVelocity().getY()).append(" ");
                });
                sb.append("BOMBS ");
                List<Bomb> bombs = new LinkedList<>();
                gameMap.getBombs().stream().filter(bomb -> bomb.distanceTo(p) < Configurations.getScanRadius()).forEach(bomb -> bombs.add(bomb));
                sb.append(bombs.size()).append(" ");
                bombs.stream().forEach((bomb) -> {
                    sb.append(bomb.getPosition().getX()).append(" ").append(bomb.getPosition().getY()).append(" ").append(bomb.getLifetime()).append(" ");
                });
                out = sb.toString();
            }
            break;
            case "CONFIGURATIONS": {
                out = "CONFIGURATIONS_OUT " + Configurations.getPlayerConfigString();
            }
            break;

            default: {
                if (cmd.charAt(0) != '#') {
                    out = "Unknown Command " + line;
                }
            }
            break;
        }
        return out;
    }
}
