import java.io.DataInputStream;
import java.io.IOException;

public class StreamUtils {
//    serverInput

    static byte[] readData(DataInputStream in, long length) throws IOException {
        int bytesRead;
        byte[] buffer = new byte[1024];
        byte[] fileBytes = new byte[0];
        while (length > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, length))) != -1) {
            fileBytes = combineBytes(fileBytes, buffer, bytesRead);
            length -= bytesRead;
        }
        return fileBytes;
    }

    /**
     * combines two byte arrays, all of the first one and a specific part of the second one
     *
     * @param first     the first array to combine
     * @param second    the second array to combine
     * @param bytesRead the part of the second array that should be read
     * @return the combined array
     */
    private static byte[] combineBytes(byte[] first, byte[] second, int bytesRead) {
        byte[] combined = new byte[first.length + bytesRead];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, bytesRead);
        return combined;
    }
}
