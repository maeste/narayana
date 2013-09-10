package io.narayana.orderedresource.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.internal.jta.xa.XID;
import com.arjuna.ats.jta.xa.XidImple;

/**
 * This class connects to the resource manager to query which branches have
 * committed.
 */
public class NonXAJDBCResourceRecoveryHelper {
	private Connection connection;

	public NonXAJDBCResourceRecoveryHelper(DataSource dataSource)
			throws SQLException {
		connection = dataSource.getConnection();
	}

	public Xid[] listCommittedBranches() {
		try {
			ResultSet rs = connection.createStatement().executeQuery(
					"SELECT * from xids where transactionManagerID = \'"
							+ TxControl.getXANodeName() + "\'");
			List<Xid> xids = new ArrayList<Xid>();
			while (rs.next()) {
				byte[] read = rs.getBytes(1);

				ByteArrayInputStream bais = new ByteArrayInputStream(read);
				DataInputStream dis = new DataInputStream(bais);
				XID _theXid = new XID();
				_theXid.formatID = dis.readInt();
				_theXid.gtrid_length = dis.readInt();
				_theXid.bqual_length = dis.readInt();
				int dataLength = dis.readInt();
				_theXid.data = new byte[dataLength];
				dis.read(_theXid.data, 0, dataLength);
				XidImple xid = new XidImple(_theXid);

				xids.add(xid);

				System.out.println("read: " + xid);
			}
			System.out.println("listCommittedBranches");
			return xids.toArray(new Xid[0]);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void garbageCollect(Xid[] xids) {
		try {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < xids.length; i++) {
				buffer.append("?,");
			}
			PreparedStatement prepareStatement = connection
					.prepareStatement("DELETE from xids where xid in ("
							+ buffer.substring(0, buffer.length() - 1) + ")");
			for (int i = 0; i < xids.length; i++) {
				XID toSave = ((XidImple) xids[i]).getXID();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeInt(toSave.formatID);
				dos.writeInt(toSave.gtrid_length);
				dos.writeInt(toSave.bqual_length);
				dos.writeInt(toSave.data.length);
				dos.write(toSave.data);
				dos.flush();

				prepareStatement.setBytes(i + 1, baos.toByteArray());
			}
			if (prepareStatement.executeUpdate() != xids.length) {
				System.err.println("Update was not successfull");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
