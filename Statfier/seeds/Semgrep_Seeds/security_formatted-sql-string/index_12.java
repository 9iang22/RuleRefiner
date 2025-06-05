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

public class FalsePositiveCase {
    private ApiClient apiClient; // imagine an ApiClient class that contains a method named execute

    public void test(String parameter) throws ApiException {
        com.squareup.okhttp.Call call = constructHttpCall(parameter); // Create OKHttp call using parameter from outside
        // ok: formatted-sql-string
        apiClient.execute(call);
        // ok: formatted-sql-string
        apiClient.execute(call);
        apiClient.run(call); // proof that 'execute' name is causing the false-positive
    }
}
```