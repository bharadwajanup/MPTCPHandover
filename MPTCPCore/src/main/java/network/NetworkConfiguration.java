package network;

import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created as part of the class project for Mobile Computing
 */
public class NetworkConfiguration {

    private static String resourceFile = "config.properties";

    public static String getProperty(String name, String def) {
        ClassLoader loader = NetworkConfiguration.class.getClassLoader();
//        URL resourceFileURL = loader.getResource(resourceFile);
        InputStream fs = loader.getResourceAsStream(resourceFile);

//        System.out.println(resourceFileURL);
//        String path = null;
//        try {
//            path = URLDecoder.decode(resourceFileURL.getFile(),"UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

//        URI resourceFileURI = null;
//        try {
//            resourceFileURI = new URI(resourceFileURL.getFile());
//            System.out.println(resourceFileURI);
//
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }

//        File configFile = new File();
        FileReader reader = null;
        try {
//            reader = new FileReader(configFile);
            Properties networkProperties = new Properties();
            networkProperties.load(fs);
//            networkProperties.load(reader);
            String val = networkProperties.getProperty(name);
            if (val == null)
                return def;
//            reader.close();
            return val;

        } catch (Exception e) {
            System.out.println("Exception while Reading properties file");
            e.printStackTrace();
        }

        return def;

    }


}
