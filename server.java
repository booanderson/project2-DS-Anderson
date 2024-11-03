/*usage
 * 1. compile both server and client programs with: 
 * 	javac server.java client.java
 *
 * 2. Make sure there are two directores named:
 * 	client_files
 * 	server_files
 *
 * 3. put a file in the the cliennt directory
 *
 * 4. run the server with java Server
 *
 * 5. run the client with java Client
 *
 * 6. put in the password 
 *
 * 7. run put "filename" from clients console 
 *
 * 8. run get "filename" from clients console
 */



import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;

class Server {
    //port for the client connections
    private static final int PORT = 5000;
    //designating directory for upload
    private static final String FILE_DIRECTORY = "server_files/";
    //setting length to the OTP
    private static final int OTP_LENGTH = 6;
    //creating OTP string for use
    private static String otp;

    public static void main(String[] args) {
        //calling OTP generator function
	otp = generateOTP();
        System.out.println("One time password: " + otp);

	//creating the server sockets 
	//1 for client connections, 1 for the file transfers
        try (ServerSocket serverSocket = new ServerSocket(PORT);
             ServerSocket fileSocket = new ServerSocket(PORT + 1)) { 

            System.out.println("Server running. Waiting for client...");
            //looop to keep client socket open until terminated
	    while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected.");
                    handleClient(clientSocket, fileSocket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //handling communication with the clients
    private static void handleClient(Socket clientSocket, ServerSocket fileSocket) throws IOException {
        //used to read the input from the client socket 
	BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //used to send the text to the output stream of the client socket
	PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        out.println("Password:");
        String enteredOtp = in.readLine();
	//checking if client input password = the OTP
        if (!otp.equals(enteredOtp)) {
            out.println("Authentication Failed");
            return;
        }
        out.println("Authentication Successful");
        out.println("Hello");

	//taking the client commands through client socket buffer
        String clientCommand;
        while ((clientCommand = in.readLine()) != null) {
            String[] commandParts = clientCommand.split(" ");
	    //if the get command used. the file is then downloaded to the client from the server
            if (commandParts[0].equals("get") && commandParts.length == 2) {
                sendFile(commandParts[1], fileSocket, out);
	      //if the put command is used to upload a file to the server
            } else if (commandParts[0].equals("put") && commandParts.length == 2) {
                receiveFile(commandParts[1], fileSocket, out);
            } else if (clientCommand.equals("exit")) {
                break;
            } else {
                out.println("Invalid command.");
            }
        }
    }

    //method used to send file when get command is called
    private static void sendFile(String filename, ServerSocket fileSocket, PrintWriter out) {
	//creating file object to send
        File file = new File(FILE_DIRECTORY + filename);
        //making sure file exists
	if (!file.exists()) {
            out.println("File not found");
            return;
        }

	//getting the file transfer stocket
        try (Socket transferSocket = fileSocket.accept();
             //creating the output stream to send data
	     DataOutputStream dos = new DataOutputStream(transferSocket.getOutputStream());
	     //creating the input streap to read data 
             FileInputStream fis = new FileInputStream(file)) {

            out.println("Sending file");
	    //sending file size
            dos.writeLong(file.length());

	    //creating a buffer for the file data
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int bytesRead;
	    //reading the entire file into the buffer
            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
                byteStream.write(buffer, 0, bytesRead);
            }
	    //flushing output stream for future errors
            dos.flush();

            //getting crc of the send checksum
            int crcValue = CRC16.calculateCRC(byteStream.toByteArray());
	    //sending the crc to the client
            dos.writeShort(crcValue);
            out.println("File sent successfully");

        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
    }

    //method for receiving a file when the put command is called
    private static void receiveFile(String filename, ServerSocket fileSocket, PrintWriter out) {
	//creating the file to be put
        File file = new File(FILE_DIRECTORY + filename);

	//binding the receive to the file transfer socket
        try (Socket transferSocket = fileSocket.accept();
	     //creating the input stream for receiving data
             DataInputStream dis = new DataInputStream(transferSocket.getInputStream());
	     //input stream for writing the data
             FileOutputStream fos = new FileOutputStream(file)) {

            out.println("Receiving file");
	    //reading the length of the incoming file
            long fileSize = dis.readLong();

	    //creating the buffer to hold the data from file
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;

	    //loop to make sure all contents of the file are read
            while (totalRead < fileSize && (bytesRead = dis.read(buffer)) > 0) {
		//writing the data to the new file
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
	    //flushing the output stream for future errors
            fos.flush();

            //receiving the crc from server
            short receivedCrc = dis.readShort(); 
	    //creating new int to store client crc checksum
            int calculatedCrc = CRC16.calculateCRC(buffer);
	    //checking if the checksums are equal
            if (receivedCrc != calculatedCrc) {
                out.println("CRC Error");
            } else {
                out.println("File received successfully (crc verified)");
            }

        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }

    //method to generate the OTP
    private static String generateOTP() {
	//creating a new random generator object
        SecureRandom random = new SecureRandom();
	//creating the string builder to hold the OTP with length
        StringBuilder otpBuilder = new StringBuilder(OTP_LENGTH);
        //making sure the OTP is the proper length
	for (int i = 0; i < OTP_LENGTH; i++) {
	    //appending a random digit while length not met
            otpBuilder.append(random.nextInt(10));
        }
        return otpBuilder.toString();
    }
}

//the crc class to calculate the checksums
class CRC16 {
    //crc polynomial
    private static final int POLYNOMIAL = 0xA001;

    //method to calculate crc for the byte array from file
    public static int calculateCRC(byte[] data) {
	//setting the initial crc value
        int crc = 0xFFFF;
	//going through all bytes of the data
        for (byte b : data) {
	    //XOR with the byte
            crc ^= b;
	    //processing each bit
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ POLYNOMIAL;
                } else {
                    crc >>= 1;
                }
            }
        }
	//ensuring the crc is 16 bits
        return crc & 0xFFFF;
    }
}

