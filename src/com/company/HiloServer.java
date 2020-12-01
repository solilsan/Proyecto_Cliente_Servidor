package com.company;

import javax.crypto.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HiloServer extends Thread{

    private final Socket client;
    private int puntos;
    private String dniJugador;

    public HiloServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {

        try {

            validarDatosJugador();
            validarReglasdelJuego();
            empezarJuego();

            client.close();

        } catch (Exception e) {
            System.out.println("[Server Hilo Error] " + e.getMessage());
        }

    }

    // Validación de datos del juegador contra el servidor (los datos se envian con cifrado simetrico)
    public void validarDatosJugador() throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException {

        ObjectOutputStream setDatos = new ObjectOutputStream(client.getOutputStream());
        KeyGenerator keygen = KeyGenerator.getInstance("DES");
        SecretKey key = keygen.generateKey();
        setDatos.writeObject(key);

        Cipher desCipher = Cipher.getInstance("DES");
        desCipher.init(Cipher.DECRYPT_MODE, key);

        boolean dniValido;
        do {
            System.out.println("Validando Dni del jugador...");
            ObjectInputStream get = new ObjectInputStream(client.getInputStream());
            String dniJugador = new String(desCipher.doFinal((byte[]) get.readObject()));
            Pattern patDni = Pattern.compile("[0-9]{7,8}[A-Z a-z]");
            Matcher mat = patDni.matcher(dniJugador);

            if (!mat.matches()) {
                setDatos.writeObject(false);
                dniValido = false;
            } else {
                this.dniJugador = dniJugador;
                setDatos.writeObject(true);
                dniValido = true;
            }
        } while (!dniValido);

        Pattern patNombreApellido = Pattern.compile("^[A-Z]{1}[a-z]{2,25}$");

        boolean nombreValido;
        do {
            System.out.println("Validando nombre del jugador...");
            ObjectInputStream get = new ObjectInputStream(client.getInputStream());
            String nombreJugador = new String(desCipher.doFinal((byte[]) get.readObject()));
            Matcher mat = patNombreApellido.matcher(nombreJugador);

            if (!mat.matches()) {
                setDatos.writeObject(false);
                nombreValido = false;
            } else {
                setDatos.writeObject(true);
                nombreValido = true;
            }
        } while (!nombreValido);

        boolean apellidoValido;
        do {
            System.out.println("Validando apellido del jugador...");
            ObjectInputStream get = new ObjectInputStream(client.getInputStream());
            String apellidoJugador = new String(desCipher.doFinal((byte[]) get.readObject()));
            Matcher mat = patNombreApellido.matcher(apellidoJugador);

            if (!mat.matches()) {
                setDatos.writeObject(false);
                apellidoValido = false;
            } else {
                setDatos.writeObject(true);
                apellidoValido = true;
            }
        } while (!apellidoValido);

        boolean edadValido;
        do {
            System.out.println("Validando edad del jugador...");
            ObjectInputStream get = new ObjectInputStream(client.getInputStream());
            String edadJugador = new String(desCipher.doFinal((byte[]) get.readObject()));

            try {
                if (Integer.parseInt(edadJugador) < 18) {
                    setDatos.writeObject(false);
                    edadValido = false;
                } else {
                    setDatos.writeObject(true);
                    edadValido = true;
                }
            } catch (NumberFormatException nfe) {
                setDatos.writeObject(false);
                edadValido = false;
            }
        } while (!edadValido);

        boolean passwordValido;
        do {
            System.out.println("Validando contraseña del jugador...");
            ObjectInputStream get = new ObjectInputStream(client.getInputStream());
            String passwordJugador = new String(desCipher.doFinal((byte[]) get.readObject()));
            Pattern patpassword = Pattern.compile("^(?=\\w*\\d)(?=\\w*[A-Z])(?=\\w*[a-z])\\S{8,16}$");
            Matcher mat = patpassword.matcher(passwordJugador);

            if (!mat.matches()) {
                setDatos.writeObject(false);
                passwordValido = false;
            } else {
                setDatos.writeObject(true);
                passwordValido = true;
            }
        } while (!passwordValido);

    }

    // Reglas del juego firmada por el servidor y comprobadas por el cliente (se comprueba mediante firma digital)
    public void validarReglasdelJuego() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {

        ObjectOutputStream setReglas = new ObjectOutputStream(client.getOutputStream());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair par = keyGen.generateKeyPair();
        PrivateKey clavepriv = par.getPrivate();
        PublicKey clavepub = par.getPublic();

        Signature dsa = Signature.getInstance("SHA1withRSA");
        dsa.initSign(clavepriv);
        String mensaje = """
                1-Al empezar el juego se te mandan una pregunta con 2 o mas posibles respuestas, tienes que elegir una correcta mediente el número de la respuesta.
                2-Cada pregunta solo se puede responder una vez por partida.
                3-Si la respuestas es correcta se te añadiran 10 puntos a tus puntos totales y si es incorrecta se te quitarán 5 puntos.
                4-En cualquier momento puedes dejar de jugar escribiendo end como respuesta.
                5-Al final del juego se te mostrará los puntos conseguidos.
                """;
        dsa.update(mensaje.getBytes());
        byte[] firma = dsa.sign();

        setReglas.writeObject(clavepub);
        setReglas.writeObject(mensaje.getBytes());
        setReglas.writeObject(firma);

    }

    public void empezarJuego() {

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair par = keyGen.generateKeyPair();
            PrivateKey claveprivServer = par.getPrivate();
            PublicKey clavepubServer = par.getPublic();

            ObjectInputStream get = new ObjectInputStream(client.getInputStream());

            Cipher desCipherClient = Cipher.getInstance("RSA");
            desCipherClient.init(Cipher.ENCRYPT_MODE, (Key) get.readObject()); // El cliente me envia su clave publica

            String mensaje = """
                    1-Hay 5 estuches en la mesa. Cada uno contiene como mínimo 10 lápices y como máximo 14. ¿Cuál de estos podría ser el total de lápices?
                        1- 45.
                        2- 75.
                        3- 65.
                        4- 35.
                    """;
            byte[] mensajeCifra = desCipherClient.doFinal(mensaje.getBytes());
            // Enviar la pregunta y las respuestas al cliente
            ObjectOutputStream set = new ObjectOutputStream(client.getOutputStream());
            set.writeObject(mensajeCifra);

            set.writeObject(clavepubServer); // Envio mi clave publica al cliente

            Cipher desCipherServer = Cipher.getInstance("RSA");
            desCipherServer.init(Cipher.DECRYPT_MODE, claveprivServer);

            boolean finJuego = false;

            // Recibo la respuesta del cliente
            String mensajeDescifrado;
            byte[] mensajeCifradoBytes = (byte[]) get.readObject();
            mensajeDescifrado = new String(desCipherServer.doFinal(mensajeCifradoBytes));

            // Compruebo si la respuesta es correcta
            if (!mensajeDescifrado.equals("end")) {
                if (mensajeDescifrado.equals("3")) {
                    this.puntos += 10;
                }
                else {
                    if (this.puntos > 0) this.puntos -= 5;
                }
            }
            else {

                mensaje = "Juego terminado"
                        + "\nDni Jugador: " + this.dniJugador
                        + "\nPuntos: " + this.puntos;
                mensajeCifra = desCipherClient.doFinal(mensaje.getBytes());
                set.writeObject(mensajeCifra);

                finJuego = true;
                client.close();

            }

            if (!finJuego) {

                mensaje = """
                        2-Si X es menor que Y por una diferencia de 6 e Y es el doble de Z, ¿cuál es el valor de X cuando Z es igual a 2?
                            1- 5.
                            2- 8.
                            3- -2.
                            4- 10.
                        """;

                if (!crearPregunta(mensaje, "3", set, get, desCipherClient, desCipherServer)) {

                    mensaje = """
                            3- 4 x 4 - 4 + 4 x 4 = ¿...?
                                1- 64.
                                2- -4.
                                3- 28.
                                4- -16.
                            """;

                    if (!crearPregunta(mensaje, "3", set, get, desCipherClient, desCipherServer)) {

                        mensaje = """
                                4- 3 (x-4) = 18. ¿Cuál es el valor de X?
                                    1- 6.
                                    2- 14/3.
                                    3- 2/3.
                                    4- 10.
                                """;

                        if (!crearPregunta(mensaje, "4", set, get, desCipherClient, desCipherServer)) {

                            mensaje = """
                                    5-Cada estudiante puede elegir entre 4 tipos de sudadera y tres tipos de pantalones en su uniforme, ¿cuántas combinaciones posibles existen?
                                        1- 10.
                                        2- 24.
                                        3- 12.
                                        4- 7.
                                    """;

                            crearPregunta(mensaje, "3", set, get, desCipherClient, desCipherServer);

                            mensaje = "Juego terminado"
                                    + "\nDni Jugador: " + this.dniJugador
                                    + "\nPuntos: " + this.puntos;
                            mensajeCifra = desCipherClient.doFinal(mensaje.getBytes());
                            set.writeObject(mensajeCifra);

                        }

                    }

                }

            }

        } catch (Exception e) {
            System.out.println("[Server EmpezarJuego] " + e.getMessage());
        }

    }

    public boolean crearPregunta(String pregunta, String respuestaC, ObjectOutputStream set, ObjectInputStream get, Cipher desCipherClient, Cipher desCipherServer) throws BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException {

        boolean finJuego;

        byte[] mensajeCifra = desCipherClient.doFinal(pregunta.getBytes());
        set.writeObject(mensajeCifra);

        byte[] mensajeCifradoBytes = (byte[]) get.readObject();
        String mensajeDescifrado = new String(desCipherServer.doFinal(mensajeCifradoBytes));

        if (!mensajeDescifrado.equalsIgnoreCase("end")) {
            if (mensajeDescifrado.equals(respuestaC)) {
                this.puntos += 10;
            }
            else {
                if (this.puntos > 0) this.puntos -= 5;
            }
            finJuego = false;
        }
        else {

            pregunta = "Juego terminado"
                    + "\nDni Jugador: " + this.dniJugador
                    + "\nPuntos: " + this.puntos;
            mensajeCifra = desCipherClient.doFinal(pregunta.getBytes());
            set.writeObject(mensajeCifra);

            client.close();

            finJuego = true;

        }
        return finJuego;

    }
}
