/**
 * @author William Chang
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {

    List<Node> allServerNodes = new LinkedList<>();
    List<ServerSocketConnection> serverSocketConnectionList = new LinkedList<>();
    ServerSocket server;
    String Id;
    HashMap<String,ServerSocketConnection> serverSocketConnectionHashMap = new HashMap<>();
    Boolean lock = false;

    private String pathName;

    public Server(String id){
        this.Id = id;
    }
    private void setPathName(String id){
        pathName = "./files"+ Integer.valueOf(id);
    }
    private String getPathName(){
        return pathName;
    }

    public static void deleteDir(File file){
        File[] files = file.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        file.delete();
    }

    public static void createDir(File path){
        path.mkdir();
    }

    public static void createFile(File path) throws IOException {
        path.createNewFile();
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public List<Node> getAllServerNodes() {
        return allServerNodes;
    }

    public void setAllServerNodes(List<Node> allServerNodes) {
        this.allServerNodes = allServerNodes;
    }


    /**
     * For terminal input command
     */
    public class CommandParser extends Thread{

        Server currentServer;

        public CommandParser(Server currentServer){
            this.currentServer = currentServer;
        }

        Pattern START = Pattern.compile("^start$");
        Pattern CONNECTION_LIST = Pattern.compile(("^connectionlist$"));
        Pattern EXIT = Pattern.compile("^exit$");

        int rx_cmd(Scanner cmd){
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_START = START.matcher(cmd_in);
            Matcher m_CONNECTION_LIST = CONNECTION_LIST.matcher(cmd_in);
            Matcher m_EXIT = EXIT.matcher(cmd_in);

            if(m_START.find()){
                deleteDir(new File(currentServer.getPathName()));
                createDir(new File(currentServer.getPathName()));
                for (int i = 0;i<5;i++){
                    try {
                        createFile(new File(currentServer.getPathName()+"/"+i+".txt"));
                    } catch (IOException e) {
                        System.out.println("Create file error.");
                        e.printStackTrace();
                    }
                }
                System.out.println("Delete previous folder and Create new folder...");
            }

            else if (m_CONNECTION_LIST.find()){
                System.out.println("Number of socket connect: " + serverSocketConnectionList.size() + "\n");
                Integer i;
                for (i = 0; i < serverSocketConnectionList.size(); i++){
                    System.out.println("Socket" + serverSocketConnectionList.get(i).getRemote_id());
                    System.out.println("IP: " + serverSocketConnectionList.get(i).getOtherClient().getInetAddress() + " Port: " + serverSocketConnectionList.get(i).getOtherClient().getPort());
                }
            }

            else if (m_EXIT.find()){
                System.out.println("\n========== Server" + Id + " exit ==========\n");
                System.exit(0);
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

    public synchronized void askToPrepare(String requestClient, String fileName) {
        //this.lock = true;
        System.out.println("Prepare to write file" + fileName +".txt");
        serverSocketConnectionHashMap.get(requestClient).prepareDone(fileName);

    }

    public synchronized void writeToFile(String fileName, String timeStamp) {
        String clientID = fileName;
        try {
            //this.lock = false;
            FileWriter writer = new FileWriter("./files" + Integer.valueOf(Id) + "/" + Integer.valueOf(fileName)+".txt", true);
            writer.append(clientID + "," + timeStamp+"\n");
            writer.close();
            serverSocketConnectionHashMap.get(clientID).sendWriteAcknowledge(fileName);
        } catch (IOException e) {
            //this.lock = true;
            System.out.println("\n------Abort------\n");
            //serverSocketConnectionHashMap.get(clientID).sendWriteFailNotify(fileName);
            e.printStackTrace();
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

    public void serverSocket(Integer serverId, Server currentServer){
        try
        {
            server = new ServerSocket(Integer.valueOf(this.allServerNodes.get(serverId).port));
            Id = Integer.toString(serverId);
            InetAddress myServerIp = InetAddress.getLocalHost();
            System.out.println(" Server" + Id + " Information");
            System.out.println("       Port: " + Integer.valueOf(this.allServerNodes.get(serverId).port));
            System.out.println(" IP address: " + myServerIp.getHostAddress());
            System.out.println("   Hostname: " + myServerIp.getHostName());
        }
        catch (IOException e)
        {
            System.out.println("Error creating socket");
            System.exit(-1);
        }

        Server.CommandParser cmdpsr = new Server.CommandParser(currentServer);
        cmdpsr.start();

        Thread current_node = new Thread() {
            public void run(){
                while(true){
                    try{
                        Socket s = server.accept();
                        ServerSocketConnection serverSocketConnection = new ServerSocketConnection(s,Id, false,currentServer);
                        serverSocketConnectionList.add(serverSocketConnection);
                        serverSocketConnectionHashMap.put(serverSocketConnection.getRemote_id(),serverSocketConnection);
                    }
                    catch(IOException e){ e.printStackTrace(); }
                }
            }
        };

        current_node.setDaemon(true);
        current_node.start();
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java Server <server-number>");
            System.exit(1);
        }

        Server server = new Server(args[0]);
        System.out.println("\n========== Welcome to Server" + args[0] + " ==========");
        server.setPathName(args[0]);
        server.setServerList();
        server.serverSocket(Integer.valueOf(args[0]),server);

    }
}

