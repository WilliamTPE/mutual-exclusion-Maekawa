
# Maekawa’s mutual exclusion algorithm for distributed mutual exclusion

This program is about implementing the Maekawa’s mutual exclusion algorithm for distributed mutual exclusion.

## Description
- There are three servers in the system, numbered from zero to two.
- There are five clients in the system, numbered from zero to four.
- Assume that each file is replicated on all the servers, and all replicas of a file are consistent in the beginning.
- Mutual exclusion algorithm is implemented among the clients. The clients, numbered C0 through C4, execute Maekawa’s mutual exclusion algorithm among them.
- Quorum size is equal to three.
- Quorum of client Ci consists of the set {Ci, C(i+1)mod5, C(i+2)mod5}.
- Once a client enters the critical section, it communicates with the three servers and writes to all replicas of the corresponding file. Once all three replicas of the file have been updated, the client exits the critical section.
- If any server is unwilling to perform the write, then the write will be aborted.


```lua
           +----------------+        +----------------+        +----------------+
           |     Server 0   |        |    Server 1    |        |     Server 2   |
           +----------------+        +----------------+        +----------------+
           |  Replica of F1 |        | Replica of F1  |        | Replica of F1  |
           |  Replica of F2 |        | Replica of F2  |        | Replica of F2  |
           |  Replica of F3 |        | Replica of F3  |        | Replica of F3  |
           +----------------+        +----------------+        +----------------+
                    |                         |                         |
                    |                         |                         |
                    |                         |                         |
                    |         WRITE           |         WRITE           |
                    |------------------------>|------------------------>|
                    |  Broadcast WRITE to all servers                  |
                    |<------------------------|<------------------------|
                    |                         |                         |
                    |         WRITE           |         WRITE           |
                    |------------------------>|------------------------>|
                    |  Broadcast WRITE to all servers                  |
                    |<------------------------|<------------------------|
                    |                         |                         |
                    |         WRITE           |         WRITE           |
                    |------------------------>|------------------------>|
                    |  Broadcast WRITE to all servers                  |
                    |<------------------------|<------------------------|
                    |                         |                         |
                    |    Inform client of     |    Inform client of     |
                    |    completion of WRITE  |    completion of WRITE  |
           +----------------+        +----------------+        +----------------+
           |     Client 0   |        |    Client 1    |        |     Client 2   |
           +----------------+        +----------------+        +----------------+

```




## Run Locally

Start the program

```bash
  bash init.sh
```

At the beginning, you are required to compile the java file by selecting mode
```bash
0
```

Then you can choose server or client mode to execute.

- For Server, you need to activate from server 0 to 2. You cannot activate more than 3 servers.

- For Client, you need to activate from client 0 to 4.


## Features

#### Server-side supported command
- `start`
    - Delete previous folder and data.
    - Create a whole new folder and file.
- `connectionlist`
    - You can check the socket that connect to current server.
- `exit`
    - End the program.

#### Client-side supported command

- `setup`
    - Establish socket connection to all clients.
- `setupserver`
  - Establish socket connection to all servers.
- `connectlist`
    - Check the client socket connection status.
- `send`
    - Send WRITE request to designated quorum set
    - Timestamp format: <clientID, HH.MM.SS.XXXXXX>
- `exit`
    - End the program
- `quorumlist`
  - Showing list of the quorum set


## License

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)

