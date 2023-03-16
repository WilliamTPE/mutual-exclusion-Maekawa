import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ServerSocketConnection {
    Socket otherClient;
    String my_id;
    String remote_id;
    BufferedReader in;
    PrintWriter out;
    Boolean Initiator;
    Server my_master;

    public String getRemote_id() {
        return remote_id;
    }

    public void setRemote_id(String remote_id) {
        this.remote_id = remote_id;
    }

    public ServerSocketConnection(Socket otherClient, String myId, Boolean isServer,Server my_master) {
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
            if(!isServer) {
                out.println("SEND_CLIENT_ID");
                System.out.println("\nNew socket connection");
                remote_id = in.readLine();
                System.out.println(" Client" + remote_id +" connected.");
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
            if (cmd_in.equals("WRITE_TEST")) {
                System.out.println("Test write received from sender");
            }

            else if(cmd_in.equals("WRITE_TO_FILE")){

                String fileName = cmd.readLine();
                String TS = cmd.readLine();
                System.out.println("\nWriting file" + fileName +".txt");
                my_master.writeToFile(fileName, TS);
            }

            else if (cmd_in.equals("TEST_S")){
                String context = cmd.readLine();
                String clientId = cmd.readLine();
                System.out.println("Message: " + context + " from " + clientId);
            }

            else if (cmd_in.equals("PREPARE")){
                String requestClient = cmd.readLine();
                String fileName = cmd.readLine();
                my_master.askToPrepare(requestClient, fileName);
            }

        }
        catch (Exception e){}
        return 1;
    }


    public synchronized void sendWriteAcknowledge(String fileName){
        System.out.println("Sending WRITE DONE message to client" + fileName +"\n");
        out.println("WRITE_TO_FILE_ACK");
        out.println(fileName);
        out.println(this.my_id);
    }

    public synchronized void prepareDone(String fileName) {
        System.out.println("Preparation complete");
        out.println("PREPARE_DONE");
        out.println(this.my_id);
        out.println(fileName);
    }

    public Socket getOtherClient() {
        return otherClient;
    }

    public void setOtherClient(Socket otherClient) {
        this.otherClient = otherClient;
    }

}
