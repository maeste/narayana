/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors 
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
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
/*
 * Copyright (C) 1998, 1999, 2000, 2001,
 *
 * Arjuna Solutions Limited,
 * Newcastle upon Tyne,
 * Tyne and Wear,
 * UK.  
 *
 * $Id: ObjectStore.java 2342 2006-03-30 13:06:17Z  $
 */

package com.arjuna.ats.arjuna.objectstore;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.state.OutputObjectState;

/**
 * All transaction logs MUST implement this interface.
 *
 * @author Mark Little (mark@arjuna.com)
 * @version $Id: ObjectStore.java 2342 2006-03-30 13:06:17Z  $
 * @since JTS 1.0.
 */

public interface TxLog extends BaseStore
{
    /**
     * Remove the object's committed state.
     *
     * @param u The object to work on.
     * @param tn The type of the object to work on.
     *
     * @return <code>true</code> if no errors occurred, <code>false</code>
     * otherwise.
     */

    public boolean remove_committed (Uid u, String tn) throws ObjectStoreException;

    /**
     * Write a new copy of the object's committed state.
     *
     * @param u The object to work on.
     * @param tn The type of the object to work on.
     * @param buff The state to write.
     *
     * @return <code>true</code> if no errors occurred, <code>false</code>
     * otherwise.
     */

    public boolean write_committed (Uid u, String tn, OutputObjectState buff) throws ObjectStoreException;

    /**
     * Some object store implementations may be running with automatic
     * sync disabled. Calling this method will ensure that any states are
     * flushed to disk.
     */

    public void sync () throws java.io.SyncFailedException, ObjectStoreException;
}

