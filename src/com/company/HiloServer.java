package com.company;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HiloServer extends Thread{

    private final Socket client;
    private final ObjectOutputStream set;

    public HiloServer(Socket client) throws IOException {
        this.client = client;
        this.set = new ObjectOutputStream(client.getOutputStream());
    }

    @Override
    public void run() {

        try {
            KeyGenerator keygen = KeyGenerator.getInstance("DES");
            SecretKey key = keygen.generateKey();
            this.set.writeObject(key);

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
                    this.set.writeObject(false);
                    dniValido = false;
                } else {
                    this.set.writeObject(true);
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
                    this.set.writeObject(false);
                    nombreValido = false;
                } else {
                    this.set.writeObject(true);
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
                    this.set.writeObject(false);
                    apellidoValido = false;
                } else {
                    this.set.writeObject(true);
                    apellidoValido = true;
                }
            } while (!apellidoValido);

            boolean edadValido;
            do {
                System.out.println("Validando edad del jugador...");
                ObjectInputStream get = new ObjectInputStream(client.getInputStream());
                String edadJugador = new String(desCipher.doFinal((byte[]) get.readObject()));

                if (Integer.parseInt(edadJugador) < 18) {
                    this.set.writeObject(false);
                    edadValido = false;
                } else {
                    this.set.writeObject(true);
                    edadValido = true;
                }
            } while (!edadValido);

            this.set.close();
            client.close();

        } catch (Exception e) {
            System.out.println("[Server Hilo Error] " + e.getMessage());
        }

    }
}
