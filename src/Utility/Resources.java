package Utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Resources {
    public static Reader getResourceAsReader(String resource) throws IOException {
            return new InputStreamReader(new FileInputStream(resource));
    }
}
