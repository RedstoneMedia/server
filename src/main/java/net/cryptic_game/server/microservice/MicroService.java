package net.cryptic_game.server.microservice;

import io.netty.channel.Channel;
import net.cryptic_game.server.client.Client;
import net.cryptic_game.server.client.Request;
import net.cryptic_game.server.config.Config;
import net.cryptic_game.server.config.DefaultConfig;
import net.cryptic_game.server.socket.SocketServerUtils;
import net.cryptic_game.server.user.User;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * microservice wrapper
 *
 * @author use-to
 */
public class MicroService {

    private static final Logger logger = Logger.getLogger(MicroService.class);

    // open requests of client
    private static Map<UUID, Request> open = new HashMap<>();

    // online microservices
    private static List<MicroService> services = new ArrayList<>();

    private String name; // name of ms
    private Channel channel; // socket-channel of ms

    public MicroService(String name, Channel channel) {
        this.name = name;
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     * Sends data to microservice
     *
     * @param client   channel of client (for response)
     * @param endpoint endpoint on ms (string-array)
     * @param input    data sending to ms
     */
    public void receive(Client client, JSONArray endpoint, JSONObject input, UUID clientTag) {
        UUID tag = UUID.randomUUID();

        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("tag", tag.toString());
        jsonMap.put("data", input);
        jsonMap.put("endpoint", endpoint);

        if (Config.getBoolean(DefaultConfig.AUTH_ENABLED)) {
            if (!client.isValid()) {
                return;
            } else {
                jsonMap.put("user", client.getUser().getUUID().toString());
            }
        } else {
            jsonMap.put("user", UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
        }

        SocketServerUtils.sendJson(this.getChannel(), new JSONObject(jsonMap));

        open.put(tag, new Request(client, clientTag));
        new Thread(() -> {
            try {
                Thread.sleep(1000 * Config.getInteger(DefaultConfig.RESPONSE_TIMEOUT));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(open.containsKey(tag)) {
                Map<String, String> map = new HashMap<>();

                map.put("error", "no response - timeout");

                open.remove(tag).send(new JSONObject(map));
            }
        }).start();
	}

    /**
     * Sends data back to client
     *
     * @param output data from microservice
     * @return success
     */
    public boolean send(JSONObject output) {
        try {
            if (output.containsKey("data") && output.get("data") instanceof JSONObject) {
                JSONObject data = (JSONObject) output.get("data");

                if (output.containsKey("tag") && output.get("tag") instanceof String) {
                    UUID tag = UUID.fromString((String) output.get("tag"));

                    if (output.containsKey("ms") && output.get("ms") instanceof String && output.containsKey("endpoint")
                            && output.get("endpoint") instanceof JSONArray) {
                        MicroService ms = MicroService.get((String) output.get("ms"));

                        if (ms != null) {
                            ms.receiveFromMicroService(this, (JSONArray) output.get("endpoint"), tag, data);
                            return true;
                        }
                    } else {
                        Request req = open.get(tag);
                        if (req != null) {
                            req.send(data);
                            return true;
                        }
                    }
                }
            }
        } catch (ClassCastException e) {
        }
        return false;
    }

    /**
     * Receives data from another microservice no requests
     *
     * @param ms   microservice
     * @param data data of the sender
     */
    private void receiveFromMicroService(MicroService ms, JSONArray endpoint, UUID tag, JSONObject data) {
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("ms", ms.getName());
        jsonMap.put("endpoint", endpoint);
        jsonMap.put("tag", tag.toString());
        jsonMap.put("data", data);

        SocketServerUtils.sendJson(this.getChannel(), new JSONObject(jsonMap));
    }

    /**
     * Registers a microservice
     *
     * @param name    name of the microservice
     * @param channel channel of the microservice
     */
    public static void register(String name, Channel channel) {
        services.add(new MicroService(name, channel));
        logger.info("microservice registered: " + name);
    }

    /**
     * Unregisters a microservice
     *
     * @param channel channel of the microservice
     */
    public static void unregister(Channel channel) {
        MicroService ms = MicroService.get(channel);

        if (ms != null) {
            services.remove(ms);
            logger.info("microservice unregistered: " + ms.getName());
        }
    }

    /**
     * @param name name of the microservice
     * @return the microservice by name or null
     */
    public static MicroService get(String name) {
        for (MicroService ms : services) {
            if (ms.getName().equals(name)) {
                return ms;
            }
        }
        return null;
    }

    /**
     * @param channel channel of the microservice
     * @return the microservice by channel or null
     */
    public static MicroService get(Channel channel) {
        for (MicroService ms : services) {
            if (ms.getChannel().equals(channel)) {
                return ms;
            }
        }
        return null;
    }

    public static void sendToUser(UUID user, JSONObject data) {
        User userAccount = User.get(user);

        if (userAccount != null) {
            Client clientOfUser = Client.getClient(userAccount);

            if (clientOfUser != null) {
                clientOfUser.send(data);
            }
        }
    }

}
