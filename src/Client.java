/**
 * @author William Chang
 */
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    String Id;
    List<Node> allClientNodes = new LinkedList<>();
    List<Node> allServerNodes = new LinkedList<>();
    Integer logicalClock = 0;
    List<SocketConnection> socketConnectionList = new LinkedList<>();
    List<SocketConnection> socketConnectionListServer = new LinkedList<>();
    ServerSocket server;
    HashMap<String,SocketConnection> socketConnectionHashMap = new HashMap<>();
    HashMap<String,SocketConnection> socketConnectionHashMapServer = new HashMap<>();
    HashMap<String,Boolean> clientPermissionRequired = new HashMap<>();
    List<List<String>> quorum = new LinkedList<>();
    Integer noOfServer = 0;
    Integer currentQuorumIndex = 0;
    Integer outStandingGrantCount = 0;

    String requestedCSForFile;

    Queue<RequestClient> requestClientQueue = new LinkedList<>();
    Integer writeAckCount = 0;
    Integer prepareAckCount = 0;
    //state
    Boolean requestedCS = false;
    Boolean usingCS = false;
    Boolean voted = false;

    public Client(String id) {
        this.Id = id;
    }

    public String getId() {
        return this.Id;
    }

    public void setId(String id) {
        this.Id = id;
    }

    public List<Node> getAllClientNodes() {
        return allClientNodes;
    }

    public void setAllClientNodes(List<Node> allClientNodes) {
        this.allClientNodes = allClientNodes;
    }

    public List<Node> getAllServerNodes() {
        return allServerNodes;
    }

    public void setAllServerNodes(List<Node> allServerNodes) {
        this.allServerNodes = allServerNodes;
    }

    public Integer getLogicalClock() {
        return logicalClock;
    }

    public void setLogicalClock(Integer logicalClock) {
        this.logicalClock = logicalClock;
    }




    /**
     * For terminal input command
     */
    public class CommandParser extends Thread{

        Client current;

        public CommandParser(Client current){
            this.current = current;
        }

        Pattern SETUP = Pattern.compile("^setup$"); //set up socket connection with other clients
        Pattern SERVER_SETUP = Pattern.compile("^setupserver$"); //set up socket connection with all three servers
        Pattern TEST = Pattern.compile("^test$"); //request to write file
        Pattern EXIT = Pattern.compile("^exit$");
        Pattern SEND = Pattern.compile("^send$");
        Pattern SERVER_TEST = Pattern.compile("^testserver$");
        Pattern CLIENT_TEST = Pattern.compile("^testclient$");
        Pattern CONNECTION_DETAIL = Pattern.compile("^connectlist$");
        Pattern QUORUM_LIST = Pattern.compile("^quorumlist");

        int rx_cmd(Scanner cmd){
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_SETUP = SETUP.matcher(cmd_in);
            Matcher m_SERVER_SETUP = SERVER_SETUP.matcher(cmd_in);
            Matcher m_TEST = TEST.matcher(cmd_in);
            Matcher m_EXIT = EXIT.matcher(cmd_in);
            Matcher m_SEND = SEND.matcher(cmd_in);
            Matcher m_SERVER_TEST = SERVER_TEST.matcher(cmd_in);
            Matcher m_CLIENT_TEST = CLIENT_TEST.matcher(cmd_in);
            Matcher m_CONNECTION_DETAIL = CONNECTION_DETAIL.matcher(cmd_in);
            Matcher m_QUORUM_LIST = QUORUM_LIST.matcher(cmd_in);

            /**
             * Setup connection with client
             */
            if(m_SETUP.find()){
                setupConnections(current);
            }

            else if (m_SERVER_SETUP.find()){
                setupServerConnection(current);
            }


            /**
             * exit the client
             */
            else if (m_EXIT.find()){
                System.out.println("\n========== Client" + Id + " exit ==========\n");
                System.exit(0);
            }


            /**
             * send timestamp request
             */
            else if (m_SEND.find()){
                send();
            }

            else if (m_SERVER_TEST.find()){
                testServer();
            }

            else if (m_CLIENT_TEST.find()){
                testClient();
            }

            else if (m_CONNECTION_DETAIL.find()){
                System.out.println("\n List of socket connection");
                System.out.println(" Number of socket connection: " + socketConnectionList.size());
                Integer i;
                for (i = 0; i < socketConnectionList.size(); i++){
                    System.out.println("Socket: " + socketConnectionList.get(i).getRemote_id());
                    System.out.println("IP: " + socketConnectionList.get(i).getOtherClient().getInetAddress() + " Port: " + socketConnectionList.get(i).getOtherClient().getPort());
                }
                for (String key: socketConnectionHashMap.keySet()){
                    System.out.println("ClientID: " + key + " Socket: " + socketConnectionHashMap.get(key).getOtherClient().getPort());
                }
            }

            else if (m_QUORUM_LIST.find()){
                showQuorum();
            }

            return 1;
        }

        public void run() {
            System.out.print("\n>>");
            Scanner input = new Scanner(System.in);
            while(rx_cmd(input) != 0) {
                System.out.print("\n>>");
            }
        }
    }

    public void send() {
        String fileName = Id;
        sendRequest(fileName);
    }

    public void showQuorum() {
        Integer quorumId;
        for (quorumId = 0; quorumId < this.quorum.size(); quorumId++){
            System.out.println("Quorum at ID: " + quorumId + " with quorum members: "+this.quorum.get(quorumId).toString());
        }
    }

    private void testClient() {
        System.out.println("Sending TEST message to other clients");
        Integer i;
        for (i = 0; i < socketConnectionList.size(); i++){
            socketConnectionList.get(i).publishClient();
        }
    }

    private void testServer() {
        System.out.println("Sending TEST message to servers");
        Integer i;
        for (i = 0; i < this.socketConnectionListServer.size(); i++){
            socketConnectionListServer.get(i).publishServer();
        }
    }

    /**
     * send request to other process in specific quorum
     * @param fileName
     */
    public synchronized void sendRequest(String fileName){
        this.requestedCS = true;
        this.requestedCSForFile = fileName;

        Integer quorumSetIndex = Integer.valueOf(fileName);
        List<String> quorumMembers = quorum.get(quorumSetIndex);

        this.currentQuorumIndex = Integer.parseInt(fileName);
        Integer quorumMemberId ;
        this.outStandingGrantCount = quorumMembers.size()-1;

        for(quorumMemberId = 0; quorumMemberId < quorumMembers.size(); quorumMemberId++){
            System.out.println("Sending request to client" + quorumMembers.get(quorumMemberId));
            if (socketConnectionHashMap.containsKey(quorumMembers.get(quorumMemberId))){
                socketConnectionHashMap.get(quorumMembers.get(quorumMemberId)).sendREQ(fileName);
            }
        }
    }

    public synchronized void processRequest(String requestingClientId, String fileName) {
        if (fileName.equals(this.requestedCSForFile)){
            System.out.println("Waiting for other processes reply");
            SocketConnection requestingSocketConnection = socketConnectionHashMap.get(requestingClientId);
            requestingSocketConnection.reply(fileName);
        } else {
            if (this.usingCS || this.voted){
                requestClientQueue.add(new RequestClient(requestingClientId));
                //requestClientPriorityQueue.add(new RequestClient(requestingClientId, Long.valueOf(requestTimeStamp)));
            } else {
                this.voted = true;
                SocketConnection requestingSocketConnection = socketConnectionHashMap.get(requestingClientId);
                requestingSocketConnection.reply(fileName);
            }
        }
    }

    public synchronized void processReply(String replyingClientId, String fileName) {
        System.out.print("\n");
        if(fileName.equals(this.requestedCSForFile)){
            System.out.println("Received reply message from: client" + replyingClientId);
            this.outStandingGrantCount -= 1;
            if(this.outStandingGrantCount == 0){
                enterCS(fileName);
            }
        }
        else{
            System.out.println("You are not the requesting site => NO ACTION TAKEN");
        }
    }

    public void enterCS(String fileName) {
        System.out.println("\n========Enter critical section=======");

        this.usingCS = true;
        this.requestedCS = false;
        this.writeAckCount = this.noOfServer;
        this.prepareAckCount = this.noOfServer;

        Integer serverConnectIndex;

        for (serverConnectIndex = 0; serverConnectIndex < this.socketConnectionListServer.size(); serverConnectIndex ++){
            this.socketConnectionListServer.get(serverConnectIndex).prepare(fileName);
        }

    }

    public synchronized void processCommit(String fileName) {
        
        this.prepareAckCount -= 1;
        if(fileName.equals(this.requestedCSForFile)){
            if (this.prepareAckCount == 0){
                System.out.println("\nAll servers prepare complete");
                commit(fileName);
            }
        }
    }
    public void commit(String fileName){
        String ts = String.valueOf(LocalTime.now());
        Integer serverConnectIndex;
        try {
            for (serverConnectIndex = 0; serverConnectIndex < this.socketConnectionListServer.size() ; serverConnectIndex ++){
                this.socketConnectionListServer.get(serverConnectIndex).write(fileName, ts);
            }
        } catch (Exception e) {
            System.out.println("File write error");
        }
    }

    public synchronized void processWriteAck(String fileName) {
        if(fileName.equals(this.requestedCSForFile)){
            this.writeAckCount -= 1;
            if (this.writeAckCount == 0 ){
                System.out.println("All servers write "+ fileName + ".txt complete." );
                exitCS();
            }
        }
    }

    private void exitCS() {
        System.out.println("\n===========Exit critical section============");
        this.requestedCS = false;
        this.usingCS = false;
        List<String> quorumMembers = quorum.get(this.currentQuorumIndex);
        Integer quorumMemberId;
        System.out.println("Multicast RELEASE to other processes\n");
        for (quorumMemberId = 0; quorumMemberId < quorumMembers.size(); quorumMemberId++){
            if (socketConnectionHashMap.containsKey(quorumMembers.get(quorumMemberId))){
                socketConnectionHashMap.get(quorumMembers.get(quorumMemberId)).sendRelease();
            }
        }
    }

    public void processRelease(String releaseClientId) {
        if (releaseClientId.equals(this.requestedCSForFile)){
            System.out.println("Releasing CS...");
        } else {
            if (requestClientQueue.isEmpty()){
                this.voted = false;
            } else {
                this.voted = true;
                String nextClient = requestClientQueue.peek().clientId;
                socketConnectionHashMap.get(requestClientQueue.remove().clientId).reply(nextClient);
            }
        }
    }

    public void setupConnections(Client current){
        try {
            System.out.println("Connecting to neighbor clients...");
            Integer clientId;
            for(clientId = Integer.valueOf(this.Id) + 1; clientId < allClientNodes.size(); clientId ++ ) {
                Socket clientConnection = new Socket(this.allClientNodes.get(clientId).getIpAddress(), Integer.valueOf(allClientNodes.get(clientId).getPort()));
                SocketConnection socketConnection = new SocketConnection(clientConnection, this.getId(), true,current);
                if(socketConnection.getRemote_id() == null){
                    socketConnection.setRemote_id(Integer.toString(clientId));
                }
                socketConnectionList.add(socketConnection);
                socketConnectionHashMap.put(socketConnection.getRemote_id(),socketConnection);
                clientPermissionRequired.put(socketConnection.getRemote_id(),true);
            }
        }
        catch (Exception e){

        }
    }

    public void setupServerConnection(Client current){
        try{
            System.out.println("\nConnecting to servers...");
            Integer serverId;
            for (serverId =0; serverId < allServerNodes.size(); serverId ++){
                Socket serverConnection = new Socket(this.allServerNodes.get(serverId).getIpAddress(), Integer.valueOf(this.allServerNodes.get(serverId).getPort()));
                SocketConnection socketConnectionServer = new SocketConnection(serverConnection,this.getId(),true,current);
                if(socketConnectionServer.getRemote_id() == null){
                    socketConnectionServer.setRemote_id(Integer.toString(serverId));
                }
                socketConnectionListServer.add(socketConnectionServer);
                socketConnectionHashMapServer.put(socketConnectionServer.getRemote_id(),socketConnectionServer);
            }

            this.noOfServer = socketConnectionListServer.size();
        }
        catch (Exception e){
            System.out.println("Setup Server Connection Failure");
        }

    }

    public void clientSocket(Integer ClientId, Client current){
        try
        {
            server = new ServerSocket(Integer.valueOf(this.allClientNodes.get(ClientId).port));
            Id = Integer.toString(ClientId);
            System.out.println(" Client" + Id +" Information:");
            InetAddress myip = InetAddress.getLocalHost();
            System.out.println("       Port: " + Integer.valueOf(this.allClientNodes.get(ClientId).port));
            System.out.println(" IP address: " + myip.getHostAddress());
            System.out.println("   Hostname: " + myip.getHostName());
        }
        catch (IOException e)
        {
            System.out.println("Error creating socket");
            System.exit(-1);
        }

        CommandParser cmdpsr = new CommandParser(current);
        cmdpsr.start();

        Thread current_node = new Thread() {
            public void run(){
                while(true){
                    try{
                        Socket s = server.accept();
                        SocketConnection socketConnection = new SocketConnection(s,Id,false, current);
                        socketConnectionList.add(socketConnection);
                        socketConnectionHashMap.put(socketConnection.getRemote_id(),socketConnection);
                        clientPermissionRequired.put(socketConnection.getRemote_id(),true);
                    }
                    catch(IOException e){ e.printStackTrace(); }
                }
            }
        };

        current_node.setDaemon(true);
        current_node.start();
    }

    public void setClientList(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("ClientAddressAndPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    List<String> parsed_client = Arrays.asList(line.split(","));
                    Node n_client= new Node(parsed_client.get(0),parsed_client.get(1),parsed_client.get(2));
                    this.getAllClientNodes().add(n_client);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                //System.out.println(everything);
                //System.out.println(this.getAllClientNodes().size());

            } finally {
                br.close();
            }
        }
        catch (Exception e) {
        }
    }

    public void setServerList(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("ServerAddressAndPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server = Arrays.asList(line.split(","));
                    Node n_server = new Node(parsed_server.get(0),parsed_server.get(1),parsed_server.get(2));
                    this.getAllServerNodes().add(n_server);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                //System.out.println(everything);
                //System.out.println(this.getAllServerNodes().size());

            } finally {
                br.close();
            }
        }
        catch (Exception e) {
        }
    }
    private void setQuorumList() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("quorum_config.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    List<String> parsed_quorum = Arrays.asList(line.split(","));
                    this.quorum.add(parsed_quorum);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        }
        catch (Exception e) {
            System.out.println("Something went wrong while parsing the quorum" + e);
        }
    }

    /**
     * 1. Read server config file into the allServerNodes list
     * 2. Read client config file into the allClientNodes list
     * 3. Initiating client socket
     * @param args client id number
     */
    public static void main(String[] args) {

        if (args.length != 1)
        {
            System.out.println("Usage: java Client <client-number>");
            System.exit(1);
        }

        Client clientSide = new Client(args[0]);
        System.out.println("\n========== Welcome to Client" + args[0] + " ==========");
        clientSide.setClientList();
        clientSide.setServerList();
        clientSide.setQuorumList();
        clientSide.setupServerConnection(clientSide);
        clientSide.clientSocket(Integer.valueOf(args[0]),clientSide);
    }
}
