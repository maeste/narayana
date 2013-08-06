package io.narayana.orderedresource.example;

import io.narayana.orderedresource.FirstResource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.internal.jta.xa.XID;
import com.arjuna.ats.jta.xa.XidImple;

/**
 * This class will stores the Xid of the resource manager in a Narayana table in
 * the resource manager.
 */
public class SimpleFirstResource implements FirstResource, XAResource {

	private Connection connection;
	private Xid startedXid;

	public SimpleFirstResource(DataSource dataSource) throws SQLException {

		connection = dataSource.getConnection();
		connection.setAutoCommit(false);
	}

	@Override
	public void associateBranchIdentifier(Xid xid) throws Exception {
		PreparedStatement prepareStatement = connection
				.prepareStatement("insert into xids (xid) values (?)");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		XID toSave = ((XidImple) xid).getXID();
		dos.writeInt(toSave.formatID);
		dos.writeInt(toSave.gtrid_length);
		dos.writeInt(toSave.bqual_length);
		dos.writeInt(toSave.data.length);
		dos.write(toSave.data);
		dos.flush();
		prepareStatement.setBytes(1, baos.toByteArray());

		if (prepareStatement.executeUpdate() != 1) {
			System.err.println("Update was not successful");
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
	public int prepare(Xid arg0) throws XAException {
		return 0;
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
	public Xid[] recover(int arg0) throws XAException {
		return null;
	}

	@Override
	public boolean setTransactionTimeout(int arg0) throws XAException {
		return false;
	}

	@Override
	public void start(Xid arg0, int arg1) throws XAException {
		// Test code - please ignore
		startedXid = arg0;
	}

	/**
	 * Test code - ignore
	 */
	public Xid getStartedXid() {
		return startedXid;
	}

	/**
	 * Test code - ignore
	 */
	public Connection getConnection() {
		return connection;
	}
}
