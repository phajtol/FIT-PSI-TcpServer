import java.io.IOException;
import java.net.*;

/**
 *
 */
public class Server {


    /**
     *
     * @param args
     */
   public static void main(String[] args){
        ServerSocket socket = null;

        try {
            socket = new ServerSocket(6655);
        } catch (IOException e){
            System.out.println("Error: Cannot initialize socket! (" + e + ")");
            return;
        }

        //main server loop, waiting for clients and hadling them
        while(true){
            Socket client;

            try {
                client = socket.accept();
                System.out.println("Robot connected!");
            } catch (IOException e){
                System.out.println("Error: Cannot accept connection! (" + e + ")");
                return;
            }

            RobotHandler handler = null;

            try {
                handler = new RobotHandler(client);
            } catch (IOException e){
                System.out.println("Error: Cannot initialize RobotHandler! " + e + ")");
            }

            new Thread(handler).start();
        }

   }

}
