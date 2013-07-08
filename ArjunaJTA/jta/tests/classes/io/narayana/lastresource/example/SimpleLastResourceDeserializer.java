package io.narayana.lastresource.example;

import io.narayana.lastresource.recovery.LastResourceStatus;
import io.narayana.lastresource.recovery.LastResourceStatusDeserializer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class SimpleLastResourceDeserializer implements
		LastResourceStatusDeserializer {

	@Override
	public boolean canDeserialize(String className) {
		return className.equals(SimpleLastResource.class.getName());
	}

	@Override
	public LastResourceStatus deserialize(byte[] recoveredLastResource) {
		ByteArrayInputStream bais = new ByteArrayInputStream(
				recoveredLastResource);
		try {
			ObjectInputStream ois = new ObjectInputStream(bais);

			SimpleLastResourceRecoveryInformation slrri = new SimpleLastResourceRecoveryInformation();
			slrri.readExternal(ois);

			return new SimpleLastResourceStatus(slrri.getTransactionId());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
