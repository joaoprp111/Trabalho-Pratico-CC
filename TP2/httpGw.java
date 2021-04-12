DatagramSocket s = new DatagramSocket(9876);

byte[] receber = new byte[1924];

DatagramPacket pedido = new DatagramPacket(receber,receber.length);
s.receive(pedido);