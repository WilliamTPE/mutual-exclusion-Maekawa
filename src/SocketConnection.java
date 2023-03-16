import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;



public class SocketConnection {

    Socket otherClient;
    String my_id;
    String remote_id;
    BufferedReader in;
    PrintWriter out;
    Boolean Initiator;
    Client my_master;

    public String getRemote_id() {
        return remote_id;
    }

    public void setRemote_id(String remote_id) {
        this.remote_id = remote_id;
    }

    public SocketConnection(Socket otherClient, String myId, Boolean Initiator, Client my_master) {
        this.otherClient = otherClient;
        this.my_id = myId;
        this.my_master = my_master;
        try{
           in = new BufferedReader(new InputStreamReader(this.otherClient.getInputStream()));
           out = new PrintWriter(this.otherClient.getOutputStream(), true);
        }
        catch (Exception e){

        }

        try {
            if(!Initiator) {
                out.println("SEND_ID");
                //System.out.println("SEND_ID request sent");
                remote_id = in.readLine();
                //System.out.println("SEND_ID request response received with ID: " + remote_id);
            }
        }

        catch (Exception e){

        }
        Thread read = new Thread(){
            public void run(){
                while(rx_cmd(in,out) != 0) { }
            }
        };
        read.setDaemon(true); 	// terminate when main ends
        read.start();
    }


    public int rx_cmd(BufferedReader cmd,PrintWriter out) {
        try {
            String cmd_in = cmd.readLine();
            if(cmd_in.equals("TEST_C")){
                String context = cmd.readLine();
                String clientId = cmd.readLine();
                System.out.println("Message: " + context + " from " + clientId);
            }

            else if(cmd_in.equals("SEND_ID")){
                out.println(this.my_id);
            }

            else if(cmd_in.equals("SEND_CLIENT_ID")){
                out.println(this.my_id);
            }

            else if(cmd_in.equals("REQ")){
                String RequestingClientId = cmd.readLine();
                String FileName = cmd.readLine();
                my_master.processRequest(RequestingClientId,FileName);
            }
            else if(cmd_in.equals("REP")){
                String ReplyingClientId = cmd.readLine();
                String FileName = cmd.readLine();
                my_master.processReply(ReplyingClientId,FileName);
            }

            else if(cmd_in.equals("WRITE_TO_FILE_ACK")){
                String fileName = cmd.readLine();
                String serverId = cmd.readLine();
                System.out.println("Server" + serverId + " write file" + fileName +" DONE.");
                my_master.processWriteAck(fileName);
            }

            else if (cmd_in.equals("RELEASE")){
                System.out.println("Process release");
                String releaseClientId = cmd.readLine();
                my_master.processRelease(releaseClientId);
            }
            else if (cmd_in.equals("PREPARE_DONE")){
                String serverID = cmd.readLine();
                String fileName = cmd.readLine();
                System.out.println("Server" + serverID + " preparation completed.");
                my_master.processCommit(fileName);
            }

        }
        catch (Exception e){}
        return 1;
    }


    public synchronized void write(String fileName, String TS){
        System.out.println("Commit file" + fileName + ".txt to server" + this.getRemote_id());
        out.println("WRITE_TO_FILE");
        out.println(fileName);
        out.println(TS);
    }


    public Socket getOtherClient() {
        return otherClient;
    }

    public void setOtherClient(Socket otherClient) {
        this.otherClient = otherClient;
    }


    public synchronized void publishServer() {
        System.out.println("\n Sent.");
        out.println("TEST_S");
        out.println("test message_s");
        out.println(this.my_id);
    }

    public synchronized void publishClient() {
        System.out.println("\n Sent.");
        out.println("TEST_C");
        out.println("test message_c");
        out.println(this.my_id);
    }

    public synchronized void sendREQ(String fileName) {
        out.println("REQ");
        out.println(this.my_id);
        out.println(fileName);
    }

    public synchronized void reply(String fileName) {
        out.println("REP");
        out.println(this.my_id);
        out.println(fileName);
    }

    public synchronized void sendRelease() {
        out.println("RELEASE");
        out.println(this.my_id);
    }

    public synchronized void prepare(String fileName) {
        System.out.println("Send PREPARE message to server" + this.getRemote_id());
        out.println("PREPARE");
        out.println(this.my_id);
        out.println(fileName);
    }
}


