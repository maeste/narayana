package io.narayana.orderedresource.example;

import java.sql.Connection;

import javax.sql.XAConnection;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Test;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;
import com.sybase.jdbc4.jdbc.SybDataSource;
import com.sybase.jdbc4.jdbc.SybXADataSource;

public class TestNonXAJDBCResource {
	@Test
	public void test() throws Exception {
		SybDataSource dataSource = new SybDataSource();
		dataSource.setPortNumber(5000);
		dataSource.setUser("sa");
		dataSource.setPassword("sybase");
		dataSource.setServerName("172.17.130.209");
		dataSource.setDatabaseName("LOCALHOST");

		SybXADataSource xaDataSource = new SybXADataSource();
		xaDataSource.setPortNumber(5000);
		xaDataSource.setUser("sa");
		xaDataSource.setPassword("sybase");
		xaDataSource.setServerName("172.17.130.209");
		xaDataSource.setDatabaseName("LOCALHOST");

		Transaction transaction = new TransactionImple(-1);

		XAConnection xaConnection = xaDataSource.getXAConnection();
		XAResource xaResource = xaConnection.getXAResource();
		transaction.enlistResource(xaResource);
		Connection xaJDBCConnection = xaConnection.getConnection();

		Connection localJDBCConnection = dataSource.getConnection();
		XAResource nonXAResource = new NonXAJDBCResource(localJDBCConnection);
		transaction.enlistResource(nonXAResource);
		localJDBCConnection.setAutoCommit(false);

		xaJDBCConnection.createStatement().execute(
				"INSERT INTO foo (bar) VALUES (2)");

		localJDBCConnection.createStatement().execute(
				"INSERT INTO foo (bar) VALUES (1)");

		transaction.commit();

		// Normally this would be done in the first phase of recovery
		NonXAJDBCResourceRecoveryHelper recoveryHelper = new NonXAJDBCResourceRecoveryHelper(
				dataSource);
		Xid[] recover = recoveryHelper.listCommittedBranches();

		// // This code is part of the test to ensure the resource manager has
		// // saved the xid
		// boolean found = false;
		// for (Xid xid : recover) {
		// if (((XidImple) xid).equals(((NonXAJDBCResource) nonXAResource)
		// .getStartedXid())) {
		// found = true;
		// }
		// }
		// assertTrue(found);
		//
		// // This would normally be done in the second phase of recovery or by
		// a
		// // garbage collection routine
		// recoveryHelper.garbageCollect(recover);
		//
		// // This code is part of the test, it ensures the resource manager has
		// // deleted
		// // the requested xids
		// found = false;
		// recover = recoveryHelper.listCommittedBranches();
		// for (Xid xid : recover) {
		// if (((XidImple) xid).equals(((NonXAJDBCResource) nonXAResource)
		// .getStartedXid())) {
		// found = true;
		// }
		// }
		// assertFalse(found);
		//
		// System.out.println("Done");
	}

}
