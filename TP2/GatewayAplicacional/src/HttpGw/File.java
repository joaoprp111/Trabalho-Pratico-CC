package HttpGw;

// Classe responsável por armazenar informações de um ficheiro
public class File {
    private byte[] fileRebuild; // Byte array que será usado para reconstruir o ficheiro, para depois enviar ao cliente
    private int bytesWritten; // Número de bytes já escritos no fileRebuild até ao momento
    private int size; // Tamanho total de bytes que o ficheiro original ocupa

    public File(byte[] fileRebuild, int bytesWritten, int size) {
        this.fileRebuild = fileRebuild;
        this.bytesWritten = bytesWritten;
        this.size = size;
    }

    public byte[] getFileRebuild() {
        return fileRebuild;
    }

    public void setFileRebuild(byte[] fileRebuild) {
        this.fileRebuild = fileRebuild;
    }

    public int getBytesWritten() {
        return bytesWritten;
    }

    public void setBytesWritten(int bytesWritten) {
        this.bytesWritten = bytesWritten;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        this.fileRebuild = new byte[size];
    }

    public void writeInArray(long offset, long chunkSize, byte[] chunk){
        System.arraycopy(chunk,0,this.fileRebuild,(int)offset,(int)chunkSize);
        this.bytesWritten += (int) chunkSize;
    }
}
