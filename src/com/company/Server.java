package com.company;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {

        try {
            ServerSocket server = new ServerSocket(6000);
            System.out.println("Server ON");

            while (true) {
                Socket client = server.accept();
                HiloServer hs = new HiloServer(client);
                hs.start();
            }

        } catch (Exception IOe) {
            System.out.println("[Server Error] " + IOe.getMessage());
        }

    }

}
