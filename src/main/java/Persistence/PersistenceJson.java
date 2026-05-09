package Persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// shared json settings for the ward json persistence -  utility class
public final class PersistenceJson {

    private PersistenceJson() {
    }

    public static ObjectMapper createObjectMapper() {// create the object mapper with the shared settings
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());// add support for java time types
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);// write dates as timestamps(strings)
        m.enable(SerializationFeature.INDENT_OUTPUT);// human readable pretty prints
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);// if unknown properties, ignore them (if removed fileds)
        return m;
    }
}
