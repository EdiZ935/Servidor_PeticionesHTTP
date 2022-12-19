import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server{
    public static final int port = 8000;
    ServerSocket servSoc;
    public static int poolSize=1;
        static class Manejador extends Thread{
            int codeNumber = 200;
            String response = "OK";
            String contentType = "text/html";
            String path = "src/ContenidoServer/";
            protected Socket socket;
            protected PrintWriter pw;
            protected BufferedOutputStream bos;
            protected BufferedReader br;
            protected DataInputStream dis;
            protected String FileName = "";
            protected Dictionary<String, String> MIME = new Hashtable<String, String>();
            protected final String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                                                + "Date: "+new Date()+"\n"
                                                + "Server: EGZ_KYF Server/1.0\n"
                                                + "Content-Type: "+contentType+" \n";
            protected final String deleteHTTP_Ok = headerHTTP+"\n"
                                                + "<html><head><meta charset='UTF-8'><title>"+codeNumber+"  </title></head>"
                                                + "<body><h1>  </h1>"
                                                + "<p>Elemento: "+ FileName +" fue eliminado exitosamente mediante DELETE</p>"
                                                + "</body></html>";
            protected final String E404 = "HTTP/1.1 404 Not Found\n"
                                        + "Date: "+new Date()+"\n"
                                        + "Server: EGZ_KYF Server/1.0\n"
                                        + "Content-Type: text/html \n\n"
                    + "<html><head><meta charset='UTF-8'><title>404 NOT FOUND  </title></head>"
                    + "<body><h1> Puede tratarse de una página eliminada que no tiene reemplazo o equivalente"
                    + " o se trata de una página que simplemente no existe </h1>"
                    + "<p>Elemento: "+ FileName +" NO EXISTE O FUE ELIMINADO</p>"
                    + "</body></html>";
            protected final String E500 = "<html><head><meta charset='UTF-8'><title>500 INTERNAL SERVER ERROR  </title></head>"
                                        + "<body><h1> UPS, OCURRIÓ UN ERROR INESPERADO</h1>"
                                        + "<p>No se pudo concretar la operación</p>"
                                        + "</body></html>";
            protected final String E403 = "<html><head><meta charset='UTF-8'><title>403 FORBIDDEN  </title></head>"
                                        + "<body><h1>The server understands the request but refuses to authorize it</h1>"
                                        + "<p>The access is tied to the application logic, such as insufficient rights to a resource.</p>"
                                        + "</body></html>";
            public Manejador(Socket s)throws IOException{
                this.socket = s;
                this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));//Input : Lectura de línea
                this.dis = new DataInputStream(this.socket.getInputStream());
                this.bos = new BufferedOutputStream(socket.getOutputStream());//Output
                this.pw = new PrintWriter(new OutputStreamWriter(bos));//Output
                this.MIME.put("txt", "text/plain");
                this.MIME.put("html", "text/html");
                this.MIME.put("htm", "text/html");
                this.MIME.put("jpg", "image/jpeg");
                this.MIME.put("jpeg", "image/jpeg");
                this.MIME.put("png", "image/png");
                this.MIME.put("pdf", "application/pdf");
                this.MIME.put("doc", "application/msword");
                this.MIME.put("rar", "application/x-rar-compressed");
                this.MIME.put("mp3", "audio/mpeg");
                this.MIME.put("mp4", "video/mp4");
                this.MIME.put("c", "text/plain");
                this.MIME.put("java", "text/plain");
            } // CONSTRUCTOR
            public void run(){ // START..........
                try {

                    String linea = br.readLine();
                    if(linea==null){
                        pw.print("<html><head><title>Servidor WEB");
                        pw.print("</title><body bgcolor=\"#AACCFF\"<br>Linea Vacia</br>");
                        pw.print("</body></html>");
                        socket.close();
                        return;
                    }//END IF LINE = NULL

                    System.out.println("\nCliente Conectado desde: "+socket.getInetAddress());
                    System.out.println("Por el puerto: "+socket.getPort());
                    System.out.println("Datos: "+linea+"\r\n\r\n");

                    if (linea.toUpperCase().startsWith("GET"))
                    {
                        getFileName(linea);
                        GET();
                    }
                    else if(linea.toUpperCase().startsWith("DELETE"))
                    {
                        getFileName(linea);
                        DELETE();
                    }
                    else if (linea.toUpperCase().startsWith("HEAD"))
                    {
                        HEAD(linea);
                    }
                    else if (linea.toUpperCase().startsWith("POST"))
                    {
                        int tam = dis.available();
                        byte[] b = new byte[tam];
                        dis.read(b);
                        String request = new String(b, 0, tam);
                        String htmlPost = POST(request);
                        pw.println(htmlPost);
                        pw.flush();
                        bos.flush();
                    }
                    else if (!linea.contains("?"))
                    {
                        getFileName(linea);
                        if(FileName.compareTo("")==0)
                            SendF(path+"index.html");
                        else
                            SendF(path+FileName);
                    }
                    else
                    {
                        pw.println("HTTP/1.0 501 Not Implemented");
                        pw.println();
                    }
                    pw.flush();
                    bos.flush();

                }catch (Exception e){ // End of TRY.... RUN
                    e.printStackTrace();
                }
            }//END RUN....
            public void DELETE(){
                try{
                    System.out.println("Petición de eliminado de: " + FileName);
                    File file = new File(path+FileName);
                    if(file.exists()){
                        if(file.delete()){
                            System.out.println("Petición de eliminado de: "+ FileName +"   ha sido exitosa.");
                            this.codeNumber = 200;
                            this.response = "OK";
                            this.contentType = "text/html";
                            pw.println(deleteHTTP_Ok);
                            pw.flush();
                        }else{
                            this.codeNumber = 500;
                            this.response = "Internal Server Error";
                            this.contentType = "text/html";
                            pw.println(headerHTTP+"\n"+E500);
                            pw.flush();
                        }
                    }else{
                        System.out.println("Petición de eliminado de: "+FileName+"   ha fracasado. NO SE ENCONTRÓ");
                        this.codeNumber = 404;
                        this.response = "Not Found";
                        this.contentType = "text/html";
                        pw.println(E404);
                        pw.flush();
                    }
                    pw.flush();
                    bos.flush();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }//END of Method DELETE....
            public void GET(){
                try {
                    File temp = new File(path+FileName);
                    if (temp.exists()) {
                        if (SendF(path+FileName)) {
                            System.out.println("Petición de lectura de: " + FileName + "   ha sido exitosa.");
                        } else {
                            System.out.println("Petición de lectura de: " + FileName + "   ha fracasado.");
                            this.codeNumber = 500; // Error del servidor
                            this.response = "Internal Server Error";
                            this.contentType = "text/html";
                            pw.println(headerHTTP+"\n"+E500);
                            pw.flush();
                        }
                    } else {
                        System.out.println("Petición de lectura de: " + FileName + "   ha fracasado. NOT FOUND");
                        this.codeNumber = 404;
                        pw.println(E404);
                        pw.flush();
                    }
                    pw.flush();
                    bos.flush();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }//END of Method GET....
            public String POST(String request) {
                String[] reqLineas = request.split("\n");
                String method = "POST";
                StringTokenizer paramsTokens = new StringTokenizer(reqLineas[reqLineas.length-1], "&");

                this.codeNumber = 200; // Error del servidor
                this.response = "OK";
                this.contentType = "text/html";
                String html = headerHTTP
                        + "<html><head><meta charset='UTF-8'><title>Metodo " + method + "\n"
                        + "</title></head><body bgcolor='#AACCFF'><center><h2>Parametros obtenidos por medio de " + method + "</h2><br>\n"
                        + "<table border='2'><tr><th>Parametro</th><th>Valor</th></tr>";

                while (paramsTokens.hasMoreTokens()) {
                    String parametros = paramsTokens.nextToken();
                    StringTokenizer paramValue = new StringTokenizer(parametros, "=");
                    String param = ""; //Parámetro
                    String value = ""; //Valor
                    if (paramValue.hasMoreTokens()) {
                        param = paramValue.nextToken();
                    }
                    if (paramValue.hasMoreTokens()) {
                        value = paramValue.nextToken();
                    }
                    html = html + "<tr><td><b>" + param + "</b></td><td>" + value + "</td></tr>\n";
                }
                html = html + "</table></center></body></html>";
                return html;
            }
            public void HEAD(String linea)throws Exception{
                if (!linea.contains("?")) {
                    //  nombre archivo y únicamente enviamos tipo MIME y longitud
                    getFileName(linea);
                    File file = new File(path+FileName);
                    if(!file.exists()){
                        this.FileName = "page404.html";
                        this.codeNumber = 404;
                        this.response = "Not Found";
                        this.contentType = "text/html";
                        pw.println(E404);
                        pw.flush();
                    } else if (file.isDirectory()){
                        this.FileName = "page403.html";
                        this.codeNumber = 403;
                        this.response = "Forbidden\n";
                        this.contentType = "text/html";
                        pw.println(headerHTTP+"\n"+E403);
                    }else{
                        int posExt = FileName.indexOf(".")+1;
                        String ext = FileName.substring(posExt);
                        this.contentType = MIME.get(ext);
                        pw.println(headerHTTP+"Content-Length: " + file.length() +" \n\n");
                    }

                } else {
                    // Se devuelven únicamente header html
                    this.codeNumber = 200;
                    this.response = "OK";
                    this.contentType = "text/html";
                    pw.println(headerHTTP);
                }
                pw.flush();
                bos.flush();
            }
            void getFileName(String comando){
                int i = comando.indexOf("/");
                int f = comando.indexOf(" ", i);
                this.FileName = comando.substring(i + 1, f);
            }
            public boolean SendF(String arg){
                try{
                   //Manejo del tipo de contenido: MIME:
                    int posExt = FileName.indexOf(".")+1;
                    String ext = FileName.substring(posExt);
                    this.contentType = MIME.get(ext);
                    System.out.println(contentType);

                    int b_leidos;
                    BufferedInputStream bis2=new BufferedInputStream(new FileInputStream(arg));
                    byte[] buf=new byte[1024];
                    int tam_archivo=bis2.available();

                    String sb = "";
                    sb = sb+"HTTP/1.0 202 Accepted\n";
                    sb = sb +"Server: EGZ_KYF Server/1.0 \n";
                    sb = sb +"Date: " + new Date()+" \n";
                    sb = sb +"Content-Type: "+contentType+" \n";
                    sb = sb +"Content-Length: "+tam_archivo+" \n";
                    sb = sb +"\n";
                    bos.write(sb.getBytes());
                    bos.flush();

                    while((b_leidos=bis2.read(buf,0,buf.length))!=-1) {
                        bos.write(buf,0,b_leidos);
                    }
                    bos.flush();
                    bis2.close();

                } catch(Exception e){
                    System.out.println(e.getMessage());
                    return false;
                }
                return true;
            } // END of Method SendF....

        }//END MANEJADOR CLASS

    public Server() throws Exception{ // CONSTRUCTOR DE LA CLASE SERVER

        System.out.println("Servidor iniciado...");
        try (Scanner scan = new Scanner(System.in)) {
            System.out.print("Tam del pool de conexiones que desea: \n");
            poolSize = scan.nextInt();
        }
        ExecutorService poolHilos = Executors.newFixedThreadPool(poolSize); // aquí se define el tamaño

        this.servSoc = new ServerSocket(port);
        for(;;){
            System.out.println("Esperando Cliente... en puerto"+ port);
            Socket cliente = servSoc.accept();
            poolHilos.execute(new Manejador(cliente)); // Se ejecuta según el pool de hilos :)
        }
    }//Constructor

    public static void main(String[] args){
            try {
                Server Servidor = new Server();
            }catch(Exception e){
                e.printStackTrace();
            }
    }
}
