package io.narayana.orderedresource.example;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.narayana.orderedresource.RecoveryHelper;

import java.sql.Connection;

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import org.junit.Test;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;
import com.arjuna.ats.jta.xa.XidImple;
import com.hp.mwtests.ts.jta.common.DummyXA;
import com.sybase.jdbc4.jdbc.SybDataSource;

public class TestResource {
	@Test
	public void test() throws Exception {
		SybDataSource dataSource = new SybDataSource();
		dataSource.setPortNumber(5000);
		dataSource.setUser("sa");
		dataSource.setPassword("sybase");
		dataSource.setServerName("192.168.1.5");
		dataSource.setDatabaseName("LOCALHOST");

		Transaction transaction = new TransactionImple(-1);
		SimpleFirstResource srd = new SimpleFirstResource(dataSource);
		transaction.enlistResource(srd);
		transaction.enlistResource(new DummyXA(false));

		Connection connection = srd.getConnection();

		connection.createStatement()
				.execute("INSERT INTO foo (bar) VALUES (1)");

		transaction.commit();

		// Normally this would be done in the first phase of recovery
		RecoveryHelper recoveryHelper = new SimpleRecoveryHelper(dataSource);
		Xid[] recover = recoveryHelper.listCommittedBranches();

		// This code is part of the test to ensure the resource manager has
		// saved the xid
		boolean found = false;
		for (Xid xid : recover) {
			if (((XidImple) xid).equals(srd.getStartedXid())) {
				found = true;
			}
		}
		assertTrue(found);

		// This would normally be done in the second phase of recovery or by a
		// garbage collection routine
		recoveryHelper.garbageCollect(recover);

		// This code is part of the test, it ensures the resource manager has
		// deleted
		// the requested xids
		found = false;
		recover = recoveryHelper.listCommittedBranches();
		for (Xid xid : recover) {
			if (((XidImple) xid).equals(srd.getStartedXid())) {
				found = true;
			}
		}
		assertFalse(found);

		System.out.println("Done");
	}

}
