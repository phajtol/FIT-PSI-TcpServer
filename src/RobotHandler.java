import java.io.*;
import java.net.Socket;

/**
 * Class that takes care of navigating one robot.
 */
public class RobotHandler implements Runnable {
    private String robotName;
    private Position currentPosition;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private static final int SERVER_KEY = 54621;
    private static final int CLIENT_KEY = 45328;
    private static final int TIMEOUT = 1000;
    private static final int CHARGE_TIMEOUT = 5000;


    RobotHandler(Socket socket_) throws IOException {
        socket = socket_;

        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(TIMEOUT);

        output = new PrintWriter(socket.getOutputStream());
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    /**
     * Main method, this is ran when new robot is connected.
     */
    @Override
    public void run() {

        try {
            if(authentificate())
                navigate();

        } catch (IOException e) {
            System.out.println("Error: Communication error! (" + e + ")");
            close();
        } catch (SyntaxException e){
            output.printf("%s%c%c", Response.SERVER_SYNTAX_ERROR, 7, 8).flush();
            System.out.println("Error: Syntax error! (" + e + ")");
            close();
        } catch (LogicException e){
            System.out.println("Error: Logic error! (" + e + ")");
            output.printf("%s%c%c", Response.SERVER_LOGIC_ERROR, 7, 8).flush();
            close();
        }
    }

    /**
     * Navigates robot to center area, then searches that area for message
     * @throws IOException
     * @throws SyntaxException
     * @throws LogicException
     */
    private void navigate() throws IOException, SyntaxException, LogicException {
        int x1, y1, x2, y2;
        String buffer;


        //determine current position and orientation by telling to move twice and parsing responses
        output.printf("%s%c%c", Response.SERVER_MOVE, 7, 8).flush();

        buffer = readFromClient(14);
        if(!buffer.matches("^OK -?[0-9]+ -?[0-9]+")) throw new SyntaxException("Syntax error!");

        IntPair ip = parseXYFromOKResponse(buffer);
        x1 = ip.x;
        y1 = ip.y;

        output.printf("%s%c%c", Response.SERVER_MOVE, 7, 8).flush();

        buffer = readFromClient(14);
        if(!buffer.matches("^OK -?[0-9]+ -?[0-9]+")) throw new SyntaxException("Syntax error!");

        ip = parseXYFromOKResponse(buffer);
        x2 = ip.x;
        y2 = ip.y;

        Orientation o;
        if(x1 == x2)
            o = y2 < y1 ? Orientation.DOWN : Orientation.UP;
        else
            o = x2 > x1 ? Orientation.RIGHT : Orientation.LEFT;

        currentPosition = new Position(x2, y2, o);


        //navigating robot into center area
        while(Math.abs(currentPosition.pos.x) > 2 || Math.abs(currentPosition.pos.y) > 2){
            if(Math.abs(currentPosition.pos.x) <= 2){
                if(currentPosition.pos.y < 0) {
                    while(currentPosition.orientation != Orientation.UP)
                        turn_left();
                    move();
                } else {
                    while(currentPosition.orientation != Orientation.DOWN)
                        turn_left();
                    move();
                }
            } else {
                if(currentPosition.pos.x < 0) {
                    while(currentPosition.orientation != Orientation.RIGHT)
                        turn_left();
                    move();
                } else {
                    while(currentPosition.orientation != Orientation.LEFT)
                        turn_left();
                    move();
                }
            }
        }


        //getting robot into corner of center area
        if(currentPosition.pos.x != 2 && currentPosition.pos.x != -2){
            if(currentPosition.pos.x < 0){
                while(currentPosition.orientation != Orientation.LEFT)
                    turn_left();

                while(currentPosition.pos.x != -2)
                    move();

            } else {
                while(currentPosition.orientation != Orientation.RIGHT)
                    turn_left();

                while(currentPosition.pos.x != 2)
                    move();

            }
        }
        if(currentPosition.pos.y != 2 && currentPosition.pos.y != -2){
            if(currentPosition.pos.y < 0){
                while(currentPosition.orientation != Orientation.DOWN)
                    turn_left();
                while(currentPosition.pos.y != -2)
                    move();
            } else {
                while(currentPosition.orientation != Orientation.UP)
                    turn_left();
                while(currentPosition.pos.y != 2)
                    move();
            }
        }


        //setting robot's orientation for search
        if(currentPosition.pos.x == 2)
            while(currentPosition.orientation != Orientation.LEFT)
                turn_left();
        else if (currentPosition.pos.x == -2)
            while (currentPosition.orientation != Orientation.RIGHT)
                turn_left();


        Position startingPos = new Position(currentPosition.pos.x, currentPosition.pos.y, currentPosition.orientation);


        //searching center area for message
        while(true){
            output.printf("%s%c%c", Response.SERVER_PICK_UP, 7, 8).flush();
            buffer = readFromClient(100);
            if(!buffer.equals("")) break;

            move();

            //turn robot if he is about to get out of center area
            if(currentPosition.pos.x == 2){
                output.printf("%s%c%c", Response.SERVER_PICK_UP, 7, 8).flush();
                buffer = readFromClient(100);
                if(!buffer.equals("")) break;

                if(startingPos.pos.y == 2){
                    turn_right();
                    move();
                    turn_right();
                } else {
                    turn_left();
                    move();
                    turn_left();
                }
            } else if(currentPosition.pos.x == -2){
                output.printf("%s%c%c", Response.SERVER_PICK_UP, 7, 8).flush();
                buffer = readFromClient(100);
                if(!buffer.equals("")) break;

                if(startingPos.pos.y == 2){
                    turn_left();
                    move();
                    turn_left();
                } else {
                    turn_right();
                    move();
                    turn_right();
                }
            }
        }

        System.out.println(robotName + ": Message found: \"" + buffer + "\"");

        output.printf("%s%c%c", Response.SERVER_LOGOUT, 7, 8).flush();
        close();
        System.out.println(robotName + ": Logged out.");
    }

    private boolean authentificate() throws IOException, SyntaxException, LogicException {

        String buffer = readFromClient(12);

        if(buffer.length() >= 11) return false;

        robotName = buffer;

        int hash = 0;
        int serverHash;

        for(char c : robotName.toCharArray())
            hash += c;
        hash *= 1000;
        hash %= 65536;
        serverHash = (hash + SERVER_KEY) % 65536;

        output.printf("%d%c%c", serverHash, 7, 8);
        output.flush();

        buffer = readFromClient(14);

        String expected = Integer.toString((hash + CLIENT_KEY) % 65536);

        if(expected.equals(buffer)) {
            output.printf("%s%c%c", Response.SERVER_OK, 7, 8).flush();
            System.out.println(robotName + ": Authentification successful!");
            return true;
        } else {
            if(!buffer.matches("[0-9]{1,5}")) throw new SyntaxException("Syntax error!");
            output.printf("%s%c%c", Response.SERVER_LOGIN_FAILED, 7, 8);
            output.flush();
            System.out.println(robotName + ": Authentification failed!");
            this.close();
            return false;
        }
    }

    private void close() {
        try {
            if(input != null) input.close();
            if(output != null) output.close();
            if(socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error: Could not close all resources! (" + e + ")");
        }
    }

    /**
     * @param maxLength - maximum length of message, including terminating characters
     * @return - robot's response, without terminating characters
     * @throws IOException
     * @throws SyntaxException
     * @throws LogicException
     */
    private String readFromClient(int maxLength) throws IOException, SyntaxException, LogicException {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < maxLength; i++){
            char c = (char) input.read();

            if(c == 7) {
                char next = (char) input.read();
                if(next == 8) break;
                else {
                    builder.append(c);
                    builder.append(next);
                    i++;
                }
            }
            else builder.append(c);

            if(i == maxLength - 2) throw new SyntaxException("Syntax error!");
        }

        if(builder.toString().equals("RECHARGING")) {
            charge();
            return readFromClient(maxLength);
        }

        return builder.toString();
    }

    /**
     * Commands robot to move forward. Makes sure robot moved once by checking positions.
     * @throws IOException
     * @throws SyntaxException
     * @throws LogicException
     */
    private void move() throws IOException, SyntaxException, LogicException {
        String buffer;
        IntPair after = new IntPair(currentPosition.pos);

        while(currentPosition.pos.x == after.x && currentPosition.pos.y == after.y) {
            output.printf("%s%c%c", Response.SERVER_MOVE, 7, 8).flush();
            buffer = readFromClient(14);
            after = parseXYFromOKResponse(buffer);
        }

        if(currentPosition.orientation == Orientation.UP)
            currentPosition.pos.y++;
        else if(currentPosition.orientation == Orientation.DOWN)
            currentPosition.pos.y--;
        else if(currentPosition.orientation == Orientation.LEFT)
            currentPosition.pos.x--;
        else
            currentPosition.pos.x++;
    }

    private void turn_left() throws IOException, SyntaxException, LogicException {
        output.printf("%s%c%c", Response.SERVER_TURN_LEFT, 7, 8).flush();
        readFromClient(14);
        currentPosition.orientation = Orientation.left(currentPosition.orientation);
    }

    private void turn_right() throws IOException, SyntaxException, LogicException {
        output.printf("%s%c%c", Response.SERVER_TURN_RIGHT, 7, 8).flush();
        readFromClient(14);
        currentPosition.orientation = Orientation.right(currentPosition.orientation);
    }

    /**
     * Handles robot's charging
     * @throws IOException
     * @throws SyntaxException
     * @throws LogicException
     */
    private void charge() throws IOException, SyntaxException, LogicException {
        socket.setSoTimeout(CHARGE_TIMEOUT);
        String buffer = readFromClient(12);
        socket.setSoTimeout(TIMEOUT);

        if(!buffer.equals("FULL POWER")) throw new LogicException("Logic error");
    }

    /**
     * @param response - response from robot, general syntax: "OK <x> <y>"
     * @return - x and y from source wrapped in IntPair class
     * @throws SyntaxException - when x and y cannot be parsed
     */
    private IntPair parseXYFromOKResponse(String response) throws SyntaxException {
        int x, y;

        String firstCoordinate = response.substring(
                response.indexOf(' ') + 1,
                response.indexOf(' ', response.indexOf(' ') + 1)
        );

        String secondCoordinate = response.substring(
                response.indexOf(firstCoordinate) + firstCoordinate.length() + 1,
                response.length()
        );

        try {
            x = Integer.parseInt(firstCoordinate);
            y = Integer.parseInt(secondCoordinate);
        } catch (NumberFormatException e) {
            throw new SyntaxException("Syntax error!");
        }

        return new IntPair(x, y);
    }

}
