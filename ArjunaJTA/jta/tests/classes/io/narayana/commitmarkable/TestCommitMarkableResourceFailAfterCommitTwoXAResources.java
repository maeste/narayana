/*
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2013
 * @author JBoss Inc.
 */
package io.narayana.commitmarkable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Enumeration;
import java.util.Vector;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.xa.Xid;

import org.h2.jdbcx.JdbcDataSource;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.byteman.rule.exception.ExecuteException;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule;

@RunWith(BMUnitRunner.class)
public class TestCommitMarkableResourceFailAfterCommitTwoXAResources extends
		FailAfterCommitBase {

	private JDBCConnectableResource nonXAResource;
	private boolean failed = false;
	private SimpleXAResource xaResource;
	private SimpleXAResource2 xaResource2;

	@Test
	@BMScript("commitMarkableResourceFailAfterCommit")
	public void testFailAfterCommitH2() throws Exception {
		final JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:JBTMDB;MVCC=TRUE");
	}

	protected void doTest(final DataSource dataSource) throws Exception {
		// Test code
		Utils.createTables(dataSource.getConnection());

		// We can't just instantiate one as we need to be using the
		// same one as
		// the transaction
		// manager would have used to mark the transaction for GC
		CommitMarkableResourceRecordRecoveryModule recoveryModule = null;
		Vector recoveryModules = manager.getModules();
		if (recoveryModules != null) {
			Enumeration modules = recoveryModules.elements();

			while (modules.hasMoreElements()) {
				RecoveryModule m = (RecoveryModule) modules.nextElement();

				if (m instanceof CommitMarkableResourceRecordRecoveryModule) {
					recoveryModule = (CommitMarkableResourceRecordRecoveryModule) m;
				}
			}
		}
		// final Object o = new Object();
		// synchronized (o) {

		Thread foo = new Thread(new Runnable() {

			public void run() {

				try {
					javax.transaction.TransactionManager tm = com.arjuna.ats.jta.TransactionManager
							.transactionManager();

					tm.begin();

					Connection localJDBCConnection = dataSource.getConnection();
					localJDBCConnection.setAutoCommit(false);
					nonXAResource = new JDBCConnectableResource(
							localJDBCConnection);
					tm.getTransaction().enlistResource(nonXAResource);

					xaResource = new SimpleXAResource();
					tm.getTransaction().enlistResource(xaResource);
					xaResource2 = new SimpleXAResource2();
					tm.getTransaction().enlistResource(xaResource2);

					localJDBCConnection.createStatement().execute(
							"INSERT INTO foo (bar) VALUES (1)");

					tm.commit();
				} catch (ExecuteException t) {
				} catch (Exception e) {
					e.printStackTrace();
					failed = true;
				} catch (Error e) {

				}
			}
		});
		foo.start();
		foo.join();

		assertFalse(failed);

		Xid committed = ((JDBCConnectableResource) nonXAResource)
				.getStartedXid();
		assertNotNull(committed);
		// The recovery module has to perform lookups
		new InitialContext().rebind("commitmarkableresource", dataSource);
		// Check if the item is still in the db
		recoveryModule.periodicWorkFirstPass();
		assertTrue(recoveryModule.wasCommitted("commitmarkableresource",
				committed));
		recoveryModule.periodicWorkSecondPass();

		// Now we need to correctly complete the transaction
		manager.scan();

		// Make sure the item is no longer in the DB, it will need two scans as
		// second phase of the CommitMarkableResourceRecoveryModule will have
		// executed before the AtomicActionRecoveryModule has been able to GC
		// it
		assertTrue(recoveryModule.wasCommitted("commitmarkableresource",
				committed));
		recoveryModule.periodicWorkFirstPass();
		recoveryModule.periodicWorkSecondPass();
		assertFalse(recoveryModule.wasCommitted("commitmarkableresource",
				committed));

		assertTrue(xaResource.wasCommitted());
		assertFalse(xaResource.wasRolledback());

		assertTrue(xaResource2.commitCalled());
		assertFalse(xaResource2.rollbackCalled());
	}
}