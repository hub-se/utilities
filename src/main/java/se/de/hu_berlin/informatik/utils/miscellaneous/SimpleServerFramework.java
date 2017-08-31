package se.de.hu_berlin.informatik.utils.miscellaneous;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class SimpleServerFramework {
	
	public static int getFreePort() {
		return getFreePort(new Random().nextInt(60536) + 5000);
	}
	
	public static int getFreePort(final int startPort) {
		InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getByName(null);
		} catch (UnknownHostException e1) {
			// should not happen
			return -1;
		}
		// port between 0 and 65535 !
		Random random = new Random();
		int currentPort = startPort;
		int count = 0;
		while (true) {
			if (count > 1000) {
				return -1;
			}
			++count;
			try {
				new Socket(inetAddress, currentPort).close();
			} catch (final IOException e) {
				// found a free port
				break;
			} catch (IllegalArgumentException e) {
				// should only happen on first try (if argument wrong)
			}
			currentPort = random.nextInt(60536) + 5000;
		}
		return currentPort;
	}
	
	public static ServerSocket startServer() {
		return _startServer(getFreePort());
	}
	
	public static ServerSocket startServer(int port) {
		return _startServer(getFreePort(port));
	}
	
	private static ServerSocket _startServer(int port) {
	    try {
	        ServerSocket socket = new ServerSocket(port);
	        // set a timeout to stop blocking in case of errors occurring...
	        socket.setSoTimeout(10000);
	        return socket;
	    } catch (Exception e) {
	        System.err.println("Server Error: " + e.getMessage());
	        System.err.println("Localized: " + e.getLocalizedMessage());
	        System.err.println("Stack Trace: " + e.getStackTrace());
	        System.err.println("To String: " + e.toString());
	    }
	    
	    return null;
	}
	
	public static <T extends Serializable, R extends Serializable> ServerSideListener<T,R> startServerListener(
			ServerSocket socket, Object receiveLock, Function<T,R> sendOnReceivedData) {
		ServerSideListener<T, R> serverSideListener = new ServerSideListener<>(socket, receiveLock, sendOnReceivedData);
		serverSideListener.run();
		return serverSideListener;
	}

	public static class ServerSideListener<T extends Serializable, R extends Serializable> {

		final private ServerSocket serverSocket;
		private final Object receiveLock;
		private final Function<T,R> sendOnReceivedData;
		
		private Thread runningThread = null;
		private T data;
		private boolean isShutdown = false;
		private boolean hasNewData = false;
		private boolean serverErrorOccurred = false;
		
		public ServerSideListener(ServerSocket serverSocket, Object receiveLock, Function<T,R> sendOnReceivedData) {
			this.serverSocket = serverSocket;
			this.receiveLock = receiveLock;
			this.sendOnReceivedData = sendOnReceivedData;
		}
		
		public void run() {
			if (this.runningThread == null || !this.runningThread.isAlive()) {
				this.runningThread = new Thread(() -> {
					listenOnSocket(serverSocket);
				});
				this.runningThread.start();
			}
		}
		
		@SuppressWarnings("unchecked")
		private void listenOnSocket(ServerSocket serverSocket) {
			while (!isShutdown) {
				boolean triedReceivingMessage = false;
				boolean triedSendingMessageBack = false;
				boolean sentMessageBack = false;
				Socket clientSocket = null;
				try {
					// Create the Client Socket
					clientSocket = serverSocket.accept();
//					Log.out(this, "Server Socket Extablished...");
					// Create input and output streams to client
					ObjectOutputStream outToClient = new ObjectOutputStream(clientSocket.getOutputStream());
					ObjectInputStream inFromClient = new ObjectInputStream(clientSocket.getInputStream());

					triedReceivingMessage = true;
					/* Retrieve information */
					this.data = (T)inFromClient.readObject();

					// tell any waiting threads that there is new data...
					synchronized (receiveLock) {
						hasNewData = true;
						receiveLock.notifyAll();
					}

					triedSendingMessageBack = true;
					/* Send a message object back */
					outToClient.writeObject(sendOnReceivedData.apply(this.data));
					sentMessageBack = true;
				} catch (SocketTimeoutException e) {
					// simply try again until shutdown
					// tell any waiting threads to move on or check their abort conditions again...
					synchronized (receiveLock) {
						receiveLock.notifyAll();
					}
				} catch (Exception e) {
					if (clientSocket != null) {
						try {
							clientSocket.close();
						} catch (IOException e1) {
							// don't care
						}
					}
					// tell any waiting threads that there is an error...
					synchronized (receiveLock) {
						serverErrorOccurred = true;
						receiveLock.notifyAll();
					}
					Log.err("server", e, "error: " + e.getMessage());
				}
			}
		}
		
		public void shutDown() {
			if (this.runningThread != null && this.runningThread.isAlive()) {
				isShutdown = true;
				boolean received = false;
				int count = 0;
				while (!received && count < 5) {
					++count;
					received = sendToServer(null, serverSocket.getLocalPort(), 1,
							(r) -> {return false;},
							(t,r) -> {return true;});
				}
				if (received) {
					while (this.runningThread.isAlive()) {
						try {
							this.runningThread.join();
						} catch (InterruptedException e) {
							// wait until finished
						}
					}
				} else {
					// just don't wait?...
				}
				try {
					serverSocket.close();
				} catch (IOException e) {
					// don't care
				}
			}
		}
		
		public boolean serverErrorOccurred() {
			return serverErrorOccurred;
		}
		
		public boolean hasNewData() {
			return hasNewData;
		}

		public T getLastData() {
			hasNewData = false;
			return data;
		}

		public void resetListener() {
			serverErrorOccurred = false;
			hasNewData = false;
		}
		
	}
	
	public static <T extends Serializable, R extends Serializable> boolean sendToServer(
			T data, int port, int maxTryCount, Predicate<R> sendAgain, BiPredicate<T,R> isSuccessful) {
		boolean succeeded = false;
		int count = 0;
		int connectionTryCount = 0;
		while (!succeeded && connectionTryCount < 5) {
			++connectionTryCount;
			try {
				// Create the socket
				Socket clientSocket = new Socket((String)null, port);
//	        	Log.out("client", "Client Socket initialized...");
				// Create the input & output streams to the server
				ObjectOutputStream outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
				ObjectInputStream inFromServer = new ObjectInputStream(clientSocket.getInputStream());

				while (!succeeded && count < maxTryCount) {
					++count;
//	        		Log.out("client", "writing data to port %d...", port);
					/* Send the Message Object to the server */
					outToServer.writeObject(data);            

					/* Retrieve the Message Object from server */
					@SuppressWarnings("unchecked")
					R inFromServerMsg = (R)inFromServer.readObject();

					/* Print out the received Message */
//	        		Log.out("client", "Message from server: " + inFromServerMsg);
					// check if the server wants us to check the data again
					// (could be the case that there was an exception while reading the input stream)
					if (sendAgain.test(inFromServerMsg)) {
						continue;
					}
					succeeded = isSuccessful.test(data, inFromServerMsg);
				}

				clientSocket.close();

			} catch (Exception e) {
				Log.err("client", e, "error: " + e.getMessage());
			}
		}
	    
	    return succeeded;
	}
}
