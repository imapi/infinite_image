package com.imapi.stream;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Input stream which generates images of any size.
 * Minimum valid image size depends on a platform, but generally is no less than 45 bytes.
 *
 * @author <a href="mailto:bondarenko.ivan.v@gmail.com">Ivan Bondarenko</a>
 */
public class EndlessInputStream extends InputStream {

    /**
     * Generated content size
     */
    private final int size;

    /**
     * Buffer size (default is 2Mb)
     */
    public static final int BUFFER_SIZE = 2 * 1024 * 1024;

    /**
     * Buffer for generated content
     */
    private static final byte[] BUFFER = generateBuffer();

    /**
     * Current reader position
     */
    private int position = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        byte[] temp = new byte[1];
        int rc = read(temp, 0, 1);
        return (rc == -1) ? -1 : (int) temp[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] destination, final int offset, final int length) throws IOException {

        Function read0 = new Function() {
            @Override
            public int apply(int off, int len) {
                int bufferPos = BUFFER_SIZE > position ? position : position % BUFFER_SIZE;
                int copyLen = Math.min(len, BUFFER_SIZE - bufferPos);

                System.arraycopy(BUFFER, bufferPos, destination, off, copyLen);
                len = len - copyLen;
                off += copyLen;
                position += copyLen;

                if (len <= 0) {
                    return length;
                }
                return apply(off, len);
            }
        };

        if (position < size) {
            return read0.apply(offset, length);
        }

        return -1;
    }

    /**
     * Constructor which takes size of the generated content
     *
     * @param size int size of the content
     */
    public EndlessInputStream(final int size) {
        this.size = size;
    }

    /**
     * Constructor with empty size parameter, size defaults to @link{BUFFER_SIZE}
     */
    public EndlessInputStream() {
        this.size = BUFFER_SIZE;
    }

    /**
     * This method creates the default size buffer, which is used to generate content
     */
    private static byte[] generateBuffer() {
        byte[] imageBuffer = generateImageBuffer();
        byte[] randomBuffer = new byte[BUFFER_SIZE - imageBuffer.length];
        new Random().nextBytes(randomBuffer);
        return merge(imageBuffer, randomBuffer);
    }

    private static byte[] merge(byte[] first, byte[] second) {
        byte[] combined = new byte[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    private static byte[] generateImageBuffer() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1, 1);
        byte[] result;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "gif", baos);
            baos.flush();
            result = baos.toByteArray();
        } catch (IOException e) {
            result = new byte[0];
        } finally {
            g.dispose();
        }
        return result;
    }

    /**
     * Helper interface for functional style
     */
    private static interface Function {
        int apply(final int off, final int len);
    }
}
