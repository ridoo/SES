/**
 * Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.ses.common.integration.test;

import java.util.Properties;

import org.n52.oxf.ses.adapter.client.httplistener.HttpListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationReceiver implements Runnable, HttpListener {

	private static final Logger logger = LoggerFactory.getLogger(NotificationReceiver.class);
	
	private Object waitMutex = new Object();
	private boolean hasReceived = false;
	private String path;
	
	public NotificationReceiver(String path) {
		if (path != null) {
			if (path.startsWith("/")) {
				this.path = path;
			}
			else {
				this.path = "/"+path;
			}
		}
	}
	
	@Override
	public void run() {
		synchronized (waitMutex) {
			while (!hasReceived) {
				try {
					waitMutex.wait();
				} catch (InterruptedException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}
	
	@Override
	public String processRequest(String request, String uri, String method,
			Properties header) {
		synchronized (waitMutex) {
			if (!uri.equals(this.path)) return null;
			hasReceived = true;
			waitMutex.notifyAll();
		}
		return null;
	}

	public String getPath() {
		return path;
	}
	
	

}