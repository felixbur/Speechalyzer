package com.tlabs.speechalyzer;
import java.io.*;

/**
 * read a stream from client and write it to a _file.
 * @version 1.0
 * @author Felix Burkhardt
 */
class RecordThread extends Thread {
    /**
     * output stream to _file.
     */
    FileOutputStream out;
    /**
     * input stream from client.
     */
    DataInputStream in;
    /**
     * chunk of data.
     */
    byte[] data;

    /**
     * get parameters and init byte-array.
     * @param in stream from client.
     * @param out output stream to _file.
     */
    public RecordThread (DataInputStream in, FileOutputStream out)
    {
        this.in = in;
        this.out = out;
        data = new byte[1024];
    }

    /**
     * called by thread.start().
     */
    public void run ()
    {
        while (true) {
            try {
//                in.readFully(data);
                int count = in.read(data);
                out.write(data,0,count);
            } catch (Exception e) {
                // probably client stopped recording (closed the socket).
                System.out.println("Socket closed");
                try {
                    in.close();
                    out.flush();
                    out.close();
                } catch (Exception ex) {
                    System.err.println("promblem closing streams: " + e);
                }
                break;
            }
        }
    }
}