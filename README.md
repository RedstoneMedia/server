# game-server

The official game-server of cryptic-game.  
This system connects the microservices with clients via json-based sockets.

## Run!

### Build..

Build the game-server by using `mvn clean install` .  

### Execute

Run it with `java -jar target/server-0.0.1-SNAPSHOT.jar` .

### Environment variables

| key            | default value |
| -------------- | ------------- |
| MSSOCKET_HOST  | 127.0.0.1     |
| MSSOCKET_PORT  | 1239          |
| WEBSOCKET_HOST | 0.0.0.0       |
| WEBSOCKET_PORT | 80            |
| HTTP_PORT      | 8080          |

## Docker

### Docker-Hub

Work in progres...

## documentation

Visit the [wiki](https://github.com/cryptic-game/server/wiki) for more information.
