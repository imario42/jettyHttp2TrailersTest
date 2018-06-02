package at.datenwort.jettyTrailerTest.client;

import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Jetty;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.jetty.connector.LocalizationMessages;
import org.glassfish.jersey.message.internal.Statuses;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JettyConnector implements Connector
{
    private final HttpClient client;

    JettyConnector(final Client jaxrsClient, final Configuration config)
    {
        HttpClientTransport transport = new HttpClientTransportOverHTTP2(new HTTP2Client());
        this.client = new HttpClient(transport, null);

        try
        {
            client.start();
        }
        catch (final Exception e)
        {
            throw new ProcessingException("Failed to start the client.", e);
        }
    }

    @Override
    public ClientResponse apply(ClientRequest jerseyRequest)
    {
        final Request jettyRequest = translateRequest(jerseyRequest);

        try
        {
            // XXX IM ==> grab trailer fields
            final MutableObject<ClientResponse> clientResponseHandle = new MutableObject<>();
            final InputStreamResponseListener listener = new InputStreamResponseListener()
            {
                @Override
                public void onSuccess(Response response)
                {
                    System.err.println("onSuccess:" + jettyRequest);

                    ClientResponse jerseyResponse = clientResponseHandle.getValue();
                    if (jerseyResponse != null)
                    {
                        processResponseHeaders(((HttpResponse) response).getTrailers(), jerseyResponse, true);
                    }

                    super.onSuccess(response);
                }

                @Override
                public void onFailure(Response response, Throwable failure)
                {
                    System.err.println("onFailure:" + jettyRequest);

                    super.onFailure(response, failure);
                }

                @Override
                public void onComplete(Result result)
                {
                    System.err.println("onComplete:" + jettyRequest);

                    super.onComplete(result);
                }

                @Override
                public void onContent(Response response, ByteBuffer content, Callback callback)
                {
                    System.err.println("onContent:" + jettyRequest + " remaining: " + content.remaining());

                    ClientResponse jerseyResponse = clientResponseHandle.getValue();
                    if (jerseyResponse != null)
                    {
                        processResponseHeaders(((HttpResponse) response).getTrailers(), jerseyResponse, true);
                    }

                    super.onContent(response, content, callback);
                }
            };
            jettyRequest.send(listener);
            final Response jettyResponse = listener.get(10, TimeUnit.SECONDS);
            // XXX IM <==

            final javax.ws.rs.core.Response.StatusType status = jettyResponse.getReason() == null
                ? Statuses.from(jettyResponse.getStatus())
                : Statuses.from(jettyResponse.getStatus(), jettyResponse.getReason());

            final ClientResponse jerseyResponse = new ClientResponse(status, jerseyRequest);
            clientResponseHandle.setValue(jerseyResponse);
            processResponseHeaders(jettyResponse.getHeaders(), jerseyResponse, false);
            processResponseHeaders(((HttpResponse) jettyResponse).getTrailers(), jerseyResponse, true);
            jerseyResponse.setEntityStream(listener.getInputStream());

            return jerseyResponse;
        }
        catch (final Exception e)
        {
            throw new ProcessingException(e);
        }
    }

    @Override
    public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return "Jetty HttpClient " + Jetty.VERSION;
    }

    @Override
    public void close()
    {
        try
        {
            client.stop();
        }
        catch (final Exception e)
        {
            throw new ProcessingException("Failed to stop the client.", e);
        }
    }

    private static void processResponseHeaders(final HttpFields respHeaders, final ClientResponse jerseyResponse, boolean checkDuplicate)
    {
        if (respHeaders == null)
        {
            return;
        }

        for (final HttpField header : respHeaders)
        {
            final String headerName = header.getName();
            final MultivaluedMap<String, String> headers = jerseyResponse.getHeaders();
            List<String> list = headers.get(headerName);
            if (list == null)
            {
                list = new ArrayList<>();
            }
            if (!checkDuplicate || !list.contains(header.getValue()))
            {
                // System.err.println("adding header: " + headerName + " = " + header.getValue());
                list.add(header.getValue());
            }
            headers.put(headerName, list);
        }
    }

    private Request translateRequest(final ClientRequest clientRequest)
    {
        final HttpMethod method = HttpMethod.fromString(clientRequest.getMethod());
        if (method == null)
        {
            throw new ProcessingException(LocalizationMessages.METHOD_NOT_SUPPORTED(clientRequest.getMethod()));
        }
        final URI uri = clientRequest.getUri();
        final Request request = client.newRequest(uri);
        request.method(method);

        return request;
    }
}
