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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import oracle.jdbc.pool.OracleConnectionPoolDataSource;
import oracle.jdbc.xa.client.OracleXADataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.xa.PGXADataSource;

import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.ibm.db2.jcc.DB2ConnectionPoolDataSource;
import com.ibm.db2.jcc.DB2DataSource;
import com.ibm.db2.jcc.DB2XADataSource;
import com.microsoft.sqlserver.jdbc.SQLServerConnectionPoolDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerXADataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import com.sybase.jdbc3.jdbc.SybConnectionPoolDataSource;
import com.sybase.jdbc3.jdbc.SybDataSource;
import com.sybase.jdbc3.jdbc.SybXADataSource;

public class PerformanceTestCommitMarkableResource extends
		TestCommitMarkableResourceBase {
	private int threadCount = 10;
	private int iterationCount = 10;
	private int waiting;
	private boolean go;
	private final Object waitLock = new Object();
	private AtomicInteger totalExecuted = new AtomicInteger();

	private String dbType = System.getProperty("dbType", "h2");

	@Before
	public void setUp() {
		synchronized (waitLock) {
			waiting = 0;
		}
		synchronized (this) {
			go = false;
		}
	}

	private void checkSize(String string, Statement statement, int expected)
			throws SQLException {
		ResultSet result = statement.executeQuery("select count(*) from "
				+ string);
		result.next();
		int actual = result.getInt(1);
		result.close();
		assertEquals(expected, actual);
	}

	// @org.junit.Ignore
	@Test
	public void testCommitMarkableResource() throws Exception {
		System.out.println("testCommitMarkableResource: " + new Date());

		ConnectionPoolDataSource dataSource = null;
		DataSource recoveryDataSource = null;

		// General options
		// BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
		// .setPerformImmediateCleanupOfCommitMarkableResourceBranches(true);
		// BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
		// .setNotifyCommitMarkableRecoveryModuleOfCompleteBranches(
		// false);

		if (dbType.equals("oracle")) {
			// ORA-01795: maximum number of expressions in a list is 1000
			BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
					.setCommitMarkableResourceRecordDeleteBatchSize(1000);
			dataSource = new OracleConnectionPoolDataSource();
			((OracleConnectionPoolDataSource) dataSource).setDriverType("thin");
			((OracleConnectionPoolDataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((OracleConnectionPoolDataSource) dataSource).setPortNumber(1521);
			((OracleConnectionPoolDataSource) dataSource)
					.setNetworkProtocol("tcp");
			((OracleConnectionPoolDataSource) dataSource)
					.setDatabaseName("orcl");
			((OracleConnectionPoolDataSource) dataSource).setUser("dtf11");
			((OracleConnectionPoolDataSource) dataSource).setPassword("dtf11");
			recoveryDataSource = (DataSource) dataSource;
		} else if (dbType.equals("sybase")) {

			// wide table support?
			BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
					.setCommitMarkableResourceRecordDeleteBatchSize(2000);
			dataSource = new SybConnectionPoolDataSource();
			((SybConnectionPoolDataSource) dataSource).setPortNumber(5000);
			((SybConnectionPoolDataSource) dataSource).setUser("sa");
			((SybConnectionPoolDataSource) dataSource).setPassword("sybase");
			((SybConnectionPoolDataSource) dataSource)
					.setServerName("192.168.1.5");
			((SybConnectionPoolDataSource) dataSource)
					.setDatabaseName("LOCALHOST");
			recoveryDataSource = new SybDataSource();
			((SybDataSource) recoveryDataSource).setPortNumber(5000);
			((SybDataSource) recoveryDataSource).setUser("sa");
			((SybDataSource) recoveryDataSource).setPassword("sybase");
			((SybDataSource) recoveryDataSource).setServerName("192.168.1.5");
			((SybDataSource) recoveryDataSource).setDatabaseName("LOCALHOST");
		} else if (dbType.equals("h2")) {

			// Smaller batch size as H2 uses a hashtable in the delete which is
			// inefficent for bytearray clause
			BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
					.setCommitMarkableResourceRecordDeleteBatchSize(100);
			dataSource = new JdbcDataSource();
			((JdbcDataSource) dataSource)
					.setURL("jdbc:h2:mem:JBTMDB;MVCC=TRUE;DB_CLOSE_DELAY=-1");
			recoveryDataSource = ((JdbcDataSource) dataSource);
		} else if (dbType.equals("postgres")) {

			dataSource = new PGConnectionPoolDataSource();
			((PGConnectionPoolDataSource) dataSource).setPortNumber(5432);
			((PGConnectionPoolDataSource) dataSource).setUser("dtf11");
			((PGConnectionPoolDataSource) dataSource).setPassword("dtf11");
			((PGConnectionPoolDataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((PGConnectionPoolDataSource) dataSource)
					.setDatabaseName("jbossts");
			recoveryDataSource = new PGSimpleDataSource();
			((PGSimpleDataSource) recoveryDataSource).setPortNumber(5432);
			((PGSimpleDataSource) recoveryDataSource).setUser("dtf11");
			((PGSimpleDataSource) recoveryDataSource).setPassword("dtf11");
			((PGSimpleDataSource) recoveryDataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((PGSimpleDataSource) recoveryDataSource)
					.setDatabaseName("jbossts");
		} else if (dbType.equals("mysql")) {
			dataSource = new MysqlConnectionPoolDataSource();
			// need paranoid as otherwise it sends a connection change user
			((MysqlConnectionPoolDataSource) dataSource)
					.setUrl("jdbc:mysql://tywin.buildnet.ncl.jboss.com:3306/jbossts?user=dtf11&password=dtf11&paranoid=true");
			recoveryDataSource = (DataSource) dataSource;
		} else if (dbType.equals("db2")) {
			dataSource = new DB2ConnectionPoolDataSource();

			((DB2ConnectionPoolDataSource) dataSource).setPortNumber(50001);
			((DB2ConnectionPoolDataSource) dataSource).setUser("db2");
			((DB2ConnectionPoolDataSource) dataSource).setPassword("db2");
			// dataSource.setURL("jdbc:arjuna:ibmdb2");
			((DB2ConnectionPoolDataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((DB2ConnectionPoolDataSource) dataSource).setDatabaseName("BTDB1");
			((DB2ConnectionPoolDataSource) dataSource).setDriverType(4);
			recoveryDataSource = new DB2DataSource();
			((DB2DataSource) recoveryDataSource).setPortNumber(50001);
			((DB2DataSource) recoveryDataSource).setUser("db2");
			((DB2DataSource) recoveryDataSource).setPassword("db2");
			// dataSource.setURL("jdbc:arjuna:ibmdb2");
			((DB2DataSource) recoveryDataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((DB2DataSource) recoveryDataSource).setDatabaseName("BTDB1");
			((DB2DataSource) recoveryDataSource).setDriverType(4);
		} else if (dbType.equals("sqlserver")) {
			dataSource = new SQLServerConnectionPoolDataSource();
			((SQLServerConnectionPoolDataSource) dataSource)
					.setPortNumber(3918);
			((SQLServerConnectionPoolDataSource) dataSource)
					.setUser("dballo01");
			((SQLServerConnectionPoolDataSource) dataSource)
					.setPassword("dballo01");
			// dataSource.setURL("jdbc:arjuna:sqlserver_jndi");
			((SQLServerConnectionPoolDataSource) dataSource)
					.setServerName("dev30.mw.lab.eng.bos.redhat.com");
			((SQLServerConnectionPoolDataSource) dataSource)
					.setDatabaseName("dballo01");
			((SQLServerConnectionPoolDataSource) dataSource)
					.setSendStringParametersAsUnicode(false);
			recoveryDataSource = (DataSource) dataSource;
		}
		PooledConnection pooledConnection = dataSource.getPooledConnection();
		Utils.createTables(pooledConnection.getConnection());
		pooledConnection.close();

		doTest(new Handler(dataSource, recoveryDataSource));
	}

	// @org.junit.Ignore
	@Test
	public void testXAResource() throws Exception {
		System.out.println("testXAResource: " + new Date());

		XADataSource dataSource = null;

		if (dbType.equals("oracle")) {
			dataSource = new OracleXADataSource();
			((OracleXADataSource) dataSource).setDriverType("thin");
			((OracleXADataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((OracleXADataSource) dataSource).setPortNumber(1521);
			((OracleXADataSource) dataSource).setNetworkProtocol("tcp");
			((OracleXADataSource) dataSource).setDatabaseName("orcl");
			((OracleXADataSource) dataSource).setUser("dtf11");
			((OracleXADataSource) dataSource).setPassword("dtf11");
		} else if (dbType.equals("sybase")) {
			dataSource = new SybXADataSource();
			((SybXADataSource) dataSource).setPortNumber(5000);
			((SybXADataSource) dataSource).setUser("sa");
			((SybXADataSource) dataSource).setPassword("sybase");
			((SybXADataSource) dataSource).setServerName("192.168.1.5");
			((SybXADataSource) dataSource).setDatabaseName("LOCALHOST");
		} else if (dbType.equals("h2")) {
			dataSource = new org.h2.jdbcx.JdbcDataSource();
			((JdbcDataSource) dataSource)
					.setURL("jdbc:h2:mem:JBTMDB2;MVCC=TRUE;DB_CLOSE_DELAY=-1");
		} else if (dbType.equals("postgres")) {

			dataSource = new PGXADataSource();
			((PGXADataSource) dataSource).setPortNumber(5432);
			((PGXADataSource) dataSource).setUser("dtf11");
			((PGXADataSource) dataSource).setPassword("dtf11");
			((PGXADataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((PGXADataSource) dataSource).setDatabaseName("jbossts");
		} else if (dbType.equals("mysql")) {

			dataSource = new MysqlXADataSource();
			((MysqlXADataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((MysqlXADataSource) dataSource).setPortNumber(3306);
			((MysqlXADataSource) dataSource).setDatabaseName("jbossts");
			((MysqlXADataSource) dataSource).setUser("dtf11");
			((MysqlXADataSource) dataSource).setPassword("dtf11");
		} else if (dbType.equals("db2")) {
			dataSource = new com.ibm.db2.jcc.DB2XADataSource();

			((DB2XADataSource) dataSource).setPortNumber(50001);
			((DB2XADataSource) dataSource).setUser("db2");
			((DB2XADataSource) dataSource).setPassword("db2");
			// dataSource.setURL("jdbc:arjuna:ibmdb2");
			((DB2XADataSource) dataSource)
					.setServerName("tywin.buildnet.ncl.jboss.com");
			((DB2XADataSource) dataSource).setDatabaseName("BTDB1");
			((DB2XADataSource) dataSource).setDriverType(4);
		} else if (dbType.equals("sqlserver")) {
			dataSource = new com.microsoft.sqlserver.jdbc.SQLServerXADataSource();
			((SQLServerXADataSource) dataSource).setPortNumber(3918);
			((SQLServerXADataSource) dataSource).setUser("crashrec");
			((SQLServerXADataSource) dataSource).setPassword("crashrec");
			// dataSource.setURL("jdbc:arjuna:sqlserver_jndi");
			((SQLServerXADataSource) dataSource)
					.setServerName("dev30.mw.lab.eng.bos.redhat.com");
			((SQLServerXADataSource) dataSource).setDatabaseName("crashrec");
			((SQLServerXADataSource) dataSource)
					.setSendStringParametersAsUnicode(false);

		}

		Utils.createTables(dataSource);

		doTest(new Handler(dataSource));
	}

	public void doTest(final Handler xaHandler) throws Exception {

		// Test code
		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(new Runnable() {

				public void run() {
					synchronized (waitLock) {
						waiting++;
						waitLock.notify();
					}
					synchronized (PerformanceTestCommitMarkableResource.this) {
						while (!go) {
							try {
								PerformanceTestCommitMarkableResource.this
										.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
								return;
							}
						}
					}

					int success = 0;
					for (int i = 0; i < iterationCount; i++) {
						javax.transaction.TransactionManager tm = com.arjuna.ats.jta.TransactionManager
								.transactionManager();
						try {
							tm.begin();
							tm.getTransaction().enlistResource(
									new DummyXAResource());

							xaHandler.enlistResource(tm.getTransaction());

							tm.commit();
							// System.out.println("done");
							success++;
						} catch (SQLException e) {
							System.err.println("boom");
							e.printStackTrace();
							if (e.getCause() != null) {
								e.getCause().printStackTrace();
							}
							SQLException nextException = e.getNextException();
							while (nextException != null) {
								nextException.printStackTrace();
								nextException = nextException
										.getNextException();
							}
							Throwable[] suppressed = e.getSuppressed();
							for (int j = 0; j < suppressed.length; j++) {
								suppressed[j].printStackTrace();
							}
							try {
								tm.rollback();
							} catch (IllegalStateException | SecurityException
									| SystemException e1) {
								e1.printStackTrace();
								fail("Problem with transaction");
							}
						} catch (NotSupportedException | SystemException
								| IllegalStateException | RollbackException
								| SecurityException | HeuristicMixedException
								| HeuristicRollbackException e) {
							e.printStackTrace();
							fail("Problem with transaction");
						}
					}

					try {
						xaHandler.finishWork();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					totalExecuted.addAndGet(success);
				}
			});
			threads[i].start();
		}

		synchronized (waitLock) {
			while (waiting < threads.length) {
				waitLock.wait();
			}
		}
		long startTime = -1;
		synchronized (PerformanceTestCommitMarkableResource.this) {
			go = true;
			PerformanceTestCommitMarkableResource.this.notifyAll();
			startTime = System.currentTimeMillis();
		}

		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}

		long endTime = System.currentTimeMillis();

		System.out.println(new Date() + "  Number of transactions: "
				+ totalExecuted.intValue());

		long additionalCleanuptime = xaHandler.postRunCleanup();

		long timeInMillis = (endTime - startTime) + additionalCleanuptime;
		System.out.println("  Total time millis: " + timeInMillis);
		System.out.println("  Average transaction time: " + timeInMillis
				/ totalExecuted.intValue());
		System.out
				.println("  Transactions per second: "
						+ Math.round((totalExecuted.intValue() / (timeInMillis / 1000d))));

		xaHandler.checkFooSize();
	}

	private class Handler {

		private ThreadLocal<XAConnection> xaConnection = new ThreadLocal<XAConnection>();
		private XADataSource xaDataSource;
		private ConnectionPoolDataSource dataSource;
		private ThreadLocal<Connection> connection = new ThreadLocal<Connection>();
		private ThreadLocal<PooledConnection> pooledConnection = new ThreadLocal<PooledConnection>();
		private Object recoveryDataSource;

		public Handler(XADataSource xaDataSource) {
			this.xaDataSource = xaDataSource;
		}

		public Handler(ConnectionPoolDataSource dataSource,
				DataSource recoveryDataSource) {
			this.dataSource = dataSource;
			this.recoveryDataSource = recoveryDataSource;
		}

		private void enlistResource(Transaction transaction)
				throws SQLException, IllegalStateException, RollbackException,
				SystemException {
			if (xaDataSource != null) {
				if (this.xaConnection.get() == null) {
					this.xaConnection.set(xaDataSource.getXAConnection());
					this.connection.set(xaConnection.get().getConnection());
				}
				XAResource xaResource = xaConnection.get().getXAResource();
				transaction.enlistResource(xaResource);

				Statement createStatement = connection.get().createStatement();
				createStatement.execute("INSERT INTO "
						+ Utils.getXAFooTableName() + " (bar) VALUES (1)");
				createStatement.close();
			} else {
				if (this.pooledConnection.get() == null) {
					this.pooledConnection.set(dataSource.getPooledConnection());
				}
				Connection connection = this.pooledConnection.get()
						.getConnection();
				connection.setAutoCommit(false);

				XAResource nonXAResource = new JDBCConnectableResource(
						connection);
				transaction.enlistResource(nonXAResource);

				Statement createStatement = connection.createStatement();
				createStatement.execute("INSERT INTO foo (bar) VALUES (1)");
				createStatement.close();
			}
		}

		private void finishWork() throws SQLException {
			if (xaConnection.get() != null) {
				connection.get().close();
				connection.set(null);
				xaConnection.get().close();
				xaConnection.set(null);
			}
			if (pooledConnection.get() != null) {
				pooledConnection.get().close();
			}
		}

		public long postRunCleanup() throws NamingException, SQLException,
				ObjectStoreException {
			if (dataSource != null) {
				PooledConnection pooledConnection = null;
				Connection connection = null;
				try {
					pooledConnection = dataSource.getPooledConnection();
					connection = pooledConnection.getConnection();
					Statement statement = connection.createStatement();
					CommitMarkableResourceRecordRecoveryModule crrrm = null;
					RecoveryManager recMan = RecoveryManager.manager();
					Vector recoveryModules = recMan.getModules();
					if (recoveryModules != null) {
						Enumeration modules = recoveryModules.elements();

						while (modules.hasMoreElements()) {
							RecoveryModule m = (RecoveryModule) modules
									.nextElement();

							if (m instanceof CommitMarkableResourceRecordRecoveryModule) {
								crrrm = (CommitMarkableResourceRecordRecoveryModule) m;
								break;
							}
						}
					}
					int expectedReapableRecords = BeanPopulator
							.getDefaultInstance(JTAEnvironmentBean.class)
							.isPerformImmediateCleanupOfCommitMarkableResourceBranches() ? 0
							: threadCount * iterationCount;
					checkSize("xids", statement, expectedReapableRecords);
					if (expectedReapableRecords > 0) {
						// The recovery module has to perform lookups
						new InitialContext().rebind("commitmarkableresource",
								recoveryDataSource);
						long startTime = System.currentTimeMillis();
						crrrm.periodicWorkFirstPass();
						crrrm.periodicWorkSecondPass();
						long endTime = System.currentTimeMillis();

						checkSize("xids", statement, 0);
						statement.close();

						System.out.println("  Total cleanup time: "
								+ (endTime - startTime)
								+ " Average cleanup time: "
								+ (endTime - startTime)
								/ expectedReapableRecords);

						return endTime - startTime;
					} else {
						statement.close();
					}
				} finally {
					if (connection != null) {
						connection.close();
					}

					if (pooledConnection != null) {
						pooledConnection.close();
					}
				}
			}
			return 0;
		}

		public void checkFooSize() throws SQLException {
			Connection connection = null;
			XAConnection xaConnection = null;
			PooledConnection pooledConnection = null;
			String tableToCheck = null;
			if (dataSource != null) {
				pooledConnection = dataSource.getPooledConnection();
				connection = pooledConnection.getConnection();
				tableToCheck = "foo";
			} else {
				xaConnection = xaDataSource.getXAConnection();
				connection = xaConnection.getConnection();
				tableToCheck = Utils.getXAFooTableName();
			}
			Statement statement = connection.createStatement();
			checkSize(tableToCheck, statement, threadCount * iterationCount);
			statement.close();
			connection.close();
			if (xaConnection != null) {
				xaConnection.close();
			}
			if (pooledConnection != null) {
				pooledConnection.close();
			}
		}
	}
}
