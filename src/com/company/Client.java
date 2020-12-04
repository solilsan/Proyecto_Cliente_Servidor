package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;
import java.util.Scanner;

public class Client {

    static boolean validarReglas = false;

    /**
     * @param args Main
     */
    public static void main(String[] args) {

        try {
            boolean error = false;
            Socket server = new Socket(InetAddress.getLocalHost(), 6000);

            do {

                try {

                    System.out.println("1-Registrarse.\n2-Salir.");
                    Scanner scanner = new Scanner(System.in);
                    error = false;

                    switch (Integer.parseInt(scanner.nextLine())) {
                        case 1 -> registro(server, scanner);
                        case 2 -> server.close();
                        default -> {
                            System.out.println("introduce un número del 1-2 por favor.");
                            error = true;
                        }
                    }

                    if (!error) {
                        reglasJuego(server);

                        if (validarReglas) {
                            empezarJuego(server);
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

    /**
     * Esta función nos permite realziar el registro, enviamos los datos al servidor y este lo valida
     */
    // Validación de datos del juegador contra el servidor (los datos se envian con cifrado simetrico)
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

    /**
     * Esta función nos permite validar las reglas del juego, el servidor nos envia las normas del juego cifradas con firma digital
     * y el jugador comprueba esas normas y si las acepta o no.
     */
    // Reglas del juego firmada por el servidor y comprobadas por el cliente (se comprueba mediante firma digital)
    public static void reglasJuego(Socket server) {

        try {

            ObjectInputStream get = new ObjectInputStream(server.getInputStream());

            Signature verificadsa = Signature.getInstance("SHA1withRSA");
            verificadsa.initVerify((PublicKey) get.readObject());
            byte[] reglasJuego = (byte[]) get.readObject();
            verificadsa.update(reglasJuego);

            boolean check = verificadsa.verify((byte[]) get.readObject());

            if (check) {
                System.out.println("\nReglas del juego del servidor validadas por el Jugador:");
                System.out.println(new String(reglasJuego));

                System.out.println("1-Ponga si para poder accptar las reglas y continuar con el juego, ponga cualquier otra cosa para salir.");
                System.out.print("Respuesta: ");
                Scanner teclado = new Scanner(System.in);
                String mensaje = teclado.nextLine();

                ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
                if (mensaje.equalsIgnoreCase("si")) {
                    // Si los datos son correctos empezamos el juego
                    set.writeObject(true);
                    validarReglas = true;
                }
                else {
                    set.writeObject(false);
                    validarReglas = false;
                }

            }
            else {
                validarReglas = false;
                System.out.println("Intento de falsificación de reglas de un servidor desconocido.");
                System.out.println("Se detiende la comunicación con el servidor por seguridad.");
            }

        } catch (Exception e) {
            System.out.println("[ReglasJuego Error] " + e.getMessage());
        }

    }

    /**
     * Esta función nos permite empezar el juego, en servidor nos envia la pregunta con cifrado asimetrico
     * El jugador responde a la pregunta y envia la respuesta al servidor con cifrado asimetrico
     */
    // Recibo las preguntas del servidor con cifrado asimetrico y envio la respuesta al servidor con cifrado asimetrico
    public static void empezarJuego(Socket server) {

        try {

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair par = keyGen.generateKeyPair();
            PrivateKey claveprivClient = par.getPrivate();
            PublicKey clavepubClient = par.getPublic();

            ObjectOutputStream set = new ObjectOutputStream(server.getOutputStream());
            set.writeObject(clavepubClient); // Envio mi clave publica al servidor

            Cipher desCipherClient = Cipher.getInstance("RSA");
            desCipherClient.init(Cipher.DECRYPT_MODE, claveprivClient);

            ObjectInputStream get = new ObjectInputStream(server.getInputStream());
            byte[] mensajeCifradoBytes = (byte[]) get.readObject();
            String mensajeDescifrado = new String(desCipherClient.doFinal(mensajeCifradoBytes));
            System.out.println(mensajeDescifrado);

            Cipher desCipherServer = Cipher.getInstance("RSA");
            desCipherServer.init(Cipher.ENCRYPT_MODE, (Key) get.readObject()); // El servidor me envia su clave publica

            // Respuesta
            Scanner teclado = new Scanner(System.in);
            String mensaje;

            System.out.print("Respuesta: ");
            mensaje = teclado.nextLine();
            byte[] mensajeCifra = desCipherServer.doFinal(mensaje.getBytes());
            // Enviar respuesta al servidor
            set.writeObject(mensajeCifra);

            boolean finJuego = false;
            do {

                mensajeCifradoBytes = (byte[]) get.readObject();
                mensajeDescifrado = new String(desCipherClient.doFinal(mensajeCifradoBytes));
                System.out.println(mensajeDescifrado);

                if (!mensajeDescifrado.substring(0, 15).equalsIgnoreCase("Juego terminado")) {
                    System.out.print("Respuesta: ");
                    mensaje = teclado.nextLine();
                    mensajeCifra = desCipherServer.doFinal(mensaje.getBytes());
                    set.writeObject(mensajeCifra);
                }
                else {
                    finJuego = true;
                }

            } while (!mensaje.equalsIgnoreCase("end") && !finJuego);

            if (mensaje.equalsIgnoreCase("end")) {
                mensajeCifradoBytes = (byte[]) get.readObject();
                mensajeDescifrado = new String(desCipherClient.doFinal(mensajeCifradoBytes));
                System.out.println(mensajeDescifrado);
            }

        } catch (Exception e) {
            System.out.println("[EmpezarJuego Error] " + e.getMessage());
        }

    }

}
