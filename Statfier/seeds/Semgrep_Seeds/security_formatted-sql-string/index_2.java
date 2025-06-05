```java
// cf. https://www.baeldung.com/sql-injection

package sql.injection;

import com.biz.org.AccountDTO;
import com.biz.org.DB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;

public class SqlExample {
    public void findAccountsById(String id) throws SQLException {
        String sql = "SELECT * "
            + "FROM accounts WHERE id = '"
            + id
            + "'";
        Connection c = DB.getConnection();
        // ruleid:formatted-sql-string
        ResultSet rs = c.createStatement().executeQuery(sql);
    }
}
```