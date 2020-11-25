package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        try {
            boolean error = false;
            Socket server = new Socket(InetAddress.getLocalHost(), 6000);

            do {

                try {

                    System.out.println("1-Iniciar sessión.\n2-Registrarse.\n3-Salir.");
                    Scanner scanner = new Scanner(System.in);

                    switch (Integer.parseInt(scanner.nextLine())) {
                        case 1 -> {
                            System.out.println("Iniciar sessión.");
                            System.out.println("Nink: ");
                            String dni = scanner.nextLine();
                            System.out.println("Contraseña: ");
                            String password = scanner.next();
                        }
                        case 2 -> registro(server, scanner);
                        case 3 -> server.close();
                        default -> {
                            System.out.println("introduce un número del 1-3 por favor.");
                            error = true;
                        }
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Introduce un número por favor.");
                    error = true;
                }

            } while (error);


        } catch (ConnectException ce) {
            System.out.println("[Client Error] Fallo al conectar con el servidor, " + ce.getMessage());
        } catch (Exception e) {
            System.out.println("[Client Error] " + e.getMessage());
        }

    }

    public static void registro(Socket server ,Scanner scanner) {

        try {
            boolean correct;
            ObjectInputStream get = new ObjectInputStream(server.getInputStream());
            SecretKey key = (SecretKey) get.readObject();
            Cipher desCipher = Cipher.getInstance("DES");
            desCipher.init(Cipher.ENCRYPT_MODE, key);

            System.out.println("Registrarse.");

            do {
                System.out.print("Dni: ");
                String dni = scanner.nextLine();
                byte[] dniCifrado = desCipher.doFinal(dni.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(dniCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Dni no válido, introduce un Dni correcto por favor.");
                }

            } while (!correct);

            do {
                System.out.print("Nombre: ");
                String nombre = scanner.nextLine();
                byte[] dniCifrado = desCipher.doFinal(nombre.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(dniCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Nombre no válido, primera letra mayúsculas mínimo 3 caracteres y máximo 25.");
                }

            } while (!correct);

            do {
                System.out.print("Apellido: ");
                String nombre = scanner.nextLine();
                byte[] dniCifrado = desCipher.doFinal(nombre.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(dniCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Apellido no válido, primera letra mayúsculas mínimo 3 caracteres y máximo 25.");
                }

            } while (!correct);

            do {
                System.out.print("Edad: ");
                String edad = scanner.nextLine();
                byte[] dniCifrado = desCipher.doFinal(edad.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(dniCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Tienes que ser mayor de edad.");
                }

            } while (!correct);

            get.close();

        } catch (Exception e) {
            System.out.println("[Registro Error] " + e.getMessage());
        }

    }

}
