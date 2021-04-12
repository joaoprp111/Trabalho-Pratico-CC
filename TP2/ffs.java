DatagramSocket s = new DatagramSocket(9876);

byte[] enviar = new byte[1924];

DatagramPacket pedido = new DatagramPacket(enviar,enviar.length);
s.send(pedido);