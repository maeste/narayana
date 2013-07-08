package io.narayana.lastresource.example;

import io.narayana.lastresource.recovery.LastResourceStatus;

import java.sql.Connection;
import java.sql.ResultSet;

import oracle.jdbc.pool.OracleDataSource;

public class SimpleLastResourceStatus implements LastResourceStatus {

	private String transactionId;

	public SimpleLastResourceStatus(String transactionId) {
		this.transactionId = transactionId;
	}

	@Override
	public boolean wasCommitted() {
		try {
			OracleDataSource dataSource = new oracle.jdbc.pool.OracleDataSource();
			dataSource.setDriverType("thin");
			dataSource.setServerName("ol6-112.localdomain");
			dataSource.setPortNumber(1521);
			dataSource.setNetworkProtocol("tcp");
			dataSource.setDatabaseName("orcl");
			dataSource.setUser("audit_test");
			dataSource.setPassword("password");

			Connection connection = dataSource.getConnection();
			System.out
					.println("Querying for: + SELECT COMMIT_SCN from FLASHBACK_TRANSACTION_QUERY where XID = '"
							+ transactionId + "'");
			ResultSet rs = connection.createStatement().executeQuery(
					"SELECT COMMIT_SCN from FLASHBACK_TRANSACTION_QUERY where XID = '"
							+ transactionId + "'");
			return rs.next();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
