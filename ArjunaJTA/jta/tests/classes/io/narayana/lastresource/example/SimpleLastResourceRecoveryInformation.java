package io.narayana.lastresource.example;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SimpleLastResourceRecoveryInformation implements Externalizable {
	private String transactionId;

	public SimpleLastResourceRecoveryInformation(String transactionId) {
		System.out.println("The transaction is: " + transactionId);
		this.transactionId = transactionId;
	}

	public SimpleLastResourceRecoveryInformation() {

	}

	@Override
	public void readExternal(ObjectInput arg0) throws IOException,
			ClassNotFoundException {
		transactionId = (String) arg0.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput arg0) throws IOException {
		arg0.writeObject(transactionId);
	}

	public String getTransactionId() {
		return transactionId;
	}

}
