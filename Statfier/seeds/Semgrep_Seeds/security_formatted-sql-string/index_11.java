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

public class tableConcatStatements {
    public void tableConcat() {
        // ok:formatted-sql-string
        stmt.execute("DROP TABLE " + tableName);
        stmt.execute(String.format("CREATE TABLE %s", tableName));
    }
}
```