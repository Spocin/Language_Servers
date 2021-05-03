# Language Servers
![](https://img.shields.io/badge/display%20type-hybrid-brightgreen)

This project was created as a part of the subject: **Technologie programowania rozproszonego**

This project implements network of servers that separates the client-server connectivity through proxy server.

Client sends a request to the proxy to translate given word to given language. If proxy accepts request, it is forwarded to the adequate Language Server. Depending on given word, server responds straight to the client with translation or appropriate massage.

Exemplary dictionaries for Language Servers can be found inside: `Language_Server/src/Data`

**So far project is implemented for use within one machine.**

![](https://i.imgur.com/FpMn7Oa.png)

## Reqirements
- Java installed on your machine
- JavaFX somewhere on your machine

## Instalation
All JARs should be turned on using command line.

### Proxy
- Go to `Artifacts/`
- Type in `java -jar Proxy.jar`

### Language Server
- Go to `Artifacts/`
- Type in `java -jar Language_Server.jar`

### Client
- Go to `Artifacts/`
- Open `Client_Start.bat` in some text editor
- Replace `"Insert path here"` with the path to the `JavaFX/lib` on your machine and save
- Type in `Client_Start`
