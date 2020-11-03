import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DBAuthService implements AuthService{
    private static final Logger LOGGER = LogManager.getLogger(DBAuthService.class.getName());
    Connection connection ;

    @Override
    public boolean start() {
        LOGGER.info("Start auth service");
        try {
            Class.forName("org.sqlite.JDBC");

            Properties properties = new Properties();
            properties.load(this.getClass().getResourceAsStream("config.properties"));

            String url = properties.getProperty("db.url");
            String login = properties.getProperty("db.user");
            String password = properties.getProperty("db.password");

            this.connection = DriverManager.getConnection(url, login, password);
        } catch (ClassNotFoundException | SQLException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        LOGGER.info("Stop auth service");
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public boolean checkUser(String login, String password) {
        String query = String.format("select * from users where Login = '%s' and  Password = '%s'", login, password);
        LOGGER.debug("get nick by login");
        String message = String.format("query: %s", query.replace(password, "*****"));
        LOGGER.debug(message);
        try(Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

}
