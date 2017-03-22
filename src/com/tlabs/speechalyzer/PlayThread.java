package com.tlabs.speechalyzer;

import java.io.*;

/**
 * streams a specified _file to the client.
 * 
 * @version 1.0
 * @author Felix Burkhardt
 */
public class PlayThread extends Thread {
    /**
     * input stream.
     */
    DataInputStream in;

    /**
     * output stream.
     */
    DataOutputStream out;

    /**
     * chunk of data.
     */
    byte[] data;

    /**
     * number of bytes read from _file.
     */
    int ret;

    /**
     * true if _file is finished.
     */
    boolean fileStopped = false;

    /**
     * true if client closed socket.
     */
    boolean clientStopped = false;

    int offset = 0;

    /**
     * get parameters and init bytearrry.
     * 
     * @param out
     *            output stream
     * @param in
     *            input stream
     */
    public PlayThread(DataOutputStream out, DataInputStream in, int offset) {
        this.in = in;
        this.out = out;
        this.offset = offset;
        // _size of byte array should correspond with client.
        data = new byte[2048];
    }

    /**
     * called by thread.start().
     */
    public void run() {
        // debugging var
        int i = 0;
        // finished by break or exception
        try {
            in.skip(offset);
        } catch (IOException io) {
            System.out.println("catched IOException");
            io.printStackTrace();
        }
        while (true) {
            // read data from _file
            try {
                ret = in.read(data, 0, data.length);
                // System.out.println("sending bytes: " + ret + ", packet Num: " + i++);
            } catch (IOException io) {
                System.out.println("catched IOException");
                io.printStackTrace();
            }
            // if bytes = -1 _file is finished
            if (ret == -1) {
                fileStopped = true;
            }
            // write data to client
            try {
                out.write(data);
                if (fileStopped)
                    break;
            } catch (IOException e) {
                // probably client stopped playback
                clientStopped = true;
                break;
            }
        }
        if (fileStopped) {
            System.out.println("finished sending _file");
        }
        if (clientStopped) {
            System.out.println("Socket closed by client");
        }
        // close streams
        System.out.println("closing streams");
        try {
            in.close();
            out.close();
        } catch (Exception e) {
            System.err.println("problem closing streams: " + e);
        }
    }
}