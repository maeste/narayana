import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Test;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.jta.xa.XidImple;

public class RecoverOracle {

	@Test
	public void recoverOracle() throws SQLException, XAException {
		String hostName = "ol6-112.localdomain";
		String userName = "audit_test";
		String password = "password";
		String recoveryUserName = "crashrec";
		String recoveryPassword = "crashrec";
		String databaseName = "orcl";

		// dataSource.setServerName("db04.mw.lab.eng.bos.redhat.com");
		// dataSource.setDatabaseName("qaora11");

		{
			oracle.jdbc.xa.client.OracleXADataSource dataSource = new oracle.jdbc.xa.client.OracleXADataSource();
			dataSource.setDriverType("thin");
			dataSource.setPortNumber(1521);
			dataSource.setNetworkProtocol("tcp");
			dataSource.setUser(userName);
			dataSource.setPassword(password);
			dataSource.setServerName(hostName);
			dataSource.setDatabaseName(databaseName);

			XAConnection xaConnection = dataSource.getXAConnection();
			XAResource xaResource2 = xaConnection.getXAResource();
			XidImple xid = new XidImple(new Uid(), true, 1);
			xaResource2.start(xid, XAResource.TMNOFLAGS);
			xaConnection.getConnection().createStatement()
					.execute("INSERT INTO test_tab (id) VALUES (1)");
			xaResource2.prepare(xid);

			// Connection connection = dataSource.getConnection();
			// connection
			// .createStatement()
			// .execute(
			// "Insert into sys.pending_trans$ (LOCAL_TRAN_ID,GLOBAL_TRAN_FMT,GLOBAL_ORACLE_ID,GLOBAL_FOREIGN_ID,TRAN_COMMENT,STATE,STATUS,HEURISTIC_DFLT,SESSION_VECTOR,RECO_VECTOR,TYPE#,FAIL_TIME,HEURISTIC_TIME,RECO_TIME,TOP_DB_USER,TOP_OS_USER,TOP_OS_HOST,TOP_OS_TERMINAL,GLOBAL_COMMIT#,SPARE1,SPARE2,SPARE3,SPARE4) values ('8.20.23482',131077,null,'00000000000000000000FFFFC0A82DCA22C9BD23502E28340000247231',null,'prepared','P','','00000002','00000000',0,to_timestamp('07-JUL-13 16:56:43','DD-MON-RR HH24.MI.SSXFF'),null,to_timestamp('07-JUL-13 04:48:35','DD-MON-RR HH24.MI.SSXFF'),null,'Gupta.Mayank','mymachine-W7','unknown','64104005',null,null,null,null)");

			// Runtime.getRuntime().halt(-1);
		}

		{
			oracle.jdbc.xa.client.OracleXADataSource dataSource = new oracle.jdbc.xa.client.OracleXADataSource();
			dataSource.setDriverType("thin");
			dataSource.setPortNumber(1521);
			dataSource.setNetworkProtocol("tcp");
			dataSource.setUser(recoveryUserName);
			dataSource.setPassword(recoveryPassword);
			dataSource.setServerName(hostName);
			dataSource.setDatabaseName(databaseName);

			XAResource xaResource = dataSource.getXAConnection()
					.getXAResource();
			Xid[] recover = xaResource.recover(XAResource.TMSTARTRSCAN);
			int rolledBack = 0;
			for (int i = 0; i < recover.length; i++) {
				try {
					System.out.println("Rolling back: "
							+ new XidImple(recover[i]));
					xaResource.rollback(recover[i]);
					System.out.println("Rolled back");
					rolledBack++;
				} catch (XAException e) {
					e.printStackTrace();
				}
			}
			xaResource.recover(XAResource.TMENDRSCAN);
			int expected = 1;
			if (expected >= 0) {
				assertTrue("Rolled back: " + rolledBack, rolledBack == expected);
			}
		}
	}
}
