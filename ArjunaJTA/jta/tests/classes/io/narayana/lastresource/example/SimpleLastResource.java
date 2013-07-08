package io.narayana.lastresource.example;

import io.narayana.lastresource.LastResource;
import io.narayana.lastresource.recovery.LastResourceStatus;
import io.narayana.lastresource.recovery.LastResourceStatusDeserializer;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import oracle.jdbc.pool.OracleDataSource;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;

public class SimpleLastResource implements LastResource, XAResource {
	private OracleDataSource dataSource;
	private Connection connection;

	public static void main(String[] args) throws Exception {
		Transaction transaction = new TransactionImple(60);
		SimpleLastResource srd = new SimpleLastResource();
		transaction.enlistResource(srd);
		srd.doLogic();
		// This would normally be done by the transaction manager during prepare
		Externalizable recoveryInformation = srd.getRecoveryInformation();
		transaction.commit();

		LastResourceStatusDeserializer lrsDeserializer = new SimpleLastResourceDeserializer();
		if (lrsDeserializer.canDeserialize(SimpleLastResource.class.getName())) {
			ByteArrayOutputStream s = new ByteArrayOutputStream();
			ObjectOutputStream o = new ObjectOutputStream(s);
			recoveryInformation.writeExternal(o);
			byte[] byteArray = s.toByteArray();
			LastResourceStatus deserialize = lrsDeserializer
					.deserialize(byteArray);
			if (!deserialize.wasCommitted()) {
				throw new RuntimeException("was not recovered");
			}
		}
		System.out.println("Done");
	}

	public SimpleLastResource() throws SQLException {
		dataSource = new oracle.jdbc.pool.OracleDataSource();
		dataSource.setDriverType("thin");
		dataSource.setServerName("ol6-112.localdomain");
		dataSource.setPortNumber(1521);
		dataSource.setNetworkProtocol("tcp");
		dataSource.setDatabaseName("orcl");
		dataSource.setUser("audit_test");
		dataSource.setPassword("password");

		connection = dataSource.getConnection();
		connection.setAutoCommit(false);
	}

	public void doLogic() throws SQLException {
		connection.createStatement().execute(
				"INSERT INTO test_tab (id) VALUES (1)");
	}

	@Override
	public Externalizable getRecoveryInformation() {
		try {
			ResultSet rs = null;
			boolean sleep = false;
			do {
				if (sleep) {
					System.out.println("Required retry");
					Thread.currentThread().sleep(1000);
				}
				rs = connection
						.createStatement()
						.executeQuery(
								"SELECT xid from v$TRANSACTION where XIDUSN = (select regexp_substr(dbms_transaction.local_transaction_id, ('^[0-9]+')) from dual) and XIDSLOT = (select substr (regexp_substr(dbms_transaction.local_transaction_id,'.[0-9]+'), 2) from dual) and XIDSQN = (select regexp_substr(dbms_transaction.local_transaction_id, ('[0-9]+$')) from dual)");
				sleep = true;
			} while (!rs.next());
			return new SimpleLastResourceRecoveryInformation(rs.getString(1));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void commit(Xid arg0, boolean arg1) throws XAException {
		try {
			connection.commit();
		} catch (SQLException e) {
			throw new XAException(e.getMessage());
		}
	}

	@Override
	public void rollback(Xid arg0) throws XAException {
		try {
			connection.rollback();
		} catch (SQLException e) {
			throw new XAException(e.getMessage());
		}
	}

	@Override
	public void end(Xid arg0, int arg1) throws XAException {
	}

	@Override
	public void forget(Xid arg0) throws XAException {
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	@Override
	public boolean isSameRM(XAResource arg0) throws XAException {
		return false;
	}

	@Override
	public int prepare(Xid arg0) throws XAException {
		return 0;
	}

	@Override
	public Xid[] recover(int arg0) throws XAException {
		return null;
	}

	@Override
	public boolean setTransactionTimeout(int arg0) throws XAException {
		return false;
	}

	@Override
	public void start(Xid arg0, int arg1) throws XAException {
	}
}
