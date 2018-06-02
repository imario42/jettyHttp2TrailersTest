package at.datenwort.jettyTrailerTest.client;

import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

public class JettyConnectorProvider implements ConnectorProvider
{
    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig)
    {
        return new JettyConnector(client, runtimeConfig);
    }
}
