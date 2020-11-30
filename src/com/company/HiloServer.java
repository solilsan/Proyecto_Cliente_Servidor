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

    public HiloServer(Socket client) throws IOException {
        this.client = client;
    }

    @Override
    public void run() {

        try {

            validarDatosJugador();
            validarReglasdelJuego();

            client.close();

        } catch (Exception e) {
            System.out.println("[Server Hilo Error] " + e.getMessage());
        }

    }

    public synchronized void validarDatosJugador() throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException {

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
            System.out.println("Validando Dni del jugador...");
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

    public synchronized void validarReglasdelJuego() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {

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
}
