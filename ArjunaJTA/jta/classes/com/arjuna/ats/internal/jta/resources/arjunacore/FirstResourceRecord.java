/*
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
 * $Id: XAResourceRecord.java 2342 2006-03-30 13:06:17Z  $
 */

package com.arjuna.ats.internal.jta.resources.arjunacore;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.ObjectType;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.AbstractRecord;
import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;
import com.arjuna.ats.jta.logging.jtaLogger;

public class FirstResourceRecord extends AbstractRecord {
	private static final Uid START_XARESOURCE = Uid.minUid();
	private Xid xid;
	private XAResource xaResource;

	public FirstResourceRecord(TransactionImple tx, XAResource res, Xid xid) {
		super(new Uid(), null, ObjectType.ANDPERSISTENT);

		xaResource = res;
		this.xid = xid;
	}

	public Uid order() {
		return START_XARESOURCE;
	}

	public boolean propagateOnCommit() {
		return false;
	}

	public int typeIs() {
		return Integer.MIN_VALUE;
	}

	public int topLevelPrepare() {
		try {
			xaResource.prepare(xid);
			return TwoPhaseOutcome.PREPARE_OK;
		} catch (Exception e) {
			return TwoPhaseOutcome.PREPARE_NOTOK;
		}

	}

	public int topLevelAbort() {
		try {
			xaResource.rollback(xid);
			return TwoPhaseOutcome.FINISH_OK;
		} catch (XAException e) {
			return TwoPhaseOutcome.FINISH_ERROR;
		}
	}

	public int topLevelCommit() {
		try {
			xaResource.commit(xid, false);
			return TwoPhaseOutcome.FINISH_OK;
		} catch (XAException e1) {
			return TwoPhaseOutcome.FINISH_ERROR;
		}
	}

	public int topLevelOnePhaseCommit() {
		try {
			xaResource.commit(xid, true);
			return TwoPhaseOutcome.FINISH_OK;
		} catch (XAException e1) {
			return TwoPhaseOutcome.FINISH_ERROR;
		}
	}

	public boolean forgetHeuristic() {
		if (jtaLogger.logger.isTraceEnabled()) {
			jtaLogger.logger.trace("XAResourceRecord.forget for " + this);
		}

		try {
			xaResource.forget(xid);
		} catch (Exception e) {
		}

		return true;
	}

	public String type() {
		return "/StateManager/AbstractRecord/FirstResourceRecord";
	}

	public Object value() {
		return xaResource;
	}

	public void setValue(Object o) {
	}

	public int nestedAbort() {
		return TwoPhaseOutcome.FINISH_OK;
	}

	public int nestedCommit() {
		return TwoPhaseOutcome.FINISH_OK;
	}

	public int nestedPrepare() {
		return TwoPhaseOutcome.PREPARE_OK; // do nothing
	}

	public int nestedOnePhaseCommit() {
		return TwoPhaseOutcome.FINISH_OK;
	}

	public void merge(AbstractRecord a) {
	}

	public void alter(AbstractRecord a) {
	}

	public boolean shouldAdd(AbstractRecord a) {
		return false;
	}

	public boolean shouldAlter(AbstractRecord a) {
		return false;
	}

	public boolean shouldMerge(AbstractRecord a) {
		return false;
	}

	public boolean shouldReplace(AbstractRecord a) {
		return false;
	}

}
