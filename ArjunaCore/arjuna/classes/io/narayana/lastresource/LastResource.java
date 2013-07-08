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

package io.narayana.lastresource;

/**
 * The LastResource interface allows 1PC resources to provide additional
 * information to the transaction manager to support avoiding requiring a
 * heuristic outcome during second phase failure.
 */
public interface LastResource {
	/**
	 * The Externatizable should contain enough information should the
	 * transaction manager crash it shall be able to reconnect to the resource
	 * manager to determine the status of the local transaction.
	 */
	public java.io.Externalizable getRecoveryInformation();
}
