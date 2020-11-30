package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        try {
            boolean error = false;
            Socket server = new Socket(InetAddress.getLocalHost(), 6000);

            do {

                try {

                    System.out.println("1-Registrarse.\n2-Salir.");
                    Scanner scanner = new Scanner(System.in);

                    switch (Integer.parseInt(scanner.nextLine())) {
                        case 1 -> registro(server, scanner);
                        case 2 -> server.close();
                        default -> {
                            System.out.println("introduce un número del 1-2 por favor.");
                            error = true;
                        }
                    }

                    reglasJuego(server);

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
                byte[] nombreCifrado = desCipher.doFinal(nombre.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(nombreCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Nombre no válido, primera letra mayúsculas mínimo 3 caracteres y máximo 25.");
                }

            } while (!correct);

            do {
                System.out.print("Apellido: ");
                String apellido = scanner.nextLine();
                byte[] apellidoCifrado = desCipher.doFinal(apellido.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(apellidoCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Apellido no válido, primera letra mayúsculas mínimo 3 caracteres y máximo 25.");
                }

            } while (!correct);

            do {
                System.out.print("Edad: ");
                String edad = scanner.nextLine();
                byte[] edadCifrado = desCipher.doFinal(edad.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(edadCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("Tienes que ser mayor de edad.");
                }

            } while (!correct);

            do {
                System.out.print("Contraseña: ");
                String password = scanner.nextLine();
                byte[] passwordCifrado = desCipher.doFinal(password.getBytes());

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                set.writeObject(passwordCifrado);

                correct = (boolean) get.readObject();

                if (!correct) {
                    System.out.println("La contraseña debe tener al entre 8 y 16 caracteres, al menos un dígito, al menos una minúscula y al menos una mayúscula.\n" +
                            "NO puede tener otros símbolos.");
                }

            } while (!correct);

        } catch (Exception e) {
            System.out.println("[Registro Error] " + e.getMessage());
        }

    }

    public static void reglasJuego(Socket server) {

        try {

            ObjectInputStream get = new ObjectInputStream(server.getInputStream());

            Signature verificadsa = Signature.getInstance("SHA1withRSA");
            verificadsa.initVerify((PublicKey) get.readObject());
            byte[] reglasJuego = (byte[]) get.readObject();
            verificadsa.update(reglasJuego);

            boolean check = verificadsa.verify((byte[]) get.readObject());

            if (check) {
                System.out.println("\nReglas del juego validadas por el Jugador:");
                System.out.println(new String(reglasJuego));
            }
            else {
                System.out.println("Intento de falsificación de reglas de un servidor desconocido");
            }

            get.close();

        } catch (Exception e) {
            System.out.println("[ReglasJuego Error] " + e.getMessage());
        }

    }

}
