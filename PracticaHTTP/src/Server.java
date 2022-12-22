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
            String path = "src/ContenidoServer/";
            protected Socket socket;
            protected PrintWriter pw;
            protected BufferedOutputStream bos;
            protected BufferedReader br;
            protected DataInputStream dis;
            protected String FileName = "";
            protected Dictionary<String, String> MIME = new Hashtable<String, String>();
            protected String deleteHtml_Ok ="<html><head><meta charset='UTF-8'><title>"+codeNumber+"  </title></head>"
                                                + "<body><h1>  </h1>"
                                                + "<p>Elemento: "+ FileName +" fue eliminado exitosamente mediante DELETE</p>"
                                                + "</body></html>";
            protected String E404 = "HTTP/1.1 404 Not Found\n"
                                        + "Date: "+new Date()+"\n"
                                        + "Server: EGZ_KYF Server/1.0\n"
                                        + "Content-Type: text/html \n\n"
                    + "<html><head><meta charset='UTF-8'><title>404 NOT FOUND  </title></head>"
                    + "<body><h1> Puede tratarse de una página eliminada que no tiene reemplazo o equivalente"
                    + " o se trata de una página que simplemente no existe </h1>"
                    ;
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
                    String linea = br.readLine(); // Se lee la petición que se va a ejecutar
                    if(linea==null){ // si la linea esta vacía se envía texto html
                        pw.print("<html><head><title>Servidor WEB EGZ_KYF");
                        pw.print("</title><body bgcolor=\"#AACCFF\"<br>Linea Vacia</br>");
                        pw.print("</body></html>");
                        socket.close();
                        return;
                    }//END IF LINE = NULL

                    System.out.println("\nCliente Conectado desde: "+socket.getInetAddress());
                    System.out.println("Por el puerto: "+socket.getPort());
                    System.out.println("Datos: "+linea+"\r\n\r\n");

                    if (linea.toUpperCase().startsWith("GET")) // Caso que la petición empiece con la palabra "GET"
                    {
                        getFileName(linea);//Se actualiza el file name según la petición recibida
                        GET();//Se ejecuta el método GET usando la variable global actualizada 'FileName'
                    }
                    else if(linea.toUpperCase().startsWith("DELETE"))// Caso que la petición empiece con la palabra "DELETE"
                    {
                        getFileName(linea);//Se actualiza el file name según la petición recibida
                        DELETE();//Se ejecuta el método DELETE usando la variable global actualizada 'FileName'
                    }
                    else if (linea.toUpperCase().startsWith("HEAD"))
                    {
                        //getFileName es llamado dentro de la misma función HEAD
                        HEAD(linea);//Se ejecuta el método HEAD usando la variable global actualizada 'FileName'
                    }
                    else if (linea.toUpperCase().startsWith("POST"))
                    {
                        System.out.println("Recibiendo POST...");
                        int tam = dis.available(); // Se lee el tamaño del flujo de entrada para saber que tamaño tiene que leer
                        byte[] b = new byte[tam];//Se inicia un arreglo de bytes con el tamaño obtenido
                        dis.read(b);//Se leen los datos
                        String request;
                        if(linea.contains("=")){
                            request = linea;
                        }else{
                            request = new String(b, 0, tam);
                        }

                        String htmlPost = POST(request);// Se manda a llamar la función POST recuperando una String que representa la respuesta
                        System.out.println("Respuesta:");
                        System.out.println(htmlPost);
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
                    System.out.println("Petición de eliminado del archivo: " + FileName);
                    File file = new File(path+FileName);
                    if(file.exists()){
                        if(file.delete()){//Si fue posible eliminarlo:
                            System.out.println("Petición de eliminado de: "+ FileName +"   ha sido exitosa.");
                            this.codeNumber = 200;
                            this.response = "OK";
                            String contentType = "text/html";
                            String headerHTTP = "HTTP/1.1 "+ 200 +" "+"OK"+"\n"
                                    + "Date: "+new Date()+"\n"
                                    + "Server: EGZ_KYF Server/1.0\n"
                                    + "Content-Type: "+contentType+" \n\n";
                            pw.println(headerHTTP+deleteHtml_Ok);
                            pw.flush();
                        }else{//Si no fue posible eliminarlo:

                            String contentType = "text/html";
                            String headerHTTP = "HTTP/1.1 "+ 500 +" "+"Internal Server Error"+"\n"
                                    + "Date: "+new Date()+"\n"
                                    + "Server: EGZ_KYF Server/1.0\n"
                                    + "Content-Type: "+contentType+" \n";
                            pw.println(headerHTTP+"\n"+E500);//Se actualizan los parámetros anteriores y envía encabezado y http con mensaje.
                            pw.flush();
                        }
                    }else{//Si no existe el archivo....
                        System.out.println("Petición de eliminado de: "+FileName+"   ha fracasado. NO SE ENCONTRÓ");
                        this.codeNumber = 404;
                        this.response = "Not Found";
                        pw.println(E404+ "<p>Elemento: "+ FileName +" NO EXISTE O FUE ELIMINADO</p>"
                                + "</body></html>");//Se envía la String E404 que contiene encabezado y texto http
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
                        if (SendF(path+FileName)) {//Si la función de mandado de archivo regresa true:
                            System.out.println("Petición de lectura de: " + FileName + "   ha sido exitosa.");
                        } else {//Si hubo un error en el enviado del archivo solicitado:
                            System.out.println("Petición de lectura de: " + FileName + "   ha fracasado.");
                            String contentType = "text/html";
                            String headerHTTP = "HTTP/1.1 "+ 500 +" "+"Internal Server Error"+"\n"
                                    + "Date: "+new Date()+"\n"
                                    + "Server: EGZ_KYF Server/1.0\n"
                                    + "Content-Type: "+contentType+" \n";
                            pw.println(headerHTTP+"\n"+E500);//Se manda el encabezado actualizado y html de Error 500
                            pw.flush();
                        }
                    } else {//Si no existe el archivo que se está intentando obtener:
                        System.out.println("Petición de lectura de: " + FileName + "   ha fracasado. NOT FOUND");
                        this.codeNumber = 404;
                        pw.println(E404);//Se manda encabezado y texto html de Error 404
                        pw.flush();
                    }
                    pw.flush();
                    bos.flush();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }//END of Method GET....
            public String POST(String request) {
                int indice = request.indexOf("/");
                if(request.contains("/")){
                    if(request.contains("?"))
                        request = request.substring(indice+2);
                    else
                        request = request.substring(indice+1);
                    StringTokenizer post = new StringTokenizer(request," ");
                    request = post.nextToken();
                }
                String[] reqLineas = request.split("\n");
                StringTokenizer tokens = new StringTokenizer(reqLineas[reqLineas.length-1], "&?");
                System.out.println(reqLineas[reqLineas.length-1]);

                String contentType = "text/html";
                String headerHTTP = "HTTP/1.1 "+ "200" +" "+"OK"+"\n"
                        + "Date: "+new Date()+"\n"
                        + "Server: EGZ_KYF Server/1.0\n"
                        + "Content-Type: "+contentType+" \n\n";
                String html = headerHTTP // encabezado http con los valores actualizados:
                        + "<html><head><meta charset='UTF-8'><title> Método POST </title></head>\n"
                        + "<body ><center><h2> Se han obtenido los siguientes valores con sus respectivos parámetros</h2><br>\n"
                        + "<table border='2'><tr><th>Valores:</th><th>Valor</th></tr>";

                while (tokens.hasMoreTokens()) {
                    String postValues = tokens.nextToken();
                    System.out.println(postValues);
                    StringTokenizer postValue = new StringTokenizer(postValues, "=");
                    String parametro = ""; //Parámetro
                    String valor = ""; //Valor
                    if (postValue.hasMoreTokens()) {
                        parametro = postValue.nextToken();
                    }
                    if (postValue.hasMoreTokens()) {
                        valor = postValue.nextToken();
                    }
                    html = html + "<tr><td><b>" + parametro + "</b></td><td>" + valor + "</td></tr>\n";
                }
                html = html + "</table></center></body></html>";
                return html;
            }
            public void HEAD(String linea)throws Exception{
                getFileName(linea);
                File file = new File(path+FileName);
                if (!linea.contains("?")||file.exists()) {
                    //Se actualiza el FileName según la línea de petición recibida
                    if (file.isDirectory()){//Si se está intentando acceder a un directorio:
                        this.FileName = "page403.html";
                        int codeNumber = 403;
                        String response = "Forbidden\n";
                        String contentType = "text/html";
                        String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                                + "Date: "+new Date()+"\n"
                                + "Server: EGZ_KYF Server/1.0\n"
                                + "Content-Type: "+contentType+" \n";
                        pw.println(headerHTTP+"\n"+E403);//Se manda el encabezado actualizado y la sección html Error 403
                    }else{//Si el archivo existe: se mandan los encabezados actualizando el tipo de archivo
                        int posExt = FileName.indexOf(".")+1;
                        String ext = FileName.substring(posExt);
                        String contentType = MIME.get(ext);
                        String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                                + "Date: "+new Date()+"\n"
                                + "Server: EGZ_KYF Server/1.0\n"
                                + "Content-Type: "+contentType+" \n";
                        pw.println(headerHTTP+"Content-Length: " + file.length() +" \n\n");//Se añade fileLength al encabezado
                        System.out.println(headerHTTP+"Content-Length: " + file.length() +" \n\n");
                    }

                }else if(!file.exists()){//Si el archivo no existe:
                    this.FileName = "page404.html";
                    this.codeNumber = 404;
                    this.response = "Not Found";
                    pw.println(E404+ "<p>Elemento: "+ FileName +" NO EXISTE O FUE ELIMINADO</p>"
                            + "</body></html>");//Se manda el encabezado y texto html de Error 404

                    pw.flush();
                }else { // Si la línea contiene: "?"
                    // Se envía únicamente header html
                    this.codeNumber = 200;
                    this.response = "OK";
                    String contentType = "text/html";
                    String headerHTTP = "HTTP/1.1 "+ codeNumber +" "+response+"\n"
                            + "Date: "+new Date()+"\n"
                            + "Server: EGZ_KYF Server/1.0\n"
                            + "Content-Type: "+contentType+" \n";
                    pw.println(headerHTTP);
                }
                pw.flush();
                bos.flush();
            }//End of Method HEAD...
            void getFileName(String comando){//Actualiza la variable global FileName a partir del archivo solicitado
                //Se usa la linea de petición que se le pasa como parámetro:
                int i = comando.indexOf("/");
                if(comando.indexOf("?")==(i+1)){
                    i++;
                }
                int f = comando.indexOf(" ", i);
                this.FileName = comando.substring(i + 1, f);
            }
            public boolean SendF(String file){//Función Send File
                try{
                   //Manejo del tipo de contenido: MIME:
                    int posExt = FileName.indexOf(".")+1;
                    String ext = FileName.substring(posExt);//Se obtiene la extensión del archivo y

                    int b_leidos;
                    BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(file));
                    byte[] buf = new byte[1024];
                    int tam_archivo=bis2.available();//Se mide el tamaño a partir del flujo obtenido
                    
                    //Se prepara y se escribe el encabezado del archivo que se va a mandar:
                    String head = "HTTP/1.0 202 Accepted\n"
                                + "Server: EGZ_KYF Server/1.0 \n"
                                + "Date: " + new Date()+" \n"
                                + "Content-Type: "+MIME.get(ext)+" \n"//Se utiliza el contentType obtenido con el diccionario MIME
                                + "Content-Length: "+tam_archivo+" \n\n";

                    bos.write(head.getBytes());//Se escribe la cadena 
                    bos.flush();

                    while((b_leidos = bis2.read(buf,0,buf.length))!=-1) {
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
