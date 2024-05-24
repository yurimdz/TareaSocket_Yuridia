package PaqueteServidor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Servidor {

    private static JTextArea textArea;
    private static DataOutputStream out;
    private static FileWriter logWriter;
    private static Map<String, String> menu;
    private static StringBuilder historialPedidos = new StringBuilder();

    public static void main(String[] args) {
        
        final int PUERTO = 3000;

        JFrame frame = new JFrame("Servidor Restaurante");
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

        JButton menuButton = new JButton("Ver Menú");
        panel.add(menuButton, BorderLayout.WEST);

        JButton eliminarButton = new JButton("Eliminar Producto");
        panel.add(eliminarButton, BorderLayout.SOUTH);

        JButton modificarButton = new JButton("Modificar Producto");
        panel.add(modificarButton, BorderLayout.NORTH);

        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);

        inicializarMenu();

        try {
            ServerSocket servidor = new ServerSocket(PUERTO);
            logWriter = new FileWriter("server_log.txt", true);
            logMessage("Servidor", "Se ha iniciado el servidor\n");

            Socket sc = servidor.accept();
            DataInputStream in = new DataInputStream(sc.getInputStream());
            out = new DataOutputStream(sc.getOutputStream());

            Thread clientHandler = new Thread(() -> {
                
                try {
                    while (true) {
                        String mensaje = in.readUTF();
                        logMessage("Cliente", mensaje);
                        String respuesta = procesarMensaje(mensaje);
                        out.writeUTF(respuesta);
                        logMessage("Servidor", respuesta);
                    }
                } catch (IOException e) {
                    logMessage("Error", "Error al leer mensaje del cliente: " + e.getMessage());
                }
            });
            clientHandler.start();

            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String mensaje = textField.getText();
                        if (!mensaje.trim().isEmpty()) {
                            out.writeUTF(mensaje);
                            logMessage("Servidor", mensaje);
                            textField.setText("");
                        } else {
                            logMessage("Error", "No se puede enviar un mensaje vacío.");
                        }
                    } catch (IOException ex) {
                        logMessage("Error", "Error al enviar mensaje: " + ex.getMessage());
                    }
                }
            });

            menuButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    
                    try {
                        out.writeUTF(obtenerMenu());
                    } catch (IOException ex) {
                        logMessage("Error", "Error al enviar el menú: " + ex.getMessage());
                    }
                }
            });

            eliminarButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String numeroProducto = JOptionPane.showInputDialog("Ingrese el número del producto a eliminar:");
                    eliminarProducto(numeroProducto);
                }

                private void eliminarProducto(String numeroProducto) {
                    if (menu.containsKey(numeroProducto)) {
                        String productoEliminado = menu.remove(numeroProducto);
                        logMessage("Servidor", "Producto eliminado del menú: " + productoEliminado);
                    } else {
                        logMessage("Error", "No existe un producto con el número " + numeroProducto + " en el menú.");
                    }
                }

            });

            modificarButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String numeroProducto = JOptionPane.showInputDialog("Ingrese el número del producto a modificar:");
                    String nuevoNombre = JOptionPane.showInputDialog("Ingrese el nuevo nombre del producto:");
                    String nuevoPrecioStr = JOptionPane.showInputDialog("Ingrese el nuevo precio del producto:");
                    double nuevoPrecio = Double.parseDouble(nuevoPrecioStr);
                    modificarProducto(numeroProducto, nuevoNombre, nuevoPrecio);
                }

                private void modificarProducto(String numeroProducto, String nuevoNombre, double nuevoPrecio) {
                    if (menu.containsKey(numeroProducto)) {
                        String productoAnterior = menu.get(numeroProducto);
                        menu.put(numeroProducto, nuevoNombre + " - ₡" + nuevoPrecio);
                        logMessage("Servidor", "Producto modificado en el menú: " + productoAnterior + " -> " + nuevoNombre + " - ₡" + nuevoPrecio);
                    } else {
                        logMessage("Error", "No existe un producto con el número " + numeroProducto + " en el menú.");
                    }
                }

            });

        } catch (IOException e) {
            logMessage("Error", "Error: " + e.getMessage());
        }
    }

    private static void inicializarMenu() {
        menu = new HashMap<>();
        menu.put("1", "Hamburguesa - ₡3 500");
        menu.put("2", "Pizza - ₡5 550");
        menu.put("3", "Ensalada - ₡3 000");
        menu.put("4", "Gaseosa 700ml - ₡1 500");
        menu.put("5", "Milkshake - ₡2 500");
        menu.put("6", "Nuggets (10) - ₡4 000");
        menu.put("7", "Agua en botella 700ml - ₡1 000");
        menu.put("8", "Tropical 700ml - ₡1 500");
        
    }

    private static String procesarMensaje(String mensaje) {
        
    if (mensaje.equalsIgnoreCase("menu")) {
        return obtenerMenu();
    } else if (menu.containsKey(mensaje)) {
        String pedido = menu.get(mensaje);
        agregarPedidoAlHistorial(pedido);
        return "Pedido recibido: " + pedido;
    } else {
        return "Le saludamos. Escriba 'menu' o presione 'Ver Menú' para ver las opciones.\n";
    }
}



    private static String obtenerMenu() {
        
        StringBuilder sb = new StringBuilder("Menu (escriba el número del producto para seleccionar):\n\n");
        for (Map.Entry<String, String> entry : menu.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
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

    private static void agregarPedidoAlHistorial(String pedido) {
        historialPedidos.append("Pedido: ").append(pedido).append("\n");
    }

    public static String obtenerHistorialPedidos() {
        return historialPedidos.toString();

    }
}
