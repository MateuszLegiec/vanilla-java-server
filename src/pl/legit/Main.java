package pl.legit;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(8080)) {

            System.out.println("Server is started");

            while (true) {
                Socket socket = serverSocket.accept();

                try {
                    InputStream is = socket.getInputStream();
                    InputStreamReader isReader = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isReader);

                    LinkedList<String> headers = new LinkedList<>();
                    String headerLine;
                    while ((headerLine = br.readLine()).length() != 0) {
                        headers.push(headerLine);
                    }

                    StringBuilder body = new StringBuilder();
                    while (br.ready()) {
                        body.append((char) br.read());
                    }
                    Request request = new Request(headers, body.toString());
                    System.out.println(request);
                } catch (IOException | ScriptException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Request {

    private static final String EXTRACTOR_SCRIPT = "var fun = function(raw) { return JSON.parse(raw);};";
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

    enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE
    }

    private final HttpMethod httpMethod;
    private final List<String> headerLines;
    private final Map<String, Object> payload;

    Request(List<String> headerLines, String body) throws ScriptException, NoSuchMethodException {
        engine.eval(EXTRACTOR_SCRIPT);
        Invocable invocable = (Invocable) engine;
        payload = (Map<String, Object>) invocable.invokeFunction("fun", body);

        if (headerLines.isEmpty()){
            throw new IllegalArgumentException();
        }

        final String methodHeader = headerLines.get(0);
        if (methodHeader.contains(HttpMethod.GET.name())) this.httpMethod = HttpMethod.GET;
        else if (methodHeader.contains(HttpMethod.POST.name())) this.httpMethod = HttpMethod.POST;
        else if (methodHeader.contains(HttpMethod.PUT.name())) this.httpMethod = HttpMethod.PUT;
        else if (methodHeader.contains(HttpMethod.PATCH.name())) this.httpMethod = HttpMethod.PATCH;
        else if (methodHeader.contains(HttpMethod.DELETE.name())) this.httpMethod = HttpMethod.DELETE;
        else throw new IllegalArgumentException();

        this.headerLines = headerLines;
    }
}

