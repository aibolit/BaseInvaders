/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Objects.BaseInvadersException;
import Objects.GameMapImpl;
import baseinvaders.Configurations;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import org.json.JSONWriter;

/**
 *
 * @author jmcilhargey
 */
public class UIServer implements Runnable {

    private volatile boolean isRunning = false;
    private final GameMapImpl gameMap;
    private final Map<String, Map<Integer, Socket>> userConnections = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, PrintWriter>> subscribedConnections = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userStreaming = new ConcurrentHashMap<>();

    public UIServer(GameMapImpl exchange) {
        this.gameMap = exchange;
        for (String user : Configurations.getUIUsers()) {
            userConnections.put(user, new ConcurrentHashMap<Integer, Socket>());
            subscribedConnections.put(user, new ConcurrentHashMap<Integer, PrintWriter>());
        }
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
            userStreaming.put(user, false);
        }
        return null;
    }

    private void removeConnection(String user, int id) {
        synchronized (subscribedConnections) {
            subscribedConnections.get(user).remove(id);
        }
        synchronized (userConnections) {
            try {
                Socket socket = userConnections.get(user).get(id);
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {

            }
            userConnections.get(user).remove(id);
            userStreaming.put(user, false);
        }
    }

    public boolean isIsRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
        if (gameMap != null) {
            gameMap.setRunning(isRunning);
        }
    }

    @Override
    public void run() {
        isRunning = true;
        try {
            new Thread() {
                @Override
                public void run() {
                    setName("UI Distributor");
                    while (true) {
                        try {
                            synchronized (gameMap) {
                                gameMap.wait(1000);
                            }
                        } catch (InterruptedException ex) {
                        } finally {
                        }

                    }
                }
            }.start();

            ServerSocket serverSocket = new ServerSocket(Configurations.getUiPort());
            while (!serverSocket.isClosed()) {
                final Socket socket = serverSocket.accept();
                new Thread() {
                    @Override
                    public void run() {
                        setName("UI Socket Accept Pending");
                        String user = null;
                        Integer connectionId = null;
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter out = new PrintWriter(socket.getOutputStream())) {

                            StringTokenizer st = new StringTokenizer(in.readLine());
                            if (!st.hasMoreTokens()) {
                                JSONWriter jsonWriter = new JSONWriter(out);
                                jsonWriter.object();
                                jsonWriter.key("STATUS").value("ERROR");
                                jsonWriter.key("MESSAGE").value("Unknown User");
                                jsonWriter.endObject();
                                return;
                            }
                            user = st.nextToken();
                            if (!st.hasMoreTokens()) {
                                JSONWriter jsonWriter = new JSONWriter(out);
                                jsonWriter.object();
                                jsonWriter.key("STATUS").value("ERROR");
                                jsonWriter.key("MESSAGE").value("Unknown User");
                                jsonWriter.endObject();
                                return;
                            }
                            String password = st.nextToken();
                            if (!Configurations.getUIUsers().contains(user) || !Configurations.getUIUserPassword(user).equals(password)) {
                                JSONWriter jsonWriter = new JSONWriter(out);
                                jsonWriter.object();
                                jsonWriter.key("STATUS").value("ERROR");
                                jsonWriter.key("MESSAGE").value("Unknown User");
                                jsonWriter.endObject();
                                return;
                            }
                            this.setName("Connection-" + user);

                            if ((connectionId = newConnection(user, socket)) == null) {
                                JSONWriter jsonWriter = new JSONWriter(out);
                                jsonWriter.object();
                                jsonWriter.key("STATUS").value("ERROR");
                                jsonWriter.key("MESSAGE").value("Maximum connection limit");
                                jsonWriter.endObject();
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
                                            JSONWriter jsonWriter = new JSONWriter(out);
                                            jsonWriter.object();
                                            jsonWriter.key("STATUS").value("SUCCESS");
                                            jsonWriter.key("DATA").value(val);
                                            jsonWriter.endObject();
                                        }
                                        out.print("\n");
                                        out.flush();
                                    }
                                    Thread.sleep(Configurations.getUITimeout());
                                } catch (BaseInvadersException ex) {
                                    synchronized (subscribedConnections) {
                                        JSONWriter jsonWriter = new JSONWriter(out);
                                        jsonWriter.object();
                                        jsonWriter.key("STATUS").value("ERROR");
                                        jsonWriter.key("MESSAGE").value(ex.getMessage());
                                        jsonWriter.endObject();
                                        out.print("\n");
                                        out.flush();
                                    }
                                } catch (InterruptedException ex) {
                                } catch (Exception ex) {
                                    synchronized (subscribedConnections) {
                                        JSONWriter jsonWriter = new JSONWriter(out);
                                        jsonWriter.object();
                                        jsonWriter.key("STATUS").value("ERROR");
                                        jsonWriter.key("MESSAGE").value("Unknown server error");
                                        jsonWriter.endObject();
                                        out.print("\n");
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
        JSONObject outJson = new JSONObject();
        switch (cmd) {
            case "FRAME":
                if (gameMap.isRunning()) {
                    outJson.put("frame", gameMap.makeCopy().toJSONObject());
                } else {
                    outJson.put("frame", "reset");
                }
                break;
            case "CONFIG":
                outJson.put("config", Configurations.toJSONObject());
                break;
            default: {
                if (cmd.charAt(0) != '#') {
                    out = "Unknown Command " + line;
                    return out;
                }
            }
            break;
        }
        out = outJson.toString();
        return out;
    }

}
