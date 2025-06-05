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

public class SQLExample3 {
    public List<AccountDTO> findAccountsById(String id) {
        String jql = String.format("from Account where id = '%s'", id);
        EntityManager em = emfactory.createEntityManager();
        // ruleid: formatted-sql-string
        TypedQuery<Account> q = em.createQuery(jql, Account.class);
        return q.getResultList()
        .stream()
        .map(this::toAccountDTO)
        .collect(Collectors.toList());
    }
}
```