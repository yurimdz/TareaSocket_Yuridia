package Paquete;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;

public class Cliente {
    private static JTextArea textArea;
    private static DataOutputStream out;
    private static FileWriter logWriter;

    public static void main(String[] args) {
        final String HOST = "localhost";
        final int PUERTO = 2000;

        JFrame frame = new JFrame("Cliente Restaurante");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JTextField textField = new JTextField();
        panel.add(textField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Enviar");
        panel.add(sendButton, BorderLayout.EAST);

        JButton verMenuButton = new JButton("Ver Menú");
        panel.add(verMenuButton, BorderLayout.WEST);

        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);

        try {
            Socket sc = new Socket(HOST, PUERTO);
            DataInputStream in = new DataInputStream(sc.getInputStream());
            out = new DataOutputStream(sc.getOutputStream());
            logWriter = new FileWriter("client_log.txt", true);

            Thread serverHandler = new Thread(() -> {
                try {
                    while (true) {
                        String mensaje = in.readUTF();
                        logMessage("Servidor", mensaje);
                    }
                } catch (IOException e) {
                    logMessage("Error", "Error al leer mensaje del servidor: " + e.getMessage());
                }
            });
            serverHandler.start();

            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String mensaje = textField.getText();
                        if (!mensaje.trim().isEmpty()) {
                            out.writeUTF(mensaje);
                            logMessage("Cliente", mensaje);
                            textField.setText("");
                        } else {
                            logMessage("Error", "No se puede enviar un mensaje vacío.");
                        }
                    } catch (IOException ex) {
                        logMessage("Error", "Error al enviar mensaje: " + ex.getMessage());
                    }
                }
            });

            verMenuButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        out.writeUTF("menu");
                    } catch (IOException ex) {
                        logMessage("Error", "Error al solicitar el menú: " + ex.getMessage());
                    }
                }
            });

        } catch (IOException e) {
            logMessage("Error", "Error: " + e.getMessage());
        }
    }

    private static void logMessage(String role, String message) {
        textArea.append(role + ": " + message + "\n");
        try {
            if (logWriter != null) {
                logWriter.write(role + ": " + message + "\n");
                logWriter.flush();
            }
        } catch (IOException e) {
            textArea.append("Error al registrar el mensaje: " + e.getMessage() + "\n");
        }
    }
}
