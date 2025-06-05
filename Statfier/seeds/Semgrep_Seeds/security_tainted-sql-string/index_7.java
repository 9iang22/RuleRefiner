import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
@RestController
@EnableAutoConfiguration
public class TestController {
    @RequestMapping(value = "/ok2", method = RequestMethod.POST, produces = "plain/text")
    ResultSet ok2(@RequestBody String name) {
        String sql = "SELECT * FROM table WHERE name = 'everyone';";
        // ok: tainted-sql-string
        System.out.println("select noise for tests using tainted name:" + name);
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:8080", "guest", "password");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.execute(sql);
        return rs;
    }
}