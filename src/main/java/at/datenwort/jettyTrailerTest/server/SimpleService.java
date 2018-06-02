package at.datenwort.jettyTrailerTest.server;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/simpleService")
@Consumes(value = MediaType.APPLICATION_JSON)
@Produces(value = MediaType.APPLICATION_JSON)
public class SimpleService
{
    private final static String LONG_DATA;

    static
    {
        StringBuilder sb = new StringBuilder();
        sb.append("*START_MARKER*");
        for (int i = 0; i<800_000; i++)
        {
            sb.append("X");
        }
        sb.append("*END_MARKER*");

        LONG_DATA = sb.toString();
    }

    @GET
    @Path("/getDataLength")
    public int getDataLength()
    {
        return LONG_DATA.length();
    }

    @GET
    @Path("/getData")
    public String getData()
    {
        return LONG_DATA;
    }
}
