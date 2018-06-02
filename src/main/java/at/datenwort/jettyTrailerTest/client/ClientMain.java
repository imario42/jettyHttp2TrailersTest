package at.datenwort.jettyTrailerTest.client;

import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class ClientMain
{
    public static void main(String[] args)
    {
        ClientConfig cc = new ClientConfig();
        cc.connectorProvider(new JettyConnectorProvider());
        Client client = ClientBuilder.newBuilder()
            .withConfig(cc)
            .build();

        WebTarget webTarget = client.target("http://localhost:2880/simpleService");

        int dataLength = webTarget.path("getDataLength").request().method("GET", int.class);

        Invocation.Builder invocationBuilder = webTarget.path("getData").request();
        Response response = invocationBuilder.method("GET");
        String result = response.readEntity(String.class);
        String serverTiming = response.getHeaderString("Server-Timing");

        System.err.println("expected=" + dataLength + " got=" + result.length());
        System.err.println("start marker=" + result.startsWith("*START_MARKER*"));
        System.err.println("end marker=" + result.endsWith("*END_MARKER*"));
        System.err.println("serverTiming=" + serverTiming);

        System.exit(0);
    }
}
