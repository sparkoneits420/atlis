package org.atlis.client.ui;

import java.awt.Color;
import java.awt.Font; 
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent; 
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;  
import org.atlis.client.Screen;  
import org.atlis.common.util.Constants;
import org.atlis.common.util.Utilities;

/**
 *
 * @author smokey
 */
public class LoginScreen {

    public Screen screen;
    public Button loginButton;

    public BufferedImage BACKGROUND;
    public BufferedImage LOGO;
    public BufferedImage LOGO2;
    public BufferedImage BUTTON;
    public BufferedImage BUTTON_P;
    public BufferedImage BUTTON_HOVER;
    public BufferedImage LOGIN_INTERFACE;
    public BufferedImage TEXT_FIELD;
    public ArrayList<Component> components;
    public InputField username, password;
    public String message = "Please enter your credentials.";

    public LoginScreen(Screen screen) {
        this.screen = screen;
        initComponents();
    }

    public final void initComponents() {
        components = new ArrayList<>();
        try {
            BACKGROUND = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/background.png"));
            LOGO = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/atlislogo.png"));
            LOGO2 = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/atlislogo2.png"));
            BUTTON = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/login_button.png"));
            BUTTON_P = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/login_button_p.png"));
            BUTTON_HOVER = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/login_button_hover.png"));
            LOGIN_INTERFACE = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/login_interface.png"));
            TEXT_FIELD = ImageIO.read(new File(Constants.CACHE_DIR + "/interface/login/text_field.png"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        components.add(username = new InputField(0, 0, 171, 23) {});
        username.focused = true;
        components.add(password = new InputField(0, 0, 171, 23));
        password.passwordField = true; 
        username.setText("tater pie"); 
        password.setText(System.getProperty("DB_PASS"));
        components.add(loginButton = new Button(0, 0, BUTTON.getWidth(),
                BUTTON.getHeight(), BUTTON,
                BUTTON_HOVER, BUTTON_P, "LOGIN") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (username.getText() == null || username.getText().isBlank()) {
                    message = "Please enter a username.";
                } else if (password.getText() == null || username.getText().isBlank()) {
                    message = "Please enter a password.";
                } else if (screen.getSession().player == null) {
                    //System.out.println("wtf");
                    screen.getSession().connect(username.getText(), password.getText()); 
                }
            }
        });
    }

    public void paint(Graphics2D g) {
        if (BACKGROUND == null || TEXT_FIELD == null) {
            return;
        }
        g.drawImage(Utilities.scaleImage(BACKGROUND, screen.getWidth(),
                screen.getHeight()), 0, 0, screen);
        g.drawImage(LOGO2, (screen.getWidth() / 2) - (LOGO2.getWidth() / 2), -20, screen);
        g.drawImage(LOGIN_INTERFACE, (screen.getWidth() / 2) - (LOGIN_INTERFACE.getWidth() / 2),
                screen.getHeight() / 2 - LOGIN_INTERFACE.getHeight() / 6, screen);
        g.drawImage(TEXT_FIELD, screen.getWidth() / 2 - TEXT_FIELD.getWidth() / 2,
                (screen.getHeight() / 2 - LOGIN_INTERFACE.getHeight() / 6) + 110, screen);
        g.drawImage(TEXT_FIELD, screen.getWidth() / 2 - TEXT_FIELD.getWidth() / 2,
                (screen.getHeight() / 2 - LOGIN_INTERFACE.getHeight() / 6) + 165, screen);

        //g.drawImage(BUTTON, screen.getWidth() / 2 - BUTTON.getWidth() / 2, 
        //screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 200, screen);
        loginButton.x = screen.getWidth() / 2 - BUTTON.getWidth() / 2;
        loginButton.y = screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 200;

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Calibri", Font.BOLD, 26));
        g.setColor(Color.WHITE);
        String text = "Welcome to Atlis!";
        g.drawString(text, screen.getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2,
                screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 50);
        g.setFont(new Font("Calibri", Font.PLAIN, 16));
        text = message;
        g.drawString(text, screen.getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2,
                screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 82);
        text = "Username:";
        g.drawString(text, screen.getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2,
                screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 105);
        text = "Password:";
        g.drawString(text, screen.getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2,
                screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 160);
        username.x = (screen.getWidth() / 2 - username.width / 2);
        username.y = screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 114;
        password.x = (screen.getWidth() / 2 - password.width / 2);
        password.y = screen.getHeight() / 2 - (LOGIN_INTERFACE.getHeight() / 6) + 169;

        for (Component c : components) {
            c.paint(g);
        }
    }
 
    public void setMessage(String msg) {
        this.message = msg;
    }
}