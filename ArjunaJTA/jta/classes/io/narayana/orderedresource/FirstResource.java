/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package io.narayana.orderedresource;

import javax.transaction.xa.Xid;

/**
 * This interface allows 1PC resources to sequence themselves in the correct
 * order for the transaction manager to support avoiding requiring a heuristic
 * outcome during second phase failure.
 */
public interface FirstResource {

	/**
	 * This will ensure that the resource manager has included a globally unique
	 * identifier for its branch in the same scope as the users business logic.
	 * This xid must be persisted in the same transactional scope as the users
	 * business logic.
	 * 
	 * It will be called during the prepare phase of the 2PC transaction prior
	 * to the transaction manager storing its transaction log.
	 * 
	 * @param xid
	 *            The transaction branch for this resource manager
	 * @throws Exception
	 */
	public void associateBranchIdentifier(Xid xid) throws Exception;
}
