// stolen from ASM's ClassReader

package design.sxxov.imagination.resources.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamToByteArrayUtility {
	/** The maximum size of array to allocate. */
	private static final int MAX_BUFFER_SIZE = 1024 * 1024;

	/** The size of the temporary byte array used to read class input streams chunk by chunk. */
	private static final int INPUT_STREAM_DATA_CHUNK_SIZE = 4096;
	
	/**
	 * Reads the given input stream and returns its content as a byte array.
	 *
	 * @param inputStream an input stream.
	 * @param close true to close the input stream after reading.
	 * @return the content of the given input stream.
	 * @throws IOException if a problem occurs during reading.
	 */
	public static byte[] readStream(
		final InputStream inputStream, 
		final boolean close
	) throws IOException {
		if (inputStream == null) {
			throw new IOException("Class not found");
		}
		int bufferSize = calculateBufferSize(inputStream);
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			byte[] data = new byte[bufferSize];
			int bytesRead;
			int readCount = 0;
			while ((bytesRead = inputStream.read(data, 0, bufferSize)) != -1) {
				outputStream.write(data, 0, bytesRead);
				readCount++;
			}
			outputStream.flush();
			if (readCount == 1) {
				return data;
			}
			return outputStream.toByteArray();
		} finally {
			if (close) {
				inputStream.close();
			}
		}
	}

	public static int calculateBufferSize(final InputStream inputStream) throws IOException {
		int expectedLength = inputStream.available();
		/*
		 * Some implementations can return 0 while holding available data
		 * (e.g. new FileInputStream("/proc/a_file"))
		 * Also in some pathological cases a very small number might be returned,
		 * and in this case we use default size
		 */
		if (expectedLength < 256) {
		  return INPUT_STREAM_DATA_CHUNK_SIZE;
		}
		return Math.min(expectedLength, MAX_BUFFER_SIZE);
	}
}
