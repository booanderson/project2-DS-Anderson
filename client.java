import java.io.*;
import java.net.Socket;

class Client {
    //setting servers ip to the local host
    private static final String SERVER_ADDRESS = "localhost"; 
    //setting port for the server 
    private static final int PORT = 5000; 
    //setting the directory for the clients files
    private static final String FILE_DIRECTORY = "client_files/"; 

    public static void main(String[] args) {
        //attempting to connect to the server
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
	     //creating a buffer to read from the server
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	     //creating bugger to write to
	     //creating a buffer to read from the clietn console
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            //getting the OTP from the server
            System.out.println(in.readLine());
	    //getting the OTP input from user
            String enteredOtp = console.readLine(); 
	    //sending the OTP to the server
            out.println(enteredOtp); 

            //getting the authentication boolean from server and printing hello
            System.out.println(in.readLine()); 
            System.out.println(in.readLine()); 

	    //loop to run until exit command from user
            String command;
            while (true) {
                //initial line value for each line of console
		System.out.print("> "); 
		//getting the users command from client console
                command = console.readLine();
		//sending the command to the server
                out.println(command); 

                //checking which command was run 
		//if put command
                if (command.startsWith("put ")) {
                    String filename = command.split(" ")[1];
                    //send file to server
		    sendFile(filename); 
		    //getting the confirmation message from the server
                    System.out.println(in.readLine()); 
		  //if get command
                } else if (command.startsWith("get ")) {
                    String filename = command.split(" ")[1];
		    //receiving the file from the server
                    receiveFile(filename); 
		    //getting the confirmation message from teh server
                    System.out.println(in.readLine());
		  //if exit
                } else if (command.equals("exit")) {
                    System.out.println(in.readLine());
                    break;
                } else {
                    System.out.println(in.readLine()); // Invalid command message
                }
            }

        } catch (IOException e) {
            //catching any errors and printing a message
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    //method for send file with put command
    private static void sendFile(String filename) {
	//getting the file from client_files
        File file = new File(FILE_DIRECTORY + filename);
	//making sure the file exists 
        if (!file.exists()) {
            System.out.println("File not found in client directory.");
            return; // Exit if file doesn't exist
        }

        //trying to establish connection to file transfer socket
        try (Socket fileSocket = new Socket(SERVER_ADDRESS, PORT + 1);
	     //setting an output stream for sendign data over socket stream
             DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
	     //creating an input stream to read data from file
             FileInputStream fis = new FileInputStream(file)) {

	    //sendign the file name
            dos.writeUTF(filename);
	    //sending the file size 
	    dos.writeLong(file.length()); 

	    //creating a buffer for the file contents
            byte[] buffer = new byte[4096];
            int bytesRead;
	    
	    //reading the data from the file into the buffer then sending
            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }
	    //making sure all data is sent
            dos.flush(); 

        } catch (IOException e) {
            //Handling errors during file transfer
            System.err.println("Error sending file: " + e.getMessage());
        }
    }

    //method for receiving a file with get command
    private static void receiveFile(String filename) {
	//getting the file from the client files
        File file = new File(FILE_DIRECTORY + filename);

        //trying to get a connection from server
        try (Socket fileSocket = new Socket(SERVER_ADDRESS, PORT + 1); 
	     //creating an input stream to receive data from socket
             DataInputStream dis = new DataInputStream(fileSocket.getInputStream());
	     //creating an output stream to write the received files data
             FileOutputStream fos = new FileOutputStream(file)) {

	    //getting the files size
            long fileSize = dis.readLong(); 
	    //creating a buffer to hold the files data
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;

            //reading from file until all data received and write
            while (totalRead < fileSize && (bytesRead = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead; // Keep track of total bytes read
            }
	    //makinng sure all data was written to the file
            fos.flush();

        } catch (IOException e) {
            //handling errors during file receiving
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }
}

