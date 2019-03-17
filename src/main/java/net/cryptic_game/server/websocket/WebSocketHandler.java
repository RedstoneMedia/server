package net.cryptic_game.server.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.cryptic_game.server.microservice.MicroService;
import net.cryptic_game.server.socket.SocketServerUtils;

public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	protected static List<Channel> online = new ArrayList<Channel>();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
		try {
			JSONObject obj = (JSONObject) new JSONParser().parse(frame.text());

			if (obj.containsKey("ms") && obj.get("ms") instanceof String && obj.containsKey("data")
					&& obj.get("data") instanceof JSONObject && obj.containsKey("endpoint")
					&& obj.get("endpoint") instanceof JSONArray) {
				try {
					MicroService ms = MicroService.get((String) obj.get("ms"));

					if (ms != null) {
						ms.recive(ctx.channel(), (JSONArray) obj.get("endpoint"), (JSONObject) obj.get("data"));
					}
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
			} else if (obj.containsKey("action") && obj.get("action") instanceof String) {
				String action = (String) obj.get("action");

				if (action.equals("status")) {
					Map<String, Object> status = new HashMap<String, Object>();

					status.put("online", this.getOnlineCount());

					this.respond(ctx.channel(), status);
				}
			} else {
				this.error(ctx.channel(), "invalid data");
			}
		} catch (ParseException e) {
			this.error(ctx.channel(), "unsupported format");
			ctx.channel().close();
		}
	}

	private void error(Channel channel, String error) {
		Map<String, Object> jsonMap = new HashMap<String, Object>();

		jsonMap.put("error", error);

		this.respond(channel, jsonMap);
	}

	private void respond(Channel channel, Map<String, Object> data) {
		SocketServerUtils.sendJsonToClient(channel, new JSONObject(data));
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		online.add(ctx.channel());
	}

	private int getOnlineCount() {
		return online.size();
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		online.remove(ctx.channel());
	}

}