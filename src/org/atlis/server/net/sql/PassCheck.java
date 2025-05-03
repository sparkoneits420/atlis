package org.atlis.server.net.sql;

import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;

public class PassCheck {

    public static boolean checkLogin(String username, String password) {
        try {
            URL url = new URL("https://atlis.online/pcheck2.php");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String urlParams = "username=" + URLEncoder.encode(username, "UTF-8") +
                               "&password=" + URLEncoder.encode(password, "UTF-8");

            try (OutputStream os = con.getOutputStream()) {
                os.write(urlParams.getBytes());
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String response = in.readLine();
            in.close();

            return "1".equals(response);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
