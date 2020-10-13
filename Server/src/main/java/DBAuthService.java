import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class DBAuthService implements AuthService{
    private static final Logger logger = LogManager.getLogger(Server.class.getName());
    Connection connection ;

    @Override
    public void start() {
        logger.info("Start auth service");
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:./data/Cloud.db");
        } catch (ClassNotFoundException | SQLException e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void stop() {
        logger.info("Stop auth service");
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public boolean checkUser(String login, String password) {
        String query = String.format("select * from users where Login = '%s' and  Password = '%s'", login, password);
        logger.debug("get nick by login");
        logger.debug(String.format("query: %s", query.replace(password, "*****")));
        try(Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

}
