package it.inaf.iasfpa.astri.camera.interfaces;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * @author David Melkumyan, DESY Zeuthen
 *	modificato per invio dati anche in formato array di byte
 *  e per exit condition con size prefissato o token oltre al timeout
 *	P. Sangiorgi, IASFPA, sangiorgi@ifc.inaf.it
 */
public class SerialPortHandler implements SerialPortEventListener, Closeable {
	
	private static final Logger logger = Logger.getLogger(SerialPortHandler.class);
	
	public static final int TRANSMIT_MODE_STRING = 0;
	public static final int TRANSMIT_MODE_BYTE = 1;
	
	private String serialDevice;
	private SerialPort serialPort;
	private AsyncReceiver asyncReceiver;
	private AsyncTransmitter asyncTransmitter;
	private ReentrantLock serialPortAccessLock = new ReentrantLock(true);
	
	private int tMode;
	
	public SerialPortHandler(String owner, String device, int baudRate, int databits, int stopbits, int parity, int flowControl, int transmitMode) throws IOException, UnsupportedEncodingException {
		serialDevice = device;
		serialPort = openSerialDevice(owner, device, baudRate, databits, stopbits, parity, flowControl);
		tMode = transmitMode;
		try {
			serialPort.addEventListener(this);

		} catch (TooManyListenersException ex) {
			try {
				close();
			} catch (Throwable ignored) {
			}
			new IOException("Too many serial port listeners: " + serialDevice, ex);
		}
		asyncTransmitter = new AsyncTransmitter(transmitMode);
		asyncReceiver = new AsyncReceiver();
		serialPort.notifyOnDataAvailable(true);
	}

	private SerialPort openSerialDevice(String owner, String portname, int baudRate, int dataBits, int stopBits, int parity, int flowControl)
			throws IOException {

		if (portname == null)
			throw new NullPointerException("portname == null");

		CommPortIdentifier cid;
		try {
			cid = CommPortIdentifier.getPortIdentifier(portname);

		} catch (NoSuchPortException e) {
			throw new IOException("Invalid serial device name: " + portname, e);
		}

		if (cid.getPortType() != CommPortIdentifier.PORT_SERIAL)
			throw new IOException("Not a valid serial device: " + portname);

		if (cid.isCurrentlyOwned() == true)
			throw new IOException("Serial device is already reserved: " + cid.getCurrentOwner());

		SerialPort port = null;
		try {
			port = (SerialPort) (cid.open(owner, 100));
			port.setSerialPortParams(baudRate, dataBits, stopBits, parity);
			port.setFlowControlMode(flowControl);
			return (port);

		} catch (PortInUseException e) {
			throw new IOException("Serial device is already in use: " + portname, e);

		} catch (UnsupportedCommOperationException e) {
			if (port != null) {
				try {
					port.close();
				} catch (Throwable tx) {
				}
			}
			throw new IOException("Serial device does not support requested parameters: " + portname, e);

		} catch (Throwable t) {
			if (port != null) {
				try {
					port.close();
				} catch (Throwable tx) {
				}
			}
			throw new IOException("Unknown error while opening/initializing serial device: " + portname, t);
		}
	}

	public byte[] receive(long timeout) throws IOException {
		serialPortAccessLock.lock();
		try {
			try {
				return (asyncReceiver.receive(timeout));

			} catch (InterruptedException e) {
				throw new IOException("Receive interrupted", e);
			}

		} finally {
			serialPortAccessLock.unlock();
		}
	}

	@Override
	public void serialEvent(SerialPortEvent spe) {
		if ((spe != null) && (spe.getEventType() == SerialPortEvent.DATA_AVAILABLE)) {
			logger.trace("DATA_AVAILABLE event...");
			if (asyncReceiver != null)
				asyncReceiver.notifyReceivedData();
		}
	}
	
//	public void transmit(Object o) throws IOException {
//		if (o == null)
//			throw new NullPointerException("o == null");
//
//		serialPortAccessLock.lock();
//		try {
//			try {
//				if (tMode == TRANSMIT_MODE_STRING)
//					asyncTransmitter.transmit((String) o);
//				else if (tMode == TRANSMIT_MODE_BYTE)
//					asyncTransmitter.transmit((byte[]) o);
//			} catch (InterruptedException e) {
//				throw new IOException("Transmit interrupted", e);
//			}
//
//		} finally {
//			serialPortAccessLock.unlock();
//		}
//	}
	
	public void transmit(Object o) {
		if (o == null)
			throw new NullPointerException("o == null");
		serialPortAccessLock.lock();
		try {
			if (tMode == TRANSMIT_MODE_STRING) {
				PrintWriter outString = new PrintWriter(new OutputStreamWriter(serialPort.getOutputStream()));
				if (outString != null) {
					outString.println((String) o);
	//				outString.flush();
					outString.close();
				}
			} else if (tMode == TRANSMIT_MODE_BYTE) {
				 OutputStream outByte = serialPort.getOutputStream();
				if (outByte != null) {
					outByte.write((byte[]) o);
	//				outByte.flush();
					outByte.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			serialPortAccessLock.unlock();
		}
	}
	
	public byte[] transmitAndReceive(Object o, long timeout, int fixedSize, byte endChar) throws IOException {
		serialPortAccessLock.lock();
		try {
			try {
				if (tMode == TRANSMIT_MODE_STRING)
					asyncTransmitter.transmit((String) o);
				else if (tMode == TRANSMIT_MODE_BYTE)
					asyncTransmitter.transmit((byte[]) o);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					byte[] chunk;					
					while ((chunk = asyncReceiver.receive(timeout)) != null) {
						baos.write(chunk);
						if (fixedSize != -1) {
							if (baos.toByteArray().length == fixedSize)
								break;
						}
						if (endChar != 0) {
							if (baos.toByteArray()[baos.toByteArray().length-1] == endChar) {
								break;
							}
						}
					}
					return baos.toByteArray();

				} finally {
					baos.close();
				}

			} catch (InterruptedException e) {
				throw new IOException("Transmit and receive interrupted", e);
			}

		} finally {
			serialPortAccessLock.unlock();
		}
	}

	public String getSerialDevice() {
		return serialDevice;
	}

	@Override
	public void close() {
		if (asyncReceiver != null) {
			asyncReceiver.close();
			asyncReceiver = null;
		}

		if (asyncTransmitter != null) {
			asyncTransmitter.close();
			asyncTransmitter = null;
		}

		if (serialPort != null) {
			serialPort.removeEventListener();
			try {
				serialPort.close();
			} catch (Throwable ignored) {
			}
			serialPort = null;
		}
	}

	private class AsyncReceiver extends Thread implements Closeable {

		private InputStream in;
		private BlockingQueue<byte[]> receiveQueue;

		private AsyncReceiver() throws IOException {
			in = serialPort.getInputStream();
			receiveQueue = new LinkedBlockingQueue<byte[]>();
			start();
		}

		public void clear() {
			receiveQueue.clear();
		}

		@Override
		public void close() {
			interrupt();
			try {
				join();
			} catch (InterruptedException e) {
			}

			clear();
			if (in != null) {
				try {
					in.close();
				} catch (Throwable t) {
				}
				in = null;
			}
		}

		synchronized public void notifyReceivedData() {
			notify();
		}

		public byte[] receive(long timeout) throws InterruptedException {
			if (logger.isTraceEnabled())
				logger.trace("Receiving data...");
			return (receiveQueue.poll(timeout, TimeUnit.MILLISECONDS));
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					waitReceivedData();
					try {
						int available = in.available();
						byte[] chunk = new byte[available];
						in.read(chunk, 0, available);
						logger.trace("Received " + available + " new bytes and put in queue: " + Arrays.toString(chunk));
						receiveQueue.put(chunk);
					} catch (IOException e) {
						logger.error("Receive error: " + e.getMessage());
					}

				} catch (InterruptedException interrupted) {
					return;
				}
			}
		}

		synchronized private void waitReceivedData() throws InterruptedException {
			wait();
		}

	}

	private class AsyncTransmitter extends Thread implements Closeable {

		private OutputStream outByte;
		private PrintWriter outString;
		private BlockingQueue transmitQueue;
		
		private int tMode;
		
		private AsyncTransmitter(int transmitMode) throws IOException {
			tMode = transmitMode;
			if (tMode == TRANSMIT_MODE_STRING) {
				transmitQueue = new LinkedBlockingQueue<String>();
				outString = new PrintWriter(new OutputStreamWriter(serialPort.getOutputStream()));
			} else if (tMode == TRANSMIT_MODE_BYTE) {
				transmitQueue = new LinkedBlockingQueue<byte[]>();
				outByte = serialPort.getOutputStream();
			}
			start();
		}

		public void clear() {
			transmitQueue.clear();
		}

		@Override
		public void close() {
			interrupt();
			try {
				join();

			} catch (InterruptedException e) {
			}

			clear();
			if (outString != null) {
				try {
					outString.close();
				} catch (Throwable t) {
				}
				outString = null;
			}
			if (outByte != null) {
				try {
					outByte.close();
				} catch (Throwable t) {
				}
				outByte = null;
			}
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					if (tMode == TRANSMIT_MODE_STRING) {
						String str = (String) transmitQueue.take();
						if (outString != null) {
							outString.println(str);
							outString.flush();
						}
					} else if (tMode == TRANSMIT_MODE_BYTE) {
						byte[] bytes = (byte[]) transmitQueue.take();
						if (outByte != null) {
							outByte.write(bytes);
							outByte.flush();
						}
					}
				} catch (InterruptedException e) {
					return;
				} catch (IOException e) {
					return;
				}
			}
		}

		public void transmit(String str) throws InterruptedException {
			if (str == null)
				throw new NullPointerException("Null str");

			if (logger.isTraceEnabled())
				logger.trace("Transmitting string: <" + str + "> ...");

			transmitQueue.offer(str.trim());
		}
		
		public void transmit(byte[] bytes) throws InterruptedException {
			if (bytes == null)
				throw new NullPointerException("Null byte array");

			if (logger.isTraceEnabled())
				logger.trace("Transmitting byte array: <" + Arrays.toString(bytes) + "> ...");

			transmitQueue.offer(bytes);
		}
		
	}

}
